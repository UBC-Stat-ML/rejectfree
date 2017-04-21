package ca.ubc.bps.timers;

import java.util.Collection;
import java.util.Random;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.PegasusSolver;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.OptimizationData;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.jblas.DoubleMatrix;

import bayonet.math.NumericalUtils;
import bayonet.opt.DifferentiableFunction;
import bayonet.opt.LBFGSMinimizer;
import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.energies.Energy;
import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

/**
 * Assumes that along any ray, the potential U(x_t) can only be either
 * (1) strictly increasing
 * (2) strictly decreasing until a unique minimum is reached, after which the potential is monotone increasing.
 * 
 * This is guaranteed to hold for example if U(x) is strictly convex and the trajectories 
 * are linear. This may also apply in certain non-convex cases too.
 * 
 * @author bouchard
 *
 */
public class NumericalUnimodalInversionTimer extends ContinuousStateDependent implements Clock
{
  private final PegasusSolver solver = new PegasusSolver();
  private final Energy energy;
  private final Optimizer optimizer;
  
  public static enum Optimizer { LBFGS, BRENT }
  
  public NumericalUnimodalInversionTimer(
      Collection<? extends Coordinate> requiredVariables,
      Energy energy,
      Optimizer optimizer)
  {
    super(requiredVariables);
    this.energy = energy;
    this.optimizer = optimizer;
  }

  @Override
  public DeltaTime next(Random random)
  {
    // go to minimum energy for free
    final double minTime = lineMinimize();
    if (minTime < 0.0)
      throw new RuntimeException();
    
    // When this is used in the local version of the sampler, there can be situations
    // where one of the factor is improper, causing some trajectory to not incur collision
    // with respect to one of the factors. As long as the product of all the factors in 
    // proper, the other factors will ensure that all the variables
    // still get updated infinitely often
    if (Double.isInfinite(minTime))
      return DeltaTime.infinity();
    
    double initialEnergy = energy.valueAt(extrapolatePosition(minTime));
    final double exponential = BPSStaticUtils.sampleUnitRateExponential(random);
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
    final double upperBound = findUpperBound2(lineSolvingFunction);
    final int maxEval = 100;
    final double time2 = solver.solve(maxEval, lineSolvingFunction, 0.0, upperBound);
    return DeltaTime.isEqualTo(minTime + time2);
  }
  
  private static double findUpperBound1(LineMinimizationObjective lineSolvingFunction)
  {
    double result = 1.0;
    final int maxNIterations = Double.MAX_EXPONENT - 1;
    for (int i = 0; i < maxNIterations; i++)
    {
      if (lineSolvingFunction.derivativeAt(result) > 0.0)
        return result;
      else
        result *= 2.0;
    }
    return Double.NaN;
  }
  
  private static double findUpperBound2(UnivariateFunction lineSolvingFunction)
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
  
  private class LineMinimizationObjective implements DifferentiableFunction, UnivariateFunction
  {
    @Override
    public double valueAt(double[] _time)
    {
      double time = _time[0];
      return value(time);
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
    
    public double derivativeAt(double _time)
    {
      return derivativeAt(new double[]{_time})[0];
    }

    @Override
    public double value(double time)
    {
      double [] position = extrapolatePosition(time); 
      return energy.valueAt(position);
    }
  }

  private double lineMinimize()
  {
    LineMinimizationObjective lineRestricted = new LineMinimizationObjective();
    
    // already going up energy
    if (lineRestricted.derivativeAt(new double[]{0})[0] >= 0.0)
      return 0.0;

    if (optimizer == Optimizer.LBFGS)
    {
      double minTime = new LBFGSMinimizer().minimize(lineRestricted, new double[]{0}, 1e-10)[0];
      double minValue = lineRestricted.value(minTime);
      double valuePlusDelta = lineRestricted.value(minTime + DELTA);
      if (valuePlusDelta < minValue) // this subcase is used for improper factor in the local algorithm
        return Double.POSITIVE_INFINITY;
      else
        return minTime;
    }
    else if (optimizer == Optimizer.BRENT)
    {
      double upperBound = findUpperBound1(lineRestricted);
      if (Double.isNaN(upperBound))
        return Double.POSITIVE_INFINITY;
      BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-10);
      
      SearchInterval interval = new SearchInterval(0.0, upperBound, 0.0);
      return optimizer.optimize(
          GoalType.MINIMIZE, 
          new UnivariateObjectiveFunction(lineRestricted), 
          interval, 
          new MaxEval(10_000)).getPoint();
    }
    else 
      throw new RuntimeException();
  }
  
  private static final double DELTA = 1.0;

}
