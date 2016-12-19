package ca.ubc.rejfree;

public class PiecewiseLinear implements Dynamics
{
  @Override
  public void extrapolateInPlace(double deltaTime, MutableDouble position, MutableDouble velocity)
  {
    position.set(position.get() + deltaTime * velocity.get());
  } 
}