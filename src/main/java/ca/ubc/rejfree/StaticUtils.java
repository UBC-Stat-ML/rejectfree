package ca.ubc.rejfree;

import org.apache.commons.math3.analysis.differentiation.GradientFunction;

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
}
