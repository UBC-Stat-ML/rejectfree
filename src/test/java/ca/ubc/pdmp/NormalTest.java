package ca.ubc.pdmp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ca.ubc.rejfree.Refreshments;
import ca.ubc.rejfree.energies.NormalEnergy;
import ca.ubc.rejfree.kernels.Bounce;
import ca.ubc.rejfree.processors.SaveTrajectory;
import ca.ubc.rejfree.state.ContinuouslyEvolving;
import ca.ubc.rejfree.state.PiecewiseLinear;
import ca.ubc.rejfree.timers.NormalClock;
import rejfree.models.normal.NormalChain;
import rejfree.models.normal.NormalChainOptions;
import xlinear.DenseMatrix;
import xlinear.MatrixOperations;

public class NormalTest
{
  public static void main(String [] args)
  {
    Random random = new Random(1);
    final int size = 10;
    List<ContinuouslyEvolving> states = ContinuouslyEvolving.buildIsotropicNormalArray(size, PiecewiseLinear.instance, random);
    PDMP pdmp = new PDMP(states);
    
    NormalChainOptions options = new NormalChainOptions();
    options.nPairs = size - 1;
    NormalChain chain = new NormalChain(options);
    
    DenseMatrix pairPrecision = MatrixOperations.denseCopy(chain.pairPrecisions.get(0)); // all equal
    for (int i = 0; i < size - 1; i++)
    {
      List<Coordinate> reqVars = new ArrayList<>();
      reqVars.add(states.get(i));
      reqVars.add(states.get(i+1));
      NormalClock timer = new NormalClock(reqVars, pairPrecision);
      NormalEnergy energy = new NormalEnergy(pairPrecision);
      JumpKernel bounce = new Bounce(reqVars, energy);
      JumpProcess jp = new JumpProcess(timer, bounce);
      pdmp.jumpProcesses.add(jp);
    }
    // refreshment
    Refreshments.addGlobal(pdmp, 1.0);
    // processors
    SaveTrajectory processor = new SaveTrajectory(states.get(0));
    pdmp.processors.add(processor);
    // running
    PDMPSimulator simu = new PDMPSimulator(pdmp);
    simu.simulate(random, StoppingCriterion.byStochasticProcessTime(100_000));
    // analysis
    System.out.println("Approx = " + processor.getTrajectory().moment(2)); //integrate());
    System.out.println("Exact  = " + chain.covarMatrix.get(0,0));
    System.out.println("ESS = " + processor.getTrajectory().momentEss(2));
  }
}
