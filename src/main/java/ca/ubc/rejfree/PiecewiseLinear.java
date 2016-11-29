package ca.ubc.rejfree;

public class PiecewiseLinear extends ContinuouslyEvolving
{
  public PiecewiseLinear(MutableDouble position, MutableDouble velocity)
  {
    super(position, velocity);
  }
  @Override
  public void extrapolate(double deltaTime)
  {
    position.set(position.get() + deltaTime * velocity.get());
  } 
}