package ca.ubc.bps.state;

import java.util.Collection;
import java.util.List;

import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.StateDependentBase;

public abstract class ContinuousStateDependent extends StateDependentBase
{
  // we use a list here to fix an ordering for the vector representation
  protected final List<ContinuouslyEvolving> continuousCoordinates;
  protected final boolean isPiecewiseLinear;
  
  public ContinuousStateDependent(Collection<? extends Coordinate> requiredVariables)
  {
    // maintain all the dependencies (some of which may not be continuously evolving)
    super(requiredVariables);
    // identify the subset that is continuously evolving (getting rid of potential duplicates at same time)
    continuousCoordinates = BPSStaticUtils.continuousCoordinates(requiredVariables);
    isPiecewiseLinear = _isPiecewiseLinear(continuousCoordinates);
  }

  private void extrapolate(double deltaTime)
  {
    if (deltaTime == 0.0 || deltaTime == -0.0)
      return;
    // avoid building iterator here as this will be in inner loop
    for (int i = 0; i < continuousCoordinates.size(); i++)
      continuousCoordinates.get(i).extrapolateInPlace(deltaTime);
  }
  
  private static boolean _isPiecewiseLinear(Collection<ContinuouslyEvolving> continuousCoordinates)
  {
    for (ContinuouslyEvolving coordinate : continuousCoordinates)
      if (!BPSStaticUtils.isPiecewiseLinear(coordinate))
        return false;
    return true;
  }

  private double [] extrapolateVector(double deltaTime, boolean forPosition)
  {
    extrapolate(deltaTime);
    double [] result = ContinuouslyEvolving.toArray(continuousCoordinates, forPosition);
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
  
  public void setPosition(double [] position)
  {
    ContinuouslyEvolving.set(continuousCoordinates, position, true);
  }
  
  public void setVelocity(double [] velocity)
  {
    ContinuouslyEvolving.set(continuousCoordinates, velocity, false);
  }
}