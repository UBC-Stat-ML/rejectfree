package ca.ubc.bps.timers;

import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.energies.Energy;
import ca.ubc.bps.state.ContinuousStateDependent;

public class StandardIntensity implements Intensity
{
  private final Energy gradient;
  
  public StandardIntensity(Energy gradient)
  {
    this.gradient = gradient;
  }

  @Override
  public double evaluate(ContinuousStateDependent state, double delta)
  {
    double [] velocity = state.extrapolateVelocity(delta);
    double [] curGradient = gradient.gradient(state.extrapolatePosition(delta));
    return canonicalRate(velocity, curGradient);
  }
  
  public static double canonicalRate(double [] velocity, double [] gradient)
  {
    return Math.max(0.0, BPSStaticUtils.dot(velocity, gradient));
  }

}
