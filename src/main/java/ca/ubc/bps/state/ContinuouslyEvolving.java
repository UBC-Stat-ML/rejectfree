package ca.ubc.bps.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ca.ubc.pdmp.Coordinate;

public class ContinuouslyEvolving implements Coordinate
{
  public final MutableDouble position;
  
  // since it is continuously evolving, it necessarily has a velocity
  public final MutableDouble velocity;
  
  public final Dynamics dynamics;
  
  public ContinuouslyEvolving(MutableDouble position, MutableDouble velocity, Dynamics dynamics)
  {
    this.position = position;
    this.velocity = velocity;
    this.dynamics = dynamics;
  }

  @Override
  public void extrapolateInPlace(double deltaTime)
  {
    dynamics.extrapolateInPlace(deltaTime, position, velocity);
  }
  
  public static List<ContinuouslyEvolving> buildArray(int size, Dynamics dynamics)
  {
    List<ContinuouslyEvolving> result = new ArrayList<>(size);
    for (int i = 0; i < size; i++)
      result.add(
          new ContinuouslyEvolving(
              new MutableDoubleImplementation(), 
              new MutableDoubleImplementation(), 
              dynamics
              )
          );
    return result;
  }
  
  public static class MutableDoubleImplementation implements MutableDouble
  {
    double value = 0.0;

    @Override
    public void set(double value)
    {
      this.value = value;
    }

    @Override
    public double get()
    {
      return value;
    }
    
  }
}