package ca.ubc.bps.timers;

import java.util.Collection;
import java.util.Random;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.PegasusSolver;
import org.jblas.DoubleMatrix;

import bayonet.math.NumericalUtils;
import bayonet.opt.DifferentiableFunction;
import bayonet.opt.LBFGSMinimizer;
import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.energies.EnergyGradient;
import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class LogConcaveDensityTimer extends ContinuousStateDependent implements Clock
{
  private final PegasusSolver solver = new PegasusSolver();
  private final EnergyGradient energy;
  private static final Random jitterRandom = new Random(1);
  
  public LogConcaveDensityTimer(Collection<? extends Coordinate> requiredVariables,
      EnergyGradient energy)
  {
    super(requiredVariables);
    this.energy = energy;
  }

  @Override
  public DeltaTime next(Random random)
  {
    // go to minimum energy for free
    final double minTime = lineMinimize();
    
    // When this is used in the local version of the sampler, there can be situations
    // where one of the factor is improper, causing some trajectory to not incur collision
    // with respect to one of the factors. As long as the product of all the factors in 
    // proper, the other factors will ensure that all the variables
    // still get updated infinitely often
    if (Double.isInfinite(minTime))
      return DeltaTime.isGreaterThan(1_000 + jitterRandom.nextDouble());
    
    double initialEnergy = energy.valueAt(extrapolatePosition(minTime));
    final double exponential = BPSStaticUtils.sampleUnitRateExponential(random);
    // TODO: Use better solver / make use of gradient here as well?
    final UnivariateFunction lineSolvingFunction = new UnivariateFunction() {
      @Override
      public double value(final double time)
      {
        final double candidateEnergy = energy.valueAt(extrapolatePosition(time + minTime));
        final double delta = candidateEnergy - initialEnergy;
        if (delta < - NumericalUtils.THRESHOLD)
          System.err.println("Did not expect negative delta for convex objective. " +
              "Delta=" + delta + ", time=" + time);
        return exponential - delta;
      }
    };
    final double upperBound = findUpperBound(lineSolvingFunction);
    final int maxEval = 100;
    final double time2 = solver.solve(maxEval, lineSolvingFunction, 0.0, upperBound);
    return DeltaTime.isEqualTo(minTime + time2);
  }
  
  private static double findUpperBound(UnivariateFunction lineSolvingFunction)
  {
    double result = 1.0;
    final int maxNIterations = Double.MAX_EXPONENT - 1;
    for (int i = 0; i < maxNIterations; i++)
    {
      if (lineSolvingFunction.value(result) < 0.0)
        return result;
      else
        result *= 2.0;
    }
    throw new RuntimeException();
  }

  private double lineMinimize()
  {
    DifferentiableFunction lineRestricted = new DifferentiableFunction() {
      
      @Override
      public double valueAt(double[] _time)
      {
        double time = _time[0];
        double [] position = extrapolatePosition(time); 
        return energy.valueAt(position);
      }
      
      @Override
      public int dimension()
      {
        return 1;
      }
      
      @Override
      public double[] derivativeAt(double[] _time)
      {
        double time = _time[0];
        double [] position = extrapolatePosition(time); 
        DoubleMatrix fullDerivative = new DoubleMatrix(energy.gradient(position));
        double directionalDeriv = fullDerivative.dot(new DoubleMatrix(extrapolateVelocity(time)));
        return new double[]{directionalDeriv};
      }
    };
    
//    // heuristic: see below
//    if (lineRestricted.valueAt(extrapolatePosition(-0.01)) < lineRestricted.valueAt(extrapolatePosition(0.0)))
//      return 0.0;  Broken for some reason..?
    
    // TODO: don't lose time searching in negative (but above heuristic should mostly address this)
    double minTime = new LBFGSMinimizer().minimize(lineRestricted, new double[]{0}, 1e-10)[0];
    
    if (minTime < 0.0)
      return 0.0;
    
    double minValue = lineRestricted.valueAt(new double[]{minTime});
    double valuePlusDelta = lineRestricted.valueAt(new double[]{minTime + DELTA});
    if (valuePlusDelta < minValue)
      return Double.POSITIVE_INFINITY;
    
    return minTime;
  }
  
  private static final double DELTA = 1.0;

}
