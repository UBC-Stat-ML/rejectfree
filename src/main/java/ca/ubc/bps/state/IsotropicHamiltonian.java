package ca.ubc.bps.state;

public class IsotropicHamiltonian implements Dynamics
{
  private double precision = Double.NaN;

  public double getPrecision()
  {
    return precision;
  }

  public void setPrecision(double precision)
  {
    this.precision = precision;
  }

  @Override
  public void extrapolateInPlace(double deltaTime, MutableDouble position, MutableDouble velocity)
  {
    if (Double.isNaN(precision))
      throw new RuntimeException();
    double oldX = position.get();
    double oldV = velocity.get();
    position.set(+ oldV * Math.sin(deltaTime) / precision + oldX * Math.cos(deltaTime));
    velocity.set(- oldX * Math.sin(deltaTime) * precision + oldV * Math.cos(deltaTime));
  }
}
