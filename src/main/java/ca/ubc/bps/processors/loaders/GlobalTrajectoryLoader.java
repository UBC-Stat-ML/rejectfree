package ca.ubc.bps.processors.loaders;

import static ca.ubc.bps.BPSFactoryHelpers.all;

import java.io.File;
import java.util.Collections;
import java.util.List;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import blang.inits.experiments.Experiment;
import ca.ubc.bps.BPSFactory;
import ca.ubc.bps.BPSFactory.BPS;
import ca.ubc.bps.BPSFactory.MonitoredIndices;
import ca.ubc.bps.processors.ConvertToGlobalProcessor;
import ca.ubc.bps.processors.ConvertToGlobalProcessor.GlobalProcessor;

public abstract class GlobalTrajectoryLoader extends Experiment
{
  private final BPS bps;
  private final File bpsExecFolder;
  
  public GlobalTrajectoryLoader(File bpsExecFolder)
  {
    BPSFactory bpsFactory = BPSFactory.loadBPSFactory(bpsExecFolder);
    this.bps = bpsFactory.buildBPS();
    this.bpsExecFolder = bpsExecFolder;
  }

  @Arg @DefaultValue("all")
  private MonitoredIndices variables = all;
  
  public List<Integer> indices()
  {
    List<Integer> indices = variables.get(bps.continuouslyEvolvingStates().size());
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
