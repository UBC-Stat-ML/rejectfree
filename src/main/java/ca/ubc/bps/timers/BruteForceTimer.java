package ca.ubc.bps.timers;

import java.util.Collection;
import java.util.Random;

import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.energies.Energy;
import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class BruteForceTimer extends ContinuousStateDependent implements Clock
{
  private final double delta;
  private final Energy energy;
  
  public BruteForceTimer(Collection<? extends Coordinate> requiredVariables, double delta, Energy energy)
  {
    super(requiredVariables);
    this.delta = delta;
    this.energy = energy;
  }

  @Override
  public DeltaTime next(Random random)
  {
    double prevHeight = eval(0.0);
    double refHeight = prevHeight;
    double accumulated = 0.0;
    double t = 0.0;
    double e = BPSStaticUtils.sampleUnitRateExponential(random);
    while (true)
    {
      t += delta;
      double curHeight = eval(t);
      if (curHeight >= prevHeight)
      {
        if (accumulated + (curHeight - refHeight) >= e)
          return DeltaTime.isEqualTo(t);
        prevHeight = curHeight;
      } 
      else
      {
        accumulated += prevHeight - refHeight;
        prevHeight = curHeight;
        refHeight = curHeight;
      }
    }
  }
  
  public double eval(double t)
  {
    return energy.valueAt(extrapolatePosition(t));
  }
}
