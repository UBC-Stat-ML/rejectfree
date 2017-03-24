package ca.ubc.pdmp;

import static ca.ubc.bps.StaticUtils.*;

import java.util.List;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.google.common.collect.ImmutableList;

import ca.ubc.bps.Bounces;
import ca.ubc.bps.PDMPs;
import ca.ubc.bps.Refreshments;
import ca.ubc.bps.Trajectory;
import ca.ubc.bps.processors.MemorizeTrajectory;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.timers.NormalClock;
import rejfree.models.normal.NormalChain;
import rejfree.models.normal.NormalChainOptions;
import xlinear.DenseMatrix;
import xlinear.MatrixOperations;

public class NormalTest
{
  public static void main(String [] args)
  {
    Random random = new Random(1);
    final int size = 2;
    PDMP pdmp = PDMPs.withLinearDynamics(size, random);
    List<ContinuouslyEvolving> states = 
        continuousCoordinates(pdmp.coordinates);
    
    NormalChainOptions options = new NormalChainOptions();
    options.nPairs = size - 1;
    options.offDiag = 0.0;
    NormalChain chain = new NormalChain(options);
    
    DenseMatrix pairPrecision = MatrixOperations.denseCopy(chain.pairPrecisions.get(0)); // all equal
    for (int i = 0; i < size - 1; i++)
      Bounces.addNormalPotential(
          pdmp, 
          ImmutableList.of(
              states.get(i), 
              states.get(i+1)), 
          pairPrecision);

    int stateIndex = 0;
    
    // refreshment
    Refreshments.addGlobal(pdmp, 1.0);
    // processors
    MemorizeTrajectory processor = new MemorizeTrajectory(states.get(stateIndex));
    pdmp.processors.add(processor);
    // running
    PDMPSimulator simu = new PDMPSimulator(pdmp);
//    simu.setMaxTrajectoryLengthPerChunk(Double.POSITIVE_INFINITY); 
    simu.simulate(random, StoppingCriterion.byStochasticProcessTime(1_000_000));
    // some statistics
    Trajectory trajectory = processor.getTrajectory();
    System.out.println("Approx = " + trajectory.moment(2)); 
    System.out.println("Exact  = " + chain.covarMatrix.get(stateIndex, stateIndex));
    System.out.println("ESS = " + trajectory.momentEss(2));
    SummaryStatistics segLenStats = trajectory.segmentLengthSummaryStatistics();
    System.out.println("segment lengths mean = " + segLenStats.getMean() + " SD = " + segLenStats.getStandardDeviation());
  }
}
