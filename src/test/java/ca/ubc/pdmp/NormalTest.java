package ca.ubc.pdmp;

import static ca.ubc.bps.StaticUtils.*;

import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;

import ca.ubc.bps.Bounces;
import ca.ubc.bps.PDMPs;
import ca.ubc.bps.Refreshments;
import ca.ubc.bps.processors.SaveTrajectory;
import ca.ubc.bps.state.ContinuouslyEvolving;
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
    PDMP pdmp = PDMPs.withLinearDynamics(size, random);
    List<ContinuouslyEvolving> states = 
        continuousCoordinates(pdmp.coordinates);
    
    NormalChainOptions options = new NormalChainOptions();
    options.nPairs = size - 1;
    NormalChain chain = new NormalChain(options);
    
    DenseMatrix pairPrecision = MatrixOperations.denseCopy(chain.pairPrecisions.get(0)); // all equal
    for (int i = 0; i < size - 1; i++)
      Bounces.addNormalPotential(
          pdmp, 
          ImmutableList.of(
              states.get(i), 
              states.get(i+1)), 
          pairPrecision);

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
