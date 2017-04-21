package ca.ubc.bps.state;

public class Hyperbolic implements Dynamics
{
  @Override
  public void extrapolateInPlace(double deltaTime, MutableDouble position, MutableDouble velocity)
  {
    double updatedBd = toBdCoord(position.get()) + deltaTime * velocity.get();
    if (updatedBd <= -1 || updatedBd >= 1)
      throw new InvalidExtrapolationException();
    position.set(toUnbCoord(updatedBd));
  } 
  
  public static class InvalidExtrapolationException extends RuntimeException
  {
    private static final long serialVersionUID = 1L;
  }
    
  public static double toBdCoord(double unb)
  {
    final double abs = Math.abs(unb);
    return Math.signum(unb) * abs / (1.0 + abs);
  }
  
  public static double toUnbCoord(double bd)
  {
    final double abs = Math.abs(bd);
    return - Math.signum(bd) * abs / (abs - 1.0); 
  }
}