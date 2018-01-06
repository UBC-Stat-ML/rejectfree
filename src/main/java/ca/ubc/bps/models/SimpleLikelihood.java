package ca.ubc.bps.models;

import java.io.File;
import java.io.Writer;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import blang.inits.GlobalArg;
import blang.inits.experiments.ExperimentResults;
import briefj.BriefIO;
import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.factory.ModelBuildingContext;
import ca.ubc.bps.state.PositionVelocity;

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
  @Arg
  public Optional<File> dataFile = Optional.empty();
  
  @GlobalArg
  public ExperimentResults results = new ExperimentResults();
  
  @Arg(description = "If 1, generate one data point per latent, otherwise, skip every n")
                      @DefaultValue("1")
  public int generatedDataSparsity = 1;

  @Override
  public void setup(ModelBuildingContext context, List<PositionVelocity> vars)
  {
    if (dataFile.isPresent())
      setupWithLoadedData(context, vars);
    else
      setupWithGeneratedData(context, vars);
  }
  
  private void setupWithLoadedData(ModelBuildingContext context, List<PositionVelocity> vars) 
  {
    int lineIndex = 0;
    for (String line : BriefIO.readLines(dataFile.get()))
      if (!line.trim().isEmpty())
      {
        PositionVelocity latent = vars.get(lineIndex++);
        if (!line.trim().toLowerCase().equals("na"))
        {
          T loaded = parse(line.trim());
          context.registerBPSPotential(createLikelihoodPotential(latent, loaded));
        }
      }
    if (lineIndex != vars.size())
      throw new RuntimeException("Expected exactly " + lineIndex + " non empty lines in " + dataFile.get());
  }

  private void setupWithGeneratedData(ModelBuildingContext context, List<PositionVelocity> vars)
  {
    Writer writer = results.getAutoClosedBufferedWriter("observations.csv");
    BriefIO.println(writer, "index,observation");
    int i = 0;
    for (PositionVelocity latent : vars)
    {
      if (i % generatedDataSparsity == 0)
      {
        T sampledObservations = sampleDatapoint(latent.position.get(), context.initializationRandom);
        context.registerBPSPotential(createLikelihoodPotential(latent, sampledObservations));
        BriefIO.println(writer, "" + (i) + "," + sampledObservations);
      }
      i++;
    }
  }
  
  protected T sampleDatapoint(double latentVariable, Random random)
  {
    throw new RuntimeException("Not implemented");
  }
  
  protected T parse(String string)
  {
    throw new RuntimeException("Not implemented");
  }
  
  public abstract BPSPotential createLikelihoodPotential(PositionVelocity latentVariable, T observation);
}
