package ca.ubc.bps;

import ca.ubc.bps.energies.EnergyGradient;
import ca.ubc.pdmp.Clock;

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
