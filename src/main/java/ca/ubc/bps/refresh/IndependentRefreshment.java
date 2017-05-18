package ca.ubc.bps.refresh;

import java.util.Collection;
import java.util.Random;

import ca.ubc.bps.state.PositionVelocityDependent;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.JumpKernel;

public class IndependentRefreshment extends PositionVelocityDependent implements JumpKernel
{
  private final boolean normalized;
  
  public IndependentRefreshment(Collection<? extends Coordinate> requiredVariables, boolean normalized)
  {
    super(requiredVariables);
    this.normalized = normalized;
  }

  @Override
  public void simulate(Random random)
  {
    simulate(random, continuousCoordinates, normalized);
  }
  
  public static void simulate(
      Random random, 
      Collection<PositionVelocity> continuousCoordinates, 
      boolean normalized)
  {
    double norm = 0.0;
    for (PositionVelocity coordinate : continuousCoordinates)
    {
      final double current = random.nextGaussian();
      coordinate.velocity.set(current);
      norm += current * current;
    }
    if (normalized)
    {
      norm = Math.sqrt(norm);
      for (PositionVelocity coordinate : continuousCoordinates)
        coordinate.velocity.set(coordinate.velocity.get() / norm);
    }
  }

}
