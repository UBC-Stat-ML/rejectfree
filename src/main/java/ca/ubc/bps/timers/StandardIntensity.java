package ca.ubc.bps.timers;

import ca.ubc.bps.energies.EnergyGradient;
import ca.ubc.bps.state.ContinuousStateDependent;

public class StandardIntensity implements Intensity
{
  private final EnergyGradient gradient;
  
  public StandardIntensity(EnergyGradient gradient)
  {
    this.gradient = gradient;
  }

  @Override
  public double evaluate(ContinuousStateDependent state, double delta)
  {
    double [] velocity = state.extrapolateVelocity(delta);
    double [] curGradient = gradient.gradient(state.extrapolatePosition(delta));
    double sum = 0.0;
    for (int i = 0; i < velocity.length; i++)
      sum += velocity[i] * curGradient[i];
    return Math.max(0.0, sum);
  }

}
