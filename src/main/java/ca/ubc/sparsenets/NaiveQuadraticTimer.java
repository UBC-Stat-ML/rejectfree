package ca.ubc.sparsenets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import bayonet.math.NumericalUtils;
import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.state.MutableDouble;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class NaiveQuadraticTimer implements Clock
{
  private final PositionVelocity sum;
  List<PositionVelocity> variables;
  private final double wStar;
  
  public NaiveQuadraticTimer(List<PositionVelocity> variables, PositionVelocity sum, double wStar)
  {
    this.sum = sum;
    if (wStar < 0)
      throw new RuntimeException();
    this.wStar = wStar;
    this.variables = variables;
  }

  @Override
  public Collection<? extends Coordinate> requiredVariables()
  {
    // TODO: add w*
    List<PositionVelocity> result = new ArrayList<PositionVelocity>();
    result.add(sum);
    result.addAll(variables);
    return result;
  }
  
  private double wStar()
  {
    // TODO make depend on w*
    return wStar;
  }

  @Override
  public DeltaTime next(Random random)
  {
    check(true, sum.position);
    check(false, sum.velocity);
    
    double wStar = wStar();
    if (wStar < 0)
      throw new RuntimeException();
    
    double x = sum.position.get();
    double v = sum.velocity.get();
    
    if (v < 0)
      return DeltaTime.infinity(); // positivity constraints will hit first
    
    double xPlusW = x + wStar;
    double e = BPSStaticUtils.sampleUnitRateExponential(random);
    double time = (-xPlusW + Math.sqrt(xPlusW * xPlusW + e)) / v;
    return DeltaTime.isEqualTo(time);
  }

  private void check(boolean isPos, MutableDouble sum)
  {
    double check = 0.0;
    for (PositionVelocity item : variables)
      check += (isPos ? item.position : item.velocity).get();
    NumericalUtils.checkIsClose(check, sum.get());
  }

}
