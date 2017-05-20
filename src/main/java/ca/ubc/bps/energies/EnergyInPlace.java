package ca.ubc.bps.energies;

import java.util.Collection;

import ca.ubc.bps.state.PositionVelocityDependent;
import ca.ubc.pdmp.Coordinate;

public class EnergyInPlace extends PositionVelocityDependent
{
  public final Energy energy;
  
  public EnergyInPlace(
      Collection<? extends Coordinate> requiredVariables, 
      Energy energy)
  {
    super(requiredVariables);
    this.energy = energy;
  }

  public double [] gradient()
  {
    return energy.gradient(currentPosition());
  }
  
  public double valueAt()
  {
    return energy.valueAt(currentPosition());
  }
}
