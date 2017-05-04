package ca.ubc.bps.models;

import java.io.Writer;
import java.util.List;
import java.util.Random;

import blang.inits.GlobalArg;
import blang.inits.experiments.ExperimentResults;
import briefj.BriefIO;
import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.factory.ModelBuildingContext;
import ca.ubc.bps.state.ContinuouslyEvolving;

/**
 * Used to cover simple cases where all continuously evolving variables in the model are points of 
 * the same data type e.g. in a univariate field. 
 * 
 * The we can simply treat adding a likelihood on each variable.
 * 
 * @author bouchard
 * 
 * T type for the observation
 *
 */
public abstract class SimpleLikelihood<T> implements Likelihood
{
  // TODO: offer option to load data instead of generating it
  
  @GlobalArg
  public ExperimentResults results = new ExperimentResults();

  @Override
  public void setup(ModelBuildingContext context, List<ContinuouslyEvolving> vars)
  {
    Writer writer = results.getAutoClosedBufferedWriter("observations.csv");
    BriefIO.println(writer, "index,observation");
    int i = 0;
    for (ContinuouslyEvolving latent : vars)
    {
      T sampledObservations = sampleDatapoint(latent.position.get(), context.initializationRandom);
      context.registerBPSPotential(createLikelihoodPotential(latent, sampledObservations));
      BriefIO.println(writer, "" + (i++) + "," + sampledObservations);
    }
  }
  
  public abstract T sampleDatapoint(double latentVariable, Random random);
  public abstract BPSPotential createLikelihoodPotential(ContinuouslyEvolving latentVariable, T observation);
}
