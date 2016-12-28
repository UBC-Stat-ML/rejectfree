package ca.ubc.rejfree.state;

public interface Dynamics
{
  void extrapolateInPlace(double deltaTime, MutableDouble position, MutableDouble velocity);
}
