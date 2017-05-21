package ca.ubc.sparsenets;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

/**
 * TODO: move all ca.ubc.sparsenets to some separate repo
 */
public class LogTimer implements Clock
{
  private final PositionVelocity w;
  private final int m; // TODO: these will be random too
  private final double sigma;
  
  public LogTimer(PositionVelocity w, int m, double sigma)
  {
    this.w = w;
    this.m = m;
    this.sigma = sigma;
  }

  @Override
  public Collection<? extends Coordinate> requiredVariables()
  {
    // TODO: add sigma once resampled
    // TODO: - AND - add m as well
    return Collections.singleton(w);
  }
  
  private double coeff()
  {
    // TODO: change that once m is random too
    return 1 + sigma() - m;
  }

  private double sigma()
  {
    // TODO: change once sigma resampled
    return sigma; 
  }

  @Override
  public DeltaTime next(Random random)
  {
    double c = coeff();
    double x = w.position.get();
    double v = w.velocity.get();
    
    if (c == 0 || v == 0)
      return DeltaTime.infinity();
    
    if (c > 0 && v < 0)
      return DeltaTime.isEqualTo(-x / v);
    
    if (c < 0 && v > 0)
      return DeltaTime.infinity();
    
    double e = BPSStaticUtils.sampleUnitRateExponential(random);
    double time = (Math.exp(e/c + Math.log(x)) - x) / v; 
    return DeltaTime.isEqualTo(time);
  }
}
