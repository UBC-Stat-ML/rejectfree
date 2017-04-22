package ca.ubc.bps.timers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.PegasusSolver;
import org.apache.commons.math3.optim.MaxEval;
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
import ca.ubc.bps.state.TransformedMutableDouble;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

/**
 * Assumes that along any ray, the potential U(x_t) can only be either
 * (1) strictly increasing
 * (2) strictly decreasing until a unique minimum is reached, after which the potential is monotone increasing.
 * 
 * This is guaranteed to hold for example if U(x) is strictly convex and the trajectories 
 * are linear. This may also apply in certain non-convex cases too, e.g. quasi convex functions.
 * 
 * @author bouchard
 *
 */
public class NumericalUnimodalInversionTimer extends ContinuousStateDependent implements Clock
{
  private final PegasusSolver solver = new PegasusSolver();
  private final Energy energy;
  private final Optimizer optimizer;
  
  public static enum Optimizer 
  { 
    LBFGS,  // Only if convex; requires slightly less variable updates, but overall a bit slower because of overhead
    BRENT,  
    ROBUST_LINE_SEARCH
  }
  
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
    if (minTime == Double.POSITIVE_INFINITY)
      return DeltaTime.infinity();
    
//    {
//      System.err.println("--");
//      LineMinimizationObjective lineRestricted = new LineMinimizationObjective();
//      System.err.println("at " + minTime + " " + lineRestricted.value(minTime));
//      System.err.println("at 0.23 " + lineRestricted.value(0.23));
//    }
//    {
//      //
//      double [] position = extrapolatePosition(0);
//      System.out.println(Arrays.toString(position));
//    }
    
    final double initialEnergy = energy.valueAt(extrapolatePosition(minTime));
    
    if (initialEnergy == Double.POSITIVE_INFINITY)
    {
      lineMinimize();
      throw new RuntimeException();
    }
    
    final double exponential = BPSStaticUtils.sampleUnitRateExponential(random);
    final UnivariateFunction lineSolvingFunction = new UnivariateFunction() {
      @Override
      public double value(final double time)
      {
        final double candidateEnergy = energy.valueAt(extrapolatePosition(time + minTime));
        final double delta = candidateEnergy - initialEnergy; 
        if (delta < - NumericalUtils.THRESHOLD)
        {
//          System.err.println("minTime:" + minTime);
//          for (int i = 0; i < continuousCoordinates.size(); i++)
//          {
//            System.err.println("dim " + i);
//            System.err.println("pos " + ((TransformedMutableDouble) (continuousCoordinates.get(i).position)).getBounded());
//            System.err.println("unb " + continuousCoordinates.get(i).position.get());
//            System.err.println("vel " + continuousCoordinates.get(i).velocity.get());
//          }
          throw new RuntimeException(
            "Did not expect negative delta. " +
              "Delta=" + delta + ", " + 
              "time=" + time);
        }
        final double result =  exponential - delta;
        return result;
      }
    };
    double upperBound = findUpperBound2(lineSolvingFunction);
    if (Double.isInfinite(lineSolvingFunction.value(upperBound)))
      upperBound = shrinkUpperBound2(upperBound, lineSolvingFunction);
    
    double time2 = solver.solve(10_000, lineSolvingFunction, 0.0, upperBound);
//    System.out.println("-=-=->" + Arrays.toString(extrapolatePosition(minTime + time2)));
    return DeltaTime.isEqualTo(minTime + time2);
  }
  
  private static final int maxNIterations = Double.MAX_EXPONENT - 1;

  private static double findUpperBound1(LineMinimizationObjective lineSolvingFunction)
  {
    double result = 2.0e-4;
    for (int i = 0; i < maxNIterations; i++)
    {
      double value = lineSolvingFunction.value(result);
      if (value == Double.POSITIVE_INFINITY || lineSolvingFunction.value(result + 1e-5) > value)
        return result;
      else
        result *= 2.0;
    }
    return Double.NaN;
  }
  
  private static double findUpperBound2(UnivariateFunction lineSolvingFunction)
  {
    double result = 2.0e-4;
    for (int i = 0; i < maxNIterations; i++)
    {
      double value = lineSolvingFunction.value(result) ;
      if (value < 0.0)
        return result;
      else
        result *= 2.0;
    }
//    result = 2.0e-4;
//    for (int i = 0; i < maxNIterations; i++)
//    {
//      double value = lineSolvingFunction.value(result) ;
//      if (value < 0.0)
//        return result;
//      else
//        result *= 2.0;
//    }
    throw new RuntimeException();
  }
  
  private double shrinkUpperBound2(double upperBound, UnivariateFunction lineSolvingFunction)
  {
    double lowerBound = upperBound/2.0;
    if (!Double.isFinite(lineSolvingFunction.value(lowerBound)) || lineSolvingFunction.value(lowerBound) < 0.0)
      throw new RuntimeException();
    // bisect b/w lowerBound and upperBound
    for (int i = 0; i < maxNIterations; i++)
    {
      double middle = lowerBound + (upperBound - lowerBound) / 2.0;
      boolean isFinite = Double.isFinite(lineSolvingFunction.value(middle));
      boolean isPos    = lineSolvingFunction.value(middle) >= 0.0;
      if (isFinite && !isPos)
        return middle;
      if (isFinite && isPos)
        lowerBound = middle;
      else if (!isFinite)
        upperBound = middle;
      else
        throw new RuntimeException();
    }
    throw new RuntimeException();
  }
  
  private double robustLineSearchForMin(double lowerBound, double upperBound, LineMinimizationObjective lineSolvingFunction)
  {
    for (int i = 0; i < maxNIterations; i++)
    {
      double middle = lowerBound + (upperBound - lowerBound) / 2.0;
      double left  = lineSolvingFunction.value(middle - 1e-5);
      double value = lineSolvingFunction.value(middle);
      double right = lineSolvingFunction.value(middle + 1e-5);
//      double derivative = lineSolvingFunction.derivativeAt(middle);
      if (value == Double.POSITIVE_INFINITY)
        upperBound = middle;
      else if (value <= left && value <= right)
        return middle;
      else if (left >= value && value >= right)
        lowerBound = middle;
      else if (left <= value && value <= right)
        upperBound = middle;
      else
        throw new RuntimeException("" + left + ' ' + value + ' ' + right);
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
    
//    private final double FINITE_DIFF_DELTA = 1e-10;
//    public double finDiffFwd(double time)
//    {
//      return (value(time + FINITE_DIFF_DELTA) - value(time)) / FINITE_DIFF_DELTA;
//    }
//
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

    if (optimizer == Optimizer.LBFGS)
    {
      // already going up energy
      if (lineRestricted.derivativeAt(new double[]{0})[0] >= 0.0)
        return 0.0;
      
      double minTime = new LBFGSMinimizer().minimize(lineRestricted, new double[]{0}, 1e-10)[0];
//      System.out.println("--> " + lineRestricted.derivativeAt(minTime));
      double minValue = lineRestricted.value(minTime);
      double valuePlusDelta = lineRestricted.value(minTime + DELTA);
      if (valuePlusDelta < minValue) // this subcase is used for improper factor in the local algorithm
        return Double.POSITIVE_INFINITY;
      else
        return minTime;
    }
    else if (optimizer == Optimizer.BRENT)
    {
//      if (true)
//        throw new RuntimeException();
      
      double upperBound = findUpperBound1(lineRestricted);
      
      if (upperBound == Double.POSITIVE_INFINITY)
        return Double.POSITIVE_INFINITY;
      BrentOptimizer optimizer = new BrentOptimizer(1e-15, 1e-15);
      
      SearchInterval interval = new SearchInterval(0.0, upperBound, 0.0);
      final double result = optimizer.optimize(
          GoalType.MINIMIZE, 
          new UnivariateObjectiveFunction(lineRestricted), 
          interval, 
          new MaxEval(10_000)).getPoint();
//      System.out.println("--> " + lineRestricted.derivativeAt(result));
      return result;
    }
    else if (optimizer == Optimizer.ROBUST_LINE_SEARCH)
    {
      if (lineRestricted.value(1e-5) > lineRestricted.value(0.0))
        return 0.0;
      
      double upperBound = findUpperBound1(lineRestricted);
      if (Double.isNaN(upperBound))
        return Double.POSITIVE_INFINITY;
      
      double result = robustLineSearchForMin(0.0, upperBound, lineRestricted);
//      System.out.println("--> " + lineRestricted.derivativeAt(result));
      return result;
    }
    else 
      throw new RuntimeException();
  }
  
  private static final double DELTA = 1.0;

}
