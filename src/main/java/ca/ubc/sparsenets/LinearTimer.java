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
  private final double tau;
  
  public LinearTimer(PositionVelocity w, double tau)
  {
    this.w = w;
    this.tau = tau;
  }

  @Override
  public Collection<? extends Coordinate> requiredVariables()
  {
   // TODO: add tau once resampled
    return Collections.singleton(w); 
  }
  
  private double tau()
  {
    // TODO: change once sigma resampled
    return tau;  
  }

  @Override
  public DeltaTime next(Random random)
  {
    double tau = tau();
    
    if (tau == 0.0)
      return DeltaTime.infinity();
    
    double v = w.velocity.get();
    
    if (v < 0.0)
      return DeltaTime.infinity();
    
    double e = BPSStaticUtils.sampleUnitRateExponential(random);
    double time = e / v / tau;
    return DeltaTime.isEqualTo(time);
  }
}
