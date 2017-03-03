package ca.ubc.pdmp;

import static ca.ubc.bps.StaticUtils.continuousCoordinates;
import static xlinear.MatrixExtensions.vectorToArray;
import static xlinear.MatrixOperations.denseCopy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jblas.DoubleMatrix;

import com.google.common.collect.ImmutableList;

import bayonet.distributions.Normal;
import bayonet.rplot.PlotLine;
import ca.ubc.bps.Bounces;
import ca.ubc.bps.PDMPs;
import ca.ubc.bps.Refreshments;
import ca.ubc.bps.energies.EnergyGradient;
import ca.ubc.bps.energies.NormalEnergy;
import ca.ubc.bps.kernels.Bounce;
import ca.ubc.bps.processors.EffectiveSampleSize;
import ca.ubc.bps.processors.SaveTrajectory;
import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.timers.NormalClock;
import hmc.DataStruct;
import rejfree.StaticUtils;
import utils.MultiVariateObj;
import utils.Objective;
import xlinear.DenseMatrix;
import xlinear.Matrix;
import xlinear.MatrixOperations;
import static xlinear.MatrixExtensions.*;

public class BivariateNormalExperiment
{
  private static Random random = new Random(1);
  
  private static double bpsTrajLen = 10_000;

  public static void main(String [] args) 
  {
    System.out.println("=== New Bounce ===");
    analyseCost(BivariateNormalExperiment::newBounces, 1000, 100_000, 10);
    
    System.out.println("=== BPS ===");
    analyseCost(BivariateNormalExperiment::bps, 1000, 100_000, 10);
    
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
  

  private static void analyseCost(Function<Double,Double> costFct, double min, double max, double factor)
  {
    MyRegression costReg = new MyRegression();
    // compute ESS for various values of alpha
    for (double alpha = min; alpha <= max; alpha *= factor) 
    {
      System.out.println("alpha=" + alpha);
      double cost = costFct.apply(rho(alpha));
      costReg.add(Math.log(alpha), Math.log(cost));
    }
    // plot and compute coeff
    costReg.report();
  }
  
  public static class MyRegression
  {
    List<Double> 
      xs = new ArrayList<Double>(), 
      ys = new ArrayList<Double>();
    SimpleRegression reg = new SimpleRegression();
    
    void add(double x, double y)
    {
      xs.add(x);
      ys.add(y);
      reg.addData(x, y);
    }
    
    void report()
    {
      System.out.println("slope="+reg.getSlope());
      PlotLine.from(xs, ys).openTemporaryPDF();
    }
  }
  
  private static double rho(double alpha) 
  {
    return Math.sqrt(1.0 - 1.0/alpha);
  }
  
  private static double newBounces(double rho)
  {
    PDMP pdmp = PDMPs.withLinearDynamics(2, random);
    List<ContinuouslyEvolving> states = 
        continuousCoordinates(pdmp.coordinates);
    states.get(0).position.set(random.nextGaussian());
    states.get(1).position.set(random.nextGaussian());
    states.get(0).velocity.set(0);
    states.get(1).velocity.set(1);
    
    addNewBounceNormalPotential(
        pdmp,
        ImmutableList.of(
            states.get(0), 
            states.get(1)), 
        precision(rho));
    
    // no need for refresh
    // Refreshments.addGlobal(pdmp, 1.0);
    
    SaveTrajectory processor = new SaveTrajectory(states.get(0));
    pdmp.processors.add(processor);
    PDMPSimulator simu = new PDMPSimulator(pdmp);
    simu.setPrintSummaryStatistics(false);
    simu.simulate(random, StoppingCriterion.byStochasticProcessTime(bpsTrajLen));
    
    System.out.println("\trho=" + rho);
    System.out.println("\tess=" + processor.getTrajectory().momentEss(2));
    System.out.println("\tmoment=" + processor.getTrajectory().moment(2));
    System.out.println("\tcost=" + simu.getNumberOfJumps());
    
    return simu.getNumberOfJumps();
  }
  
  public static void addNewBounceNormalPotential(
      PDMP pdmp,
      List<ContinuouslyEvolving> variables, 
      Matrix precision)
  {
    NormalClock timer = new NormalClock(variables, precision);
    NormalEnergy energy = new NormalEnergy(precision);
    JumpKernel bounce =  new RandomizedBounce(variables, energy);
    pdmp.jumpProcesses.add(new JumpProcess(timer, bounce));
  }
  
  public static void addFlip(
      PDMP pdmp,
      List<ContinuouslyEvolving> variables, 
      Matrix precision)
  {
    NormalClock timer = new NormalClock(variables, precision);
    NormalEnergy energy = new NormalEnergy(precision);
    JumpKernel bounce =  new FlipBounce(variables, energy);
    pdmp.jumpProcesses.add(new JumpProcess(timer, bounce));
  }
  
//  static public class NewBounce extends ContinuousStateDependent implements JumpKernel
//  {
//    private final EnergyGradient energy;
//
//    public NewBounce(List<ContinuouslyEvolving> requiredVariables, EnergyGradient energy)
//    {
//      super(requiredVariables);
//      this.energy = energy;
//    }
//
//    @Override
//    public void simulate(Random random)
//    {
//      DoubleMatrix gradient = new DoubleMatrix(energy.gradient(currentPosition()));
//      
//      DoubleMatrix oldVelocity = //new DoubleMatrix(currentVelocity());
//          new DoubleMatrix(gradient.length);
//      
//      double oldSign = oldVelocity.dot(gradient);
//      for (int i = 0; i < gradient.length; i++)
//        oldVelocity.put(i, random.nextGaussian());
//      oldVelocity = oldVelocity.mul(1.0/oldVelocity.norm2());
//      if (oldSign * (oldVelocity.dot(gradient)) < 0)
//        oldVelocity = oldVelocity.mul(-1.0);
//      
//      DoubleMatrix n1 = gradient.mul(-1.0 / gradient.norm2());
//      DoubleMatrix n2 = oldVelocity.sub(n1.mul(oldVelocity.dot(n1)));
//      n2 = n2.mul(1.0 / n2.norm2());
//      
//      double a = Math.pow(random.nextDouble(), 1.0 / ((double) gradient.length - 1));
//      double b = Math.sqrt(1.0 - a * a);
//      
//      DoubleMatrix newVelocity = n2.mul(b).add(n1.mul(a));
//      
//      setVelocity(newVelocity.data);
//    }
//  }
  
  private static double bps(double rho)
  {
    PDMP pdmp = PDMPs.withLinearDynamics(2, random);
    List<ContinuouslyEvolving> states = 
        continuousCoordinates(pdmp.coordinates);
    states.get(0).position.set(random.nextGaussian());
    states.get(1).position.set(random.nextGaussian());
    Bounces.addNormalPotential(
        pdmp,
        ImmutableList.of(
            states.get(0), 
            states.get(1)), 
        precision(rho));
    Refreshments.addGlobal(pdmp, 1.0);
    SaveTrajectory processor = new SaveTrajectory(states.get(0));
    pdmp.processors.add(processor);
    PDMPSimulator simu = new PDMPSimulator(pdmp);
    simu.setPrintSummaryStatistics(false);
    simu.simulate(random, StoppingCriterion.byStochasticProcessTime(bpsTrajLen));
    
    System.out.println("\trho=" + rho);
    System.out.println("\tess=" + processor.getTrajectory().momentEss(2));
    System.out.println("\tmoment=" + processor.getTrajectory().moment(2));
    System.out.println("\tcost=" + simu.getNumberOfJumps());
    
    return simu.getNumberOfJumps();
  }
  
  private static double gibbs(double rho)
  {
    double [] state = new double[2];
    int nIters = 10_000_000;
    List<Double> samples = new ArrayList<>();
    double conditionalVariance = 1.0 - rho * rho;
    
    for (int i = 0; i < nIters; i++)
    {
      int cur = i % 2;
      int other = (i + 1) % 2;
      double otherValue = state[other];
      state[cur] = Normal.generate(random, rho * otherValue, conditionalVariance);
      samples.add(state[0]);
    }
    
    double ess = EffectiveSampleSize.ess(samples);
    double cost = nIters;
    
    System.out.println("\trho=" + rho);
    System.out.println("\tess=" + ess);
    System.out.println("\tmoment=" + samples.stream().mapToDouble(d -> d * d).average().getAsDouble());
    System.out.println("\tcost=" + cost/ess);
    
    return cost/ess;
  }
  
  private static double hmc(double rho) 
  {
    return hmc(rho, false);
  }
  
  private static double optimizedHmc(double rho)
  {
    return hmc(rho, true);
  }
  
  private static double hmc(double rho, boolean optimized)
  {
    HMCNormalEnergy target = new HMCNormalEnergy(precision(rho));
    double epsilon = optimized ? Math.sqrt(1.0 - rho * rho) : Math.sqrt(1.0 - 0.94 * 0.94);
    int l = (int) (5.0 * 1.0 / epsilon);
    DoubleMatrix sample = new DoubleMatrix(2);
    List<Double> samples = new ArrayList<>();
    int nIters = 1_000_000;
    for (int i = 0; i < nIters; i++)
    {
      DataStruct result = hmc.HMC.doIter(random, l, epsilon, sample, target, target, true);
      sample = result.next_q;
      samples.add(sample.get(0));
    }
    double ess = EffectiveSampleSize.ess(samples);
    double cost = nIters * l;
    
    System.out.println("\trho=" + rho);
    System.out.println("\tess=" + ess);
    System.out.println("\tmoment=" + samples.stream().mapToDouble(d -> d * d).average().getAsDouble());
    System.out.println("\tcost=" + cost/ess);
    
    return cost/ess;
  }
  
  public static class HMCNormalEnergy implements Objective, MultiVariateObj 
  {
    private final Matrix precision;

    public HMCNormalEnergy(Matrix precision)
    {
      this.precision = precision;
    }

    @Override
    public DoubleMatrix mFunctionValue(DoubleMatrix vec)
    {
      DenseMatrix position = denseCopy(vec);
      return new DoubleMatrix(vectorToArray(precision.mul(position)));
    }

    @Override
    public double functionValue(DoubleMatrix vec)
    {
      DenseMatrix position = denseCopy(vec);
      return 0.5 * dot(position, precision.mul(position));
    }
  }

  private static Matrix precision(double rho)
  {
    Matrix covariance = MatrixOperations.denseCopy(new double[][] {
      {1.0, rho},
      {rho, 1.0}
    });
    return covariance.inverse();
  }
}
