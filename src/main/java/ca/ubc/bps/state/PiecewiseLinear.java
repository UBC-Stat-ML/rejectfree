package ca.ubc.bps.state;

public class PiecewiseLinear implements Dynamics
{
  // Avoid singleton as it would create additional complexity with the inits infrastructure and no clear benefits
  // private PiecewiseLinear() {}
  // public static final PiecewiseLinear instance = new PiecewiseLinear();
  
  @Override
  public void extrapolateInPlace(double deltaTime, MutableDouble position, MutableDouble velocity)
  {
    position.set(position.get() + deltaTime * velocity.get());
  } 
}