package ca.ubc.bps.timers;


@FunctionalInterface
public interface Intensity
{
  public double evaluate(double delta);
}