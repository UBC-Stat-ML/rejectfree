package ca.ubc.bps.state;

public class Hyperbolic implements Dynamics
{
  @Override
  public void extrapolateInPlace(double deltaTime, MutableDouble _position, MutableDouble velocity)
  {
    TransformedMutableDouble position = (TransformedMutableDouble) _position;
    double updatedBd = position.getBounded() + deltaTime * velocity.get();
    position.setBoundedCoordinate(updatedBd);
  } 
    
  public static double toBdCoord(double unb)
  {
    final double abs = Math.abs(unb);
    return Math.signum(unb) * abs / (1.0 + abs);
  }
  
  public static double toUnbCoord(double bd)
  {
    if (bd <= -1.0 || bd >= 1.0)
      return Double.NaN;
    final double abs = Math.abs(bd);
    return - Math.signum(bd) * abs / (abs - 1.0); 
  }
}