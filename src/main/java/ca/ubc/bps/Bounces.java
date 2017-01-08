package ca.ubc.bps;

import java.util.List;

import ca.ubc.bps.energies.NormalEnergy;
import ca.ubc.bps.kernels.Bounce;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.timers.NormalClock;
import ca.ubc.pdmp.JumpKernel;
import ca.ubc.pdmp.JumpProcess;
import ca.ubc.pdmp.PDMP;
import xlinear.Matrix;

public class Bounces
{
  public static void addNormalPotential(
      PDMP pdmp,
      List<ContinuouslyEvolving> variables, 
      Matrix precision)
  {
    NormalClock timer = new NormalClock(variables, precision);
    NormalEnergy energy = new NormalEnergy(precision);
    JumpKernel bounce = new Bounce(variables, energy);
    pdmp.jumpProcesses.add(new JumpProcess(timer, bounce));
  }
}
