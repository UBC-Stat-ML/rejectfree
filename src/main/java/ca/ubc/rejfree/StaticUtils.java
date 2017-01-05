package ca.ubc.rejfree;

import ca.ubc.rejfree.state.ContinuouslyEvolving;
import ca.ubc.rejfree.state.Dynamics;
import ca.ubc.rejfree.state.PiecewiseLinear;

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
  
  public static double dot(double [] v0, double [] v1)
  {
    if (v0.length != v1.length)
      throw new RuntimeException();
    double sum = 0.0;
    for (int i = 0; i < v0.length; i++)
      sum += v0[i] * v1[i];
    return sum;
  }
}
