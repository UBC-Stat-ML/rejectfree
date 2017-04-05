package ca.ubc.bps.processors.loaders;

import static ca.ubc.bps.factory.BPSFactoryHelpers.all;

import java.io.File;
import java.util.Collections;
import java.util.List;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import blang.inits.experiments.Experiment;
import ca.ubc.bps.factory.BPSFactory;
import ca.ubc.bps.factory.BPSFactory.BPS;
import ca.ubc.bps.factory.BPSFactory.MonitoredIndices;
import ca.ubc.bps.processors.ConvertToGlobalProcessor;
import ca.ubc.bps.processors.ConvertToGlobalProcessor.GlobalProcessor;

public abstract class GlobalTrajectoryLoader extends Experiment
{
  private final BPS bps;
  private final BPSFactory bpsFactory;
  private final File bpsExecFolder;
  
  public GlobalTrajectoryLoader(File bpsExecFolder)
  {
    this.bpsFactory = BPSFactory.loadBPSFactory(bpsExecFolder, results);
    this.bps = bpsFactory.buildBPS();
    this.bpsExecFolder = bpsExecFolder;
  }

  @Arg @DefaultValue("all")
  private MonitoredIndices variables = all;
  
  /**
   * @return The intersection of the requested indices and those that were output.
   */
  public List<Integer> indices()
  {
    final int totalNumber = bps.continuouslyEvolvingStates().size();
    List<Integer> indices = variables.get(totalNumber);
    indices.retainAll(bpsFactory.write.get(totalNumber));
    Collections.sort(indices);
    return indices;
  }
  
  @Override
  public void run()
  {
    // read the trajectories
    List<Integer> indices = indices();
    ConvertToGlobalProcessor converter = new ConvertToGlobalProcessor(createGlobalProcessor(bps));
    for (int index : indices)
    {
      MemorizeTrajectoryLoader trajLoader = new MemorizeTrajectoryLoader(bps, TrajectoryLoader.getSampleFile(bpsExecFolder, index), index);
      converter.addTrajectory(index, trajLoader.getTrajectory());
    }
    converter.convert();
  }
  
  public abstract GlobalProcessor createGlobalProcessor(BPS bps);
}
