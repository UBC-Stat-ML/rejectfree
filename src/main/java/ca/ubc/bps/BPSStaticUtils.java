package ca.ubc.bps;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import com.google.common.collect.FluentIterable;

import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.bps.state.Dynamics;
import ca.ubc.bps.state.PiecewiseLinear;
import ca.ubc.pdmp.Coordinate;

public class BPSStaticUtils
{
  public static boolean isPiecewiseLinear(Dynamics dynamics)
  {
    return dynamics.getClass() == PiecewiseLinear.class;
  }
  
  public static boolean isPiecewiseLinear(PositionVelocity coordinate)
  {
    return isPiecewiseLinear(coordinate.dynamics);
  }
  
  public static void error(String message)
  {
    throw new RuntimeException(message);
  }
  
  public static void error()
  {
    throw new RuntimeException();
  }
  
  public static double dot(double [] v0, double [] v1)
  {
    if (v0.length != v1.length)
      throw new RuntimeException();
    double sum = 0.0;
    for (int i = 0; i < v0.length; i++)
      sum += v0[i] * v1[i];
    return sum;
  }
  
  public static List<PositionVelocity> continuousCoordinates(Collection<? extends Coordinate> coordinates)
  {
    if (!isSet(coordinates))
      throw new RuntimeException();
    return FluentIterable.from(coordinates).filter(PositionVelocity.class).toList();
  }
  
  public static boolean isSet(Collection<?> collection)
  {
    return collection.size() == new HashSet<>(collection).size();
  }
  
  public static double sampleUnitRateExponential(Random random)
  {
    return - Math.log(random.nextDouble());
  }
  
  public static double sampleExponential(Random random, double rate)
  {
    return sampleUnitRateExponential(random) / rate;
  }
  
  public static boolean sampleBernoulli(Random random, double probabilityToBeTrue)
  {
    return random.nextDouble() < probabilityToBeTrue;
  }
}
