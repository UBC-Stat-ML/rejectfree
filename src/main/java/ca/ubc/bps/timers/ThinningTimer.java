package ca.ubc.bps.timers;

import java.util.Collection;
import java.util.Random;

import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.energies.Energy;
import ca.ubc.bps.state.PositionVelocityDependent;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class ThinningTimer extends PositionVelocityDependent implements Clock
{
  public final Energy energy;
  public final PoissonProcess intensityUpperBound;

  public ThinningTimer(
      Collection<? extends Coordinate> requiredVariables, 
      Energy energy,
      PoissonProcess intensityUpperBound)
  {
    super(requiredVariables);
    this.energy = energy;
    this.intensityUpperBound = intensityUpperBound;
  }

  @Override
  public DeltaTime next(Random random)
  {
    DeltaTime proposal = intensityUpperBound.next(random);
    
    if (proposal.isBound)
      return proposal;
    
    if (BPSStaticUtils.sampleBernoulli(random, acceptanceProbability(proposal.deltaTime)))
      return DeltaTime.isEqualTo(proposal.deltaTime);
    else
      return DeltaTime.isGreaterThan(proposal.deltaTime);
  }

  private double acceptanceProbability(double deltaT)
  {
    double [] velocity = extrapolateVelocity(deltaT);
    double [] position = extrapolatePosition(deltaT);
    double num = StandardIntensity.canonicalRate(velocity, energy.gradient(position));
    double denom = intensityUpperBound.evaluate(deltaT);
    if (denom < num)
      throw new RuntimeException();
    return num / denom;
  }
}
