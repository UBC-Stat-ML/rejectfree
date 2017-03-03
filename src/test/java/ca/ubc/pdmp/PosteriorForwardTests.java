package ca.ubc.pdmp;

import static ca.ubc.bps.StaticUtils.continuousCoordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import blang.validation.CheckStationarity;
import ca.ubc.bps.Bounces;
import ca.ubc.bps.PDMPs;
import ca.ubc.bps.Refreshments;
import ca.ubc.bps.state.ContinuouslyEvolving;
import xlinear.MatrixOperations;

public class PosteriorForwardTests
{
  static final int dim = 2;
  static final Random random = new Random(1);
  private static double bpsTrajLen = 100;
  private static int nSamples = 1000;

  public static void main(String [] args)
  {
    List<Double> 
      forwardOnly = normals(nSamples),
      forwardPosterior = forwardPosterior(true);
    
    System.out.println(CheckStationarity.tTest.pValue(forwardOnly, forwardPosterior));
    System.out.println(CheckStationarity.tTest.pValue(forwardOnly, forwardPosterior));

  }
  
  private static List<Double> forwardPosterior(boolean useNewBounce)
  {
    List<Double> result = new ArrayList<Double>();
    for (int i = 0; i < nSamples; i++)
      result.add(posterior(normals(dim), useNewBounce));
    return result;
  }

  public static double posterior(List<Double> normals, boolean useNewBounce)
  {
    PDMP pdmp = PDMPs.withLinearDynamics(dim, random);
    List<ContinuouslyEvolving> states = 
        continuousCoordinates(pdmp.coordinates);
    for (int d = 0; d < dim; d++) 
      states.get(d).position.set(normals.get(d));
    
    if (useNewBounce)
      BivariateNormalExperiment.addNewBounceNormalPotential(
          pdmp,
          states, 
          MatrixOperations.identity(dim));
    else
      Bounces.addNormalPotential(
          pdmp,
          states, 
          MatrixOperations.identity(dim));
    
    Refreshments.addGlobal(pdmp, 1.0);
    
    PDMPSimulator simu = new PDMPSimulator(pdmp);
    simu.simulate(random, StoppingCriterion.byStochasticProcessTime(bpsTrajLen ));
     return states.get(0).position.get();
  }
  
  private static List<Double> normals(int n)
  {
    List<Double> result = new ArrayList<Double>();
    for (int i = 0; i < nSamples; i++)
      result.add(random.nextGaussian());
    return result;
  }
}
