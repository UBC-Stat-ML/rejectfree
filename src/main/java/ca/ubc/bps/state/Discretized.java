package ca.ubc.bps.state;

public class Discretized implements Dynamics
{
  @Override
  public void extrapolateInPlace(double deltaTime, MutableDouble position, MutableDouble velocity)
  {
    // nothing to do; dynamics acts on jumps
  }
}
