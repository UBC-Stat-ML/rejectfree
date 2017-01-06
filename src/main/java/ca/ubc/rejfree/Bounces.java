package ca.ubc.rejfree;

import java.util.List;

import ca.ubc.pdmp.JumpKernel;
import ca.ubc.pdmp.JumpProcess;
import ca.ubc.pdmp.PDMP;
import ca.ubc.rejfree.energies.NormalEnergy;
import ca.ubc.rejfree.kernels.Bounce;
import ca.ubc.rejfree.state.ContinuouslyEvolving;
import ca.ubc.rejfree.timers.NormalClock;
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
