package ca.ubc.bps.processors.loaders;

import java.io.BufferedWriter;
import java.io.File;

import com.google.common.base.Joiner;

import blang.inits.ConstructorArg;
import blang.inits.DesignatedConstructor;
import blang.inits.experiments.Experiment;
import briefj.BriefIO;
import ca.ubc.bps.factory.BPSFactory;
import ca.ubc.bps.factory.BPSFactory.BPS;
import ca.ubc.bps.processors.ConvertToGlobalProcessor.GlobalProcessor;
import ca.ubc.bps.processors.ConvertToGlobalProcessor.GlobalProcessorContext;
import ca.ubc.bps.state.ContinuouslyEvolving;

public class WriteGlobalTrajectory extends GlobalTrajectoryLoader
{
  @DesignatedConstructor
  public WriteGlobalTrajectory(@ConstructorArg("bpsExecFolder") File bpsExecFolder)
  {
    super(bpsExecFolder);
  }

  public static String GLOBAL_TRAJ_FILE_NAME = "globalTrajectory.csv";

  @Override
  public GlobalProcessor createGlobalProcessor(BPS bps)
  {
    BufferedWriter writer = results.getAutoClosedBufferedWriter(GLOBAL_TRAJ_FILE_NAME);
    BriefIO.println(writer, "delta," + Joiner.on(",").join(indices().stream().map(i -> BPSFactory.VARIABLE_KEY + "_" + i).iterator()));
    String [] stringArray = new String[indices().size()];
    return new GlobalProcessor()
    {
      @Override
      public void process(GlobalProcessorContext context)
      {
        int i = 0;
        for (ContinuouslyEvolving var : context.allVariables())
          stringArray[i++] = String.valueOf(var.position.get());
        BriefIO.println(writer, String.valueOf(context.getGlobalDelta()) + "," + Joiner.on(",").join(stringArray));
      }
    };
  }
  
  public static void main(String [] args) 
  {
    Experiment.startAutoExit(args);
  }
}
