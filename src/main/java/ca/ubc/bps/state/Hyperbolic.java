package ca.ubc.bps.state;

public class Hyperbolic implements Dynamics
{
  @Override
  public void extrapolateInPlace(double deltaTime, MutableDouble _position, MutableDouble velocity)
  {
    if (_position instanceof TransformedMutableDouble)
    {
      TransformedMutableDouble position = (TransformedMutableDouble) _position;
      double updatedBd = position.getBounded() + deltaTime * velocity.get();
      position.setBoundedCoordinate(updatedBd);
    }
    else
    {
      // this will still be OK for interpolation (e.g. in processors) but not extrapolation, because of 
      // cases where going out of bound, in which case we need TransformedMutableDouble implementation and 
      // the above code
      _position.set(toUnbCoord(toBdCoord(_position.get()) + deltaTime * velocity.get())); 
    }
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