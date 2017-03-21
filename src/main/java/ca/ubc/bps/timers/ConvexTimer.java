package ca.ubc.bps.timers;

import java.util.Collection;
import java.util.Random;
import java.util.function.Function;

import bayonet.distributions.Bernoulli;
import bayonet.distributions.Exponential;
import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class ConvexTimer extends ContinuousStateDependent implements Clock
{
  public final double stepSize;
  
  private final Function<double[], Double> potential;

  public ConvexTimer(Collection<? extends Coordinate> requiredVariables, Function<double[], Double> potential, double stepSize)
  {
    super(requiredVariables);
    this.potential = potential;
    this.stepSize = stepSize;
  }

  @Override
  public DeltaTime next(Random random)
  {
    double boundRate = Math.max(getPotential(0.0), getPotential(stepSize));
    double sample = Exponential.generate(random, boundRate);
    if (sample > stepSize)
      return DeltaTime.isGreaterThan(stepSize);
    double ratio = getPotential(sample) / boundRate;
    if (Bernoulli.generate(random, ratio))
      return DeltaTime.isEqualTo(sample);
    else
      return DeltaTime.isGreaterThan(sample);
  }
  
  public double getPotential(double delta)
  {
    double value = potential.apply(extrapolateVelocity(delta));
    if (value < 0.0)
      throw new RuntimeException();
    return value;
  }

}
