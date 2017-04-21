package ca.ubc.bps;

import ca.ubc.bps.energies.Energy;
import ca.ubc.pdmp.Clock;

public class BPSPotential
{
  public final Energy energy;
  public final Clock clock;
  public BPSPotential(Energy energy, Clock clock)
  {
    this.energy = energy;
    this.clock = clock;
  }
}
