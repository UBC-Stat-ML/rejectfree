package ca.ubc.rejfree;

import java.util.List;

import com.google.common.collect.FluentIterable;

import ca.ubc.pdmp.Coordinate;

public abstract class ContinuousStateDependent extends StateDependentBase
{
  private final List<ContinuouslyEvolving> continuousCoordinates;
  private final boolean isPiecewiseLinear;
  
  public ContinuousStateDependent(List<Coordinate> requiredVariables)
  {
    super(requiredVariables);
    this.continuousCoordinates = FluentIterable.from(requiredVariables).filter(ContinuouslyEvolving.class).toList();
    isPiecewiseLinear = isPiecewiseLinear();
  }

  private void extrapolate(double deltaTime)
  {
    for (ContinuouslyEvolving coordinate : continuousCoordinates)
      coordinate.extrapolate(deltaTime);
  }
  
  private boolean isPiecewiseLinear()
  {
    for (ContinuouslyEvolving coordinate : continuousCoordinates)
      if (!(coordinate instanceof PiecewiseLinear))
        return false;
    return true;
  }

  private double [] extrapolateVector(double deltaTime, boolean forPosition)
  {
    double [] result = new double[continuousCoordinates.size()];
    extrapolate(deltaTime);
    for (int i = 0; i < continuousCoordinates.size(); i++)
    {
      final ContinuouslyEvolving z = continuousCoordinates.get(i);
      result[i] = forPosition ? z.position.get() : z.velocity.get();
    }
    extrapolate( - deltaTime);
    return result;
  }
  
  public double [] currentVelocity()
  {
    return extrapolateVector(0.0, false);
  }
  
  public double [] extrapolateVelocity(double deltaTime)
  {
    return isPiecewiseLinear ? currentVelocity() : extrapolateVector(deltaTime, false);
  }
  
  public double [] currentPosition()
  {
    return extrapolateVector(0.0, true);
  }
  
  public double [] extrapolatePosition(double deltaTime)
  {
    return extrapolateVector(deltaTime, true);
  }
  
  private void set(double [] vector, boolean forPosition)
  {
    if (vector.length != continuousCoordinates.size())
      throw new RuntimeException();
    for (int i = 0; i < vector.length; i++)
    {
      ContinuouslyEvolving coordinate = continuousCoordinates.get(i);
      (forPosition ? coordinate.position : coordinate.velocity).set(vector[i]);
    }
  }
  
  public void setPosition(double [] position)
  {
    set(position, true);
  }
  
  public void setVelocity(double [] velocity)
  {
    set(velocity, false);
  }
}