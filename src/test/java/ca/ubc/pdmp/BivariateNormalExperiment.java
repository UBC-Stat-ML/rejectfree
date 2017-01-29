package ca.ubc.pdmp;

import static ca.ubc.bps.StaticUtils.continuousCoordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.google.common.collect.ImmutableList;

import bayonet.rplot.PlotLine;
import ca.ubc.bps.Bounces;
import ca.ubc.bps.PDMPs;
import ca.ubc.bps.Refreshments;
import ca.ubc.bps.Trajectory;
import ca.ubc.bps.processors.SaveTrajectory;
import ca.ubc.bps.state.ContinuouslyEvolving;
import xlinear.Matrix;
import xlinear.MatrixOperations;

public class BivariateNormalExperiment
{
  private static Random random = new Random(1);

  public static void main(String [] args) 
  {
    MyRegression reg = new MyRegression();
    // compute ESS for various values of alpha
    for (double alpha = 1.0; alpha <= 10.0; alpha *= 1.2) 
    {
      System.out.println("alpha=" + alpha);
      double meanEss = meanEss(rho(alpha));
      reg.add(Math.log(alpha), Math.log(meanEss));
    }
    // plot and compute coeff
    reg.report();
  }
  
  private static class MyRegression
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
      System.out.println(reg.getSlope());
      PlotLine.from(xs, ys).openTemporaryPDF();
    }
  }
  
  private static double rho(double alpha) 
  {
    return Math.sqrt(1.0 - 1.0/alpha);
  }
  
  private static double meanEss(double rho)
  {
    SummaryStatistics summary = new SummaryStatistics();
    for (int i = 0; i < 1000; i++) 
      summary.addValue(ess(rho));
    System.out.println("rho=" + rho + ",ess=" + summary.getMean() + ",CI=" + 2.0*summary.getStandardDeviation() / Math.sqrt(summary.getN()));
    return summary.getMean();
  }
  
  private static double ess(double rho)
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
    simu.simulate(random, StoppingCriterion.byStochasticProcessTime(1_000_000));
    Trajectory burnedOut = processor.getTrajectory().burnOut(0.5);
    System.out.println(burnedOut.moment(1));
    return burnedOut.momentEss(2);
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
