package ca.ubc.rejfree;

import ca.ubc.pdmp.Clock;
import ca.ubc.rejfree.energies.EnergyGradient;

public class BPSPotential
{
  public final EnergyGradient energy;
  public final Clock clock;
  public BPSPotential(EnergyGradient energy, Clock clock)
  {
    this.energy = energy;
    this.clock = clock;
  }
}
