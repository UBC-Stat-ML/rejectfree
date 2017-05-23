package ca.ubc.sparsenets;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class LinearTimer implements Clock
{
  private final List<PositionVelocity> ws; 
  private final List<Integer> ms;
  private final double sigma;
  
  public LinearTimer(List<PositionVelocity> ws, List<Integer> ms, double sigma)
  {
    this.ws = ws;
    this.ms = ms;
    this.sigma = sigma;
  }

  @Override
  public Collection<? extends Coordinate> requiredVariables()
  {
    return ws;
  }

  @Override
  public DeltaTime next(Random random)
  {
    double v = 0.0;
    for (int i = 0; i < ws.size(); i++)
      v += (sigma - ms.get(i)) * ws.get(i).velocity.get();
    if (v <= 0.0)
      return DeltaTime.infinity();
    else
      return DeltaTime.isEqualTo(BPSStaticUtils.sampleUnitRateExponential(random) / v);
  }
}
