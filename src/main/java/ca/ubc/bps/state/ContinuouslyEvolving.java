package ca.ubc.bps.state;

import java.util.ArrayList;
import java.util.List;

import ca.ubc.bps.state.MonitoredMutableDouble.ModCount;
import ca.ubc.pdmp.Coordinate;

public class ContinuouslyEvolving implements Coordinate
{
  public final MutableDouble position;
  
  // since it is continuously evolving, it necessarily has a velocity
  public final MutableDouble velocity;
  
  public final Dynamics dynamics;
  
  public final Object key;
  
  private ContinuouslyEvolving(MutableDouble position, MutableDouble velocity, Dynamics dynamics, Object key)
  {
    this.position = position;
    this.velocity = velocity;
    this.dynamics = dynamics;
    this.key = key;
  }
  
  public ContinuouslyEvolving(Dynamics dynamics, Object key)
  {
    this(dynamics, key, null);
  }
  
  public ContinuouslyEvolving(Dynamics dynamics, Object key, ModCount modCount)
  {
    this(
        position(modCount, dynamics), 
        new SimpleMutableDouble(), 
        dynamics, 
        key);
  }
  
  private static MutableDouble position(ModCount modCount, Dynamics dynamics)
  {
    MutableDouble core = modCount == null ? 
        new SimpleMutableDouble() : 
        new MonitoredMutableDouble(modCount);
    if (dynamics == null || 
        dynamics instanceof PiecewiseLinear ||
        dynamics instanceof IsotropicHamiltonian)
      return core;
    else if (dynamics instanceof Hyperbolic)
      return TransformedMutableDouble.hyperbolicPosition(core);
    else
      throw new RuntimeException();
  }

  @Override
  public void extrapolateInPlace(double deltaTime)
  {
    dynamics.extrapolateInPlace(deltaTime, position, velocity);
  }
  
  public String toString() 
  {
    return key.toString();
  }
  
  public static List<ContinuouslyEvolving> buildArray(int size, Dynamics dynamics)
  {
    return buildArray(size, dynamics, null);
  }
  
  public static List<ContinuouslyEvolving> buildArray(int size, Dynamics dynamics, ModCount modCount)
  {
    List<ContinuouslyEvolving> result = new ArrayList<>(size);
    for (int i = 0; i < size; i++)
      result.add(new ContinuouslyEvolving(dynamics,i, modCount));
    return result;
  }
  
  public static double [] toArray(List<ContinuouslyEvolving> continuousCoordinates, boolean forPosition)
  {
    double [] result = new double[continuousCoordinates.size()];
    for (int i = 0; i < continuousCoordinates.size(); i++)
    {
      final ContinuouslyEvolving z = continuousCoordinates.get(i);
      result[i] = forPosition ? z.position.get() : z.velocity.get();
    }
    return result;
  }
  
  public static double [] positionsToArray(List<ContinuouslyEvolving> continuousCoordinates)
  {
    return toArray(continuousCoordinates, true);
  }
  
  public static double [] velocitiesToArray(List<ContinuouslyEvolving> continuousCoordinates)
  {
    return toArray(continuousCoordinates, false);
  }
  
  public static void set(List<ContinuouslyEvolving> continuousCoordinates, double [] vector, boolean forPosition)
  {
    if (vector.length != continuousCoordinates.size())
      throw new RuntimeException();
    for (int i = 0; i < vector.length; i++)
    {
      ContinuouslyEvolving coordinate = continuousCoordinates.get(i);
      (forPosition ? coordinate.position : coordinate.velocity).set(vector[i]);
    }
  }
  
  public static void setPosition(List<ContinuouslyEvolving> continuousCoordinates, double [] position)
  {
    set(continuousCoordinates, position, true);
  }
  
  public static void setVelocity(List<ContinuouslyEvolving> continuousCoordinates, double [] velocity)
  {
    set(continuousCoordinates, velocity, false);
  }
}