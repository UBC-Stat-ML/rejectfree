package ca.ubc.bps.timers;

import ca.ubc.bps.state.ContinuousStateDependent;

@FunctionalInterface
public interface Intensity
{
  public double evaluate(ContinuousStateDependent state, double delta);
}