package ca.ubc.pdmp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ca.ubc.rejfree.energies.NormalEnergy;
import ca.ubc.rejfree.kernels.Bounce;
import ca.ubc.rejfree.kernels.Refreshment;
import ca.ubc.rejfree.processors.IntegrateTrajectory;
import ca.ubc.rejfree.state.ContinuouslyEvolving;
import ca.ubc.rejfree.state.PiecewiseLinear;
import ca.ubc.rejfree.timers.HomogeneousPP;
import ca.ubc.rejfree.timers.NormalTimer;
import rejfree.models.normal.NormalChain;
import rejfree.models.normal.NormalChainOptions;
import xlinear.DenseMatrix;
import xlinear.MatrixOperations;

public class NormalTest
{
  public static void main(String [] args)
  {
    PDMP pdmp = new PDMP();
    
    Random random = new Random(1);
    final int size = 10;
    NormalChainOptions options = new NormalChainOptions();
    options.nPairs = size - 1;
    NormalChain chain = new NormalChain(options);
    List<ContinuouslyEvolving> states = ContinuouslyEvolving.buildIsotropicNormalArray(size, PiecewiseLinear.instance, random);
    pdmp.coordinates.addAll(states);
    DenseMatrix pairPrecision = MatrixOperations.denseCopy(chain.pairPrecisions.get(0)); // all equal
    for (int i = 0; i < size - 1; i++)
    {
      List<Coordinate> reqVars = new ArrayList<>();
      reqVars.add(states.get(i));
      reqVars.add(states.get(i+1));
      NormalTimer timer = new NormalTimer(reqVars, pairPrecision);
      NormalEnergy energy = new NormalEnergy(pairPrecision);
      JumpKernel bounce = new Bounce(reqVars, energy);
      JumpProcess jp = new JumpProcess(timer, bounce);
      pdmp.jumpProcesses.add(jp);
    }
    // refreshment
    Refreshment ref = new Refreshment(states);
    HomogeneousPP homog = new HomogeneousPP(1.0);
    pdmp.jumpProcesses.add(new JumpProcess(homog, ref));
    // processors
    IntegrateTrajectory processor = new IntegrateTrajectory(states.get(0), new IntegrateTrajectory.MomentIntegrator(2));
    pdmp.processors.add(processor);
    // running
    PDMPSimulator simu = new PDMPSimulator(pdmp);
    simu.simulate(random, StoppingCriterion.byStochasticProcessTime(10_000_000.0));
    // analysis
    System.out.println(processor.integrate());
    System.out.println(chain.covarMatrix.get(0,0));
  }
}
