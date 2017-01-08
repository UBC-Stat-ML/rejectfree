package ca.ubc.bps;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.FluentIterable;

import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.state.Dynamics;
import ca.ubc.bps.state.PiecewiseLinear;
import ca.ubc.pdmp.Coordinate;

public class StaticUtils
{
  public static boolean isPiecewiseLinear(Dynamics dynamics)
  {
    return dynamics.getClass() == PiecewiseLinear.class;
  }
  
  public static boolean isPiecewiseLinear(ContinuouslyEvolving coordinate)
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
  
  public static List<ContinuouslyEvolving> continuousCoordinates(Collection<? extends Coordinate> coordinates)
  {
    if (!isSet(coordinates))
      throw new RuntimeException();
    return FluentIterable.from(coordinates).filter(ContinuouslyEvolving.class).toList();
  }
  
  public static boolean isSet(Collection<?> collection)
  {
    return collection.size() == new HashSet<>(collection).size();
  }
}
