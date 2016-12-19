package ca.ubc.rejfree;

public interface Dynamics
{
  void extrapolateInPlace(double deltaTime, MutableDouble position, MutableDouble velocity);
}
