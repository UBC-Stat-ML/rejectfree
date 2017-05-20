package ca.ubc.bps.bounces;

import java.util.Collection;
import java.util.List;

import ca.ubc.bps.energies.EnergyInPlace;
import ca.ubc.bps.state.Dynamics;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.pdmp.Coordinate;

public class SeparableDBPSJump extends DiscreteBPSJump
{
  private final Dynamics dynamics;

  public SeparableDBPSJump(
      Collection<? extends Coordinate> variables, 
      List<EnergyInPlace> energies,
      double discretizationSize, 
      Dynamics dynamics)
  {
    super(variables, energies, discretizationSize);
    this.dynamics = dynamics;
  }

  @Override
  public void applyFlow()
  {
    for (PositionVelocity pv : continuousCoordinates)
      dynamics.extrapolateInPlace(discretizationSize, pv.position, pv.velocity); 
  }
}
