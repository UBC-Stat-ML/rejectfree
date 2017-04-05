package ca.ubc.pdmp;

import static ca.ubc.bps.StaticUtils.continuousCoordinates;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

import ca.ubc.bps.PDMPs;
import ca.ubc.bps.bounces.Bounces;
import ca.ubc.bps.processors.MemorizeTrajectory;
import ca.ubc.bps.processors.Trajectory;
import ca.ubc.bps.refresh.Refreshments;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.pdmp.BivariateNormalExperiment.MyRegression;
import xlinear.Matrix;
import xlinear.MatrixOperations;
import xlinear.SparseMatrix;

public class QuickScalingTest
{
  
  private static Random random = new Random(1);
  private static boolean useId = false;
  private static double bpsTrajLen = 10_000;

  public static void main(String [] args) 
  {
    System.out.println("trajLen=" + bpsTrajLen);
    System.out.println("useId=" + useId);
    

    
    System.out.println("=== BPS ===");
    analyseCost(QuickScalingTest::bps, 100, 1000, 10);
    
    System.out.println("=== Randomized Bounce With Refresh ===");
    analyseCost(QuickScalingTest::newBouncesWithRefresh, 100, 1000, 10);
    
    System.out.println("=== Randomized Bounce No Refresh ===");
    analyseCost(QuickScalingTest::newBouncesNoRefresh, 100, 1000, 10);
    
    System.out.println("=== Flip ===");
    analyseCost(QuickScalingTest::flip, 100, 1000, 10);
    
//    System.out.println("=== HMC ===");
//    analyseCost(BivariateNormalExperiment::hmc, 40, 320, 2);
//    
//    System.out.println("=== HMC (optimized) ===");
//    analyseCost(BivariateNormalExperiment::optimizedHmc, 1000, 10_000, 10);
//    
//    System.out.println("=== Gibbs ===");
//    analyseCost(BivariateNormalExperiment::gibbs, 10, 80, 2);
//    
//    System.out.println("=== BPS ===");
//    analyseCost(BivariateNormalExperiment::bps, 1000, 10_000, 10);
  }

  private static void analyseCost(
      Function<Integer,Double> costFct, 
      int min, 
      int max, 
      int factor)
  {
    MyRegression costReg = new MyRegression();
    // compute ESS for various values of alpha
    for (int dim = min; dim <= max; dim *= factor) 
    {
      System.out.println("dim=" + dim);
      double cost = costFct.apply(dim);
      costReg.add(Math.log(dim), Math.log(cost));
    }
    // plot and compute coeff
    costReg.report();
  }
  
  private static double newBouncesNoRefresh(int dim)
  {
    return newBounces(dim, false);
  }
  
  private static double newBouncesWithRefresh(int dim)
  {
    return newBounces(dim, true);
  }
  
  private static Matrix precisionMatrix(int dim)
  {
    if (useId)
      return MatrixOperations.identity(dim);
    else
    {
      SparseMatrix s = MatrixOperations.sparse(dim, dim);
      for (int i = 0; i < dim; i++)
      {
        double variance = Math.pow(10.0, (1.0 + 5.0 * (0.0 + i) / (dim - 1.0)));
        s.set(i, i, 1.0 / variance);
      }
      return s;
    }
  }
  
  private static double newBounces(int dim, boolean refresh)
  {
    PDMP pdmp = PDMPs.withLinearDynamics(dim, random);
    List<ContinuouslyEvolving> states = 
        continuousCoordinates(pdmp.coordinates);
    for (int d = 0; d < dim; d++) 
      states.get(d).position.set(random.nextGaussian());
    
    BivariateNormalExperiment.addNewBounceNormalPotential(
        pdmp,
        states, 
        precisionMatrix(dim));
    
    // no need for refresh
    if (refresh)
      Refreshments.addGlobal(pdmp, 1.0);
    
    MemorizeTrajectory processor = new MemorizeTrajectory(states.get(0));
    pdmp.processors.add(processor);
    PDMPSimulator simu = new PDMPSimulator(pdmp);
    simu.setPrintSummaryStatistics(false);
    simu.simulate(random, StoppingCriterion.byStochasticProcessTime(bpsTrajLen));
    
    quickReport(processor.getTrajectory());
    System.out.println("\tcost=" + simu.getNumberOfJumps());
    
    return simu.getNumberOfJumps() * dim;
  }
  
  private static double flip(int dim)
  {
    PDMP pdmp = PDMPs.withLinearDynamics(dim, random);
    List<ContinuouslyEvolving> states = 
        continuousCoordinates(pdmp.coordinates);
    for (int d = 0; d < dim; d++) 
      states.get(d).position.set(random.nextGaussian());
    
    BivariateNormalExperiment.addFlip(
        pdmp,
        states, 
        precisionMatrix(dim));
    
    // no need for refresh
    Refreshments.addGlobal(pdmp, 1.0);
    
    MemorizeTrajectory processor = new MemorizeTrajectory(states.get(0));
    pdmp.processors.add(processor);
    PDMPSimulator simu = new PDMPSimulator(pdmp);
    simu.setPrintSummaryStatistics(false);
    simu.simulate(random, StoppingCriterion.byStochasticProcessTime(bpsTrajLen));
    
    quickReport(processor.getTrajectory());
    System.out.println("\tcost=" + simu.getNumberOfJumps());
    
    return simu.getNumberOfJumps() * dim;
  }
  
  private static double bps(int dim)
  {
    PDMP pdmp = PDMPs.withLinearDynamics(dim, random);
    List<ContinuouslyEvolving> states = 
        continuousCoordinates(pdmp.coordinates);
    for (int d = 0; d < dim; d++) 
      states.get(d).position.set(random.nextGaussian());
    
    Bounces.addNormalPotential(
        pdmp,
        states, 
        precisionMatrix(dim));
    
    // no need for refresh
     Refreshments.addGlobal(pdmp, 1.0);
    
    MemorizeTrajectory processor = new MemorizeTrajectory(states.get(0));
    pdmp.processors.add(processor);
    PDMPSimulator simu = new PDMPSimulator(pdmp);
    simu.setPrintSummaryStatistics(false);
    simu.simulate(random, StoppingCriterion.byStochasticProcessTime(bpsTrajLen));
    
    quickReport(processor.getTrajectory());
    System.out.println("\tcost=" + simu.getNumberOfJumps());
    
    return simu.getNumberOfJumps() * dim;
  }

  private static void quickReport(Trajectory trajectory)
  {
    for (int degree = 1; degree <= 2; degree++)
    {
      System.out.println("\tess(" + degree + ")=" + trajectory.momentEss(degree));
      System.out.println("\tmoment(" + degree + ")=" + trajectory.moment(degree));
    }
  }
}
