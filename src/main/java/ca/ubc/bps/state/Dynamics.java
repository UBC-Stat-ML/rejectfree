package ca.ubc.bps.state;

import blang.inits.Implementations;

@Implementations({PiecewiseLinear.class})
public interface Dynamics
{
  void extrapolateInPlace(double deltaTime, MutableDouble position, MutableDouble velocity);
}
