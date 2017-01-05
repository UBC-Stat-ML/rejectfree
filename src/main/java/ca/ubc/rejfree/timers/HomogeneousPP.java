package ca.ubc.rejfree.timers;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import bayonet.distributions.Exponential;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class HomogeneousPP implements Clock
{
  final double rate;

  public HomogeneousPP(double rate)
  {
    this.rate = rate;
  }

  @Override
  public DeltaTime next(Random random)
  {
    double time = Exponential.generate(random, rate);
    return DeltaTime.isEqualTo(time);
  }

  @Override
  public List<Coordinate> requiredVariables()
  {
    return Collections.emptyList();
  }

}
