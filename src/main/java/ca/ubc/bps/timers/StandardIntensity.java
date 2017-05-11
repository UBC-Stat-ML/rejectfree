package ca.ubc.bps.timers;

import java.util.Collection;

import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.energies.Energy;
import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.pdmp.Coordinate;

public class StandardIntensity extends ContinuousStateDependent implements Intensity
{
  private final Energy gradient;
  
  public StandardIntensity(Collection<? extends Coordinate> requiredVariables, Energy gradient)
  {
    super(requiredVariables);
    this.gradient = gradient;
  }

  @Override
  public double evaluate(double delta)
  {
    double [] velocity = extrapolateVelocity(delta);
    double [] curGradient = gradient.gradient(extrapolatePosition(delta));
    return canonicalRate(velocity, curGradient);
  }
  
  public static double canonicalRate(double [] velocity, double [] gradient)
  {
    return Math.max(0.0, BPSStaticUtils.dot(velocity, gradient));
  }

}
