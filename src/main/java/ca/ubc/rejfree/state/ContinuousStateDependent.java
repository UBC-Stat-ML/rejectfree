package ca.ubc.rejfree.state;

import java.util.List;

import com.google.common.collect.FluentIterable;

import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.StateDependentBase;
import ca.ubc.rejfree.StaticUtils;

public abstract class ContinuousStateDependent extends StateDependentBase
{
  protected final List<ContinuouslyEvolving> continuousCoordinates;
  private final boolean isPiecewiseLinear;
  
  public ContinuousStateDependent(List<? extends Coordinate> requiredVariables)
  {
    // maintain all the dependencies (some of which may not be continuously evolving)
    super(requiredVariables);
    // identify the subset that is continuously evolving
    this.continuousCoordinates = FluentIterable.from(requiredVariables).filter(ContinuouslyEvolving.class).toList();
    isPiecewiseLinear = isPiecewiseLinear();
  }

  private void extrapolate(double deltaTime)
  {
    for (ContinuouslyEvolving coordinate : continuousCoordinates)
      coordinate.extrapolateInPlace(deltaTime);
  }
  
  private boolean isPiecewiseLinear()
  {
    for (ContinuouslyEvolving coordinate : continuousCoordinates)
      if (!StaticUtils.isPiecewiseLinear(coordinate))
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