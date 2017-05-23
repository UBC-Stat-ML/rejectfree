package ca.ubc.sparsenets;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class LinearTimer implements Clock
{
  private final PositionVelocity w; 
  private final double constant;
  
  public LinearTimer(PositionVelocity w, double constant)
  {
    this.w = w;
    this.constant = constant;
  }

  @Override
  public Collection<? extends Coordinate> requiredVariables()
  {
    return Collections.singleton(w); 
  }

  @Override
  public DeltaTime next(Random random)
  {
    if (constant == 0.0)
      return DeltaTime.infinity();
    
    double v = w.velocity.get();
    
    if (v < 0.0)
      return DeltaTime.infinity();
    
    double e = BPSStaticUtils.sampleUnitRateExponential(random);
    double time = e / v / constant;
    return DeltaTime.isEqualTo(time);
  }
}
