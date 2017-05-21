package ca.ubc.sparsenets;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class QuadraticTimer implements Clock
{
  private final PositionVelocity sum;
  private final double wStar;
  
  public QuadraticTimer(PositionVelocity sum, double wStar)
  {
    this.sum = sum;
    this.wStar = wStar;
  }

  @Override
  public Collection<? extends Coordinate> requiredVariables()
  {
    // TODO: add w*
    return Collections.singleton(sum);
  }
  
  private double wStar()
  {
    // TODO make depend on w*
    return wStar;
  }

  @Override
  public DeltaTime next(Random random)
  {
    double wStar = wStar();
    if (wStar < 0)
      throw new RuntimeException();
    
    double x = sum.position.get();
    double v = sum.velocity.get();
    
    if (v < 0)
      return DeltaTime.isEqualTo(- (wStar + x) / v);
    
    double xPlusW = x + wStar;
    double e = BPSStaticUtils.sampleUnitRateExponential(random);
    double time = (-xPlusW + Math.sqrt(xPlusW * xPlusW + e)) / v;
    return DeltaTime.isEqualTo(time);
  }

}
