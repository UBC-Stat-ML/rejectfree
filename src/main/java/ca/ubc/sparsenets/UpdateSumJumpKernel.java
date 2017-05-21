package ca.ubc.sparsenets;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.state.MutableDouble;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.JumpKernel;

public class UpdateSumJumpKernel implements JumpKernel
{
  final PositionVelocity sum;
  final JumpKernel enclosed;
  final List<PositionVelocity> evolvingCoordinates;
  PositionVelocity singleCoordinate;
  boolean initialized = false;

  public UpdateSumJumpKernel(PositionVelocity sum, JumpKernel enclosed)
  {
    this.sum = sum;
    this.enclosed = enclosed;
    this.evolvingCoordinates = BPSStaticUtils.continuousCoordinates(enclosed.requiredVariables());
    singleCoordinate = evolvingCoordinates.size() == 1 ? evolvingCoordinates.get(0) : null;
  }

  @Override
  public Collection<? extends Coordinate> requiredVariables()
  {
    Collection<Coordinate> result = new LinkedHashSet<>();
    result.addAll(enclosed.requiredVariables());
    result.add(sum);
    return result;
  }

  @Override
  public void simulate(Random random)
  {
    double 
      xBU = singleCoordinate == null ? Double.NaN : singleCoordinate.position.get(),
      vBU = singleCoordinate == null ? Double.NaN : singleCoordinate.velocity.get();
    enclosed.simulate(random);
    if (singleCoordinate == null)
    {
      // recompute from scratch
      recompute(true,  sum.position, evolvingCoordinates);
      recompute(false, sum.velocity, evolvingCoordinates);
    }
    else
    {
      // use delta
      updateByDelta(singleCoordinate.position, xBU, sum.position);
      updateByDelta(singleCoordinate.velocity, vBU, sum.velocity);
    }
  }

  public static void recompute(boolean isPos, MutableDouble sumCoord, List<PositionVelocity> evolvingCoordinates)
  {
    double sum = 0.0;
    for (PositionVelocity c : evolvingCoordinates)
      sum += (isPos ? c.position : c.velocity).get();
    sumCoord.set(sum); 
  }

  private void updateByDelta(MutableDouble position, double xBU, MutableDouble sum)
  {
    double delta = position.get() - xBU;
    sum.set(sum.get() + delta); 
  }
}
