package ca.ubc.rejfree.state;

public class PiecewiseLinear implements Dynamics
{
  public static final PiecewiseLinear instance = new PiecewiseLinear();
  
  @Override
  public void extrapolateInPlace(double deltaTime, MutableDouble position, MutableDouble velocity)
  {
    position.set(position.get() + deltaTime * velocity.get());
  } 
  
  private PiecewiseLinear() {}
}