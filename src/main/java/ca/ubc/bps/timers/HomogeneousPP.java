package ca.ubc.bps.timers;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import ca.ubc.bps.BPSStaticUtils;
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
    return DeltaTime.isEqualTo(BPSStaticUtils.sampleExponential(random, rate));
  }

  @Override
  public List<Coordinate> requiredVariables()
  {
    return Collections.emptyList();
  }

}
