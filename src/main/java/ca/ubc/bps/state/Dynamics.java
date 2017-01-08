package ca.ubc.bps.state;

public interface Dynamics
{
  void extrapolateInPlace(double deltaTime, MutableDouble position, MutableDouble velocity);
}
