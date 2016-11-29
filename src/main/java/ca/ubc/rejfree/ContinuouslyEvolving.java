package ca.ubc.rejfree;

import ca.ubc.pdmp.Coordinate;

// not needed by PDMPSimulator
public abstract class ContinuouslyEvolving implements Coordinate
{
  public final MutableDouble position;
  
  // since it is continuously evolving, it necessarily has a velocity
  public final MutableDouble velocity;
  
  public ContinuouslyEvolving(MutableDouble position, MutableDouble velocity)
  {
    super();
    this.position = position;
    this.velocity = velocity;
  }
}