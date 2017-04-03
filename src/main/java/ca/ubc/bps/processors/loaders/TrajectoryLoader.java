package ca.ubc.bps.processors.loaders;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import blang.inits.experiments.Experiment;
import briefj.BriefIO;
import ca.ubc.bps.BPSFactory;
import ca.ubc.bps.BPSFactory.BPS;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.pdmp.Processor;

public abstract class TrajectoryLoader extends Experiment
{
  private final BPS bps;
  private final File samples;
  private final int index;
  
  public TrajectoryLoader(BPS bps, File samples, int index)
  {
    this.bps = bps;
    this.samples = samples;
    this.index = index;
  }

  public TrajectoryLoader(File bpsExecFolder, int index)
  {
    BPSFactory bpsFactory = BPSFactory.loadBPSFactory(bpsExecFolder, results);
    this.bps = bpsFactory.buildBPS();
    this.samples = getSampleFile(bpsExecFolder, index);
    this.index = index;
  }
  
  public static File getSampleFile(File bpsExecFolder, int index)
  {
    return new File(bpsExecFolder, 
        BPSFactory.BPS.CONTINUOUSLY_EVOLVING_SAMPLES_DIR_NAME + "/" + 
        BPSFactory.VARIABLE_KEY + "=" + index + "/" + BPSFactory.BPS.DATA_FILE_NAME);
  }
  
  @Override
  public void run()
  {
    List<ContinuouslyEvolving> vars = new ArrayList<>(bps.continuouslyEvolvingStates());
    ContinuouslyEvolving theVar = vars.get(index);
    Processor p = createProcessor(theVar, bps);
    
    for (List<String> line : BriefIO.readLines(samples).splitCSV().skip(1))
    {
      final double 
        delta = Double.parseDouble(line.get(0)),
        x = Double.parseDouble(line.get(1)),
        v = Double.parseDouble(line.get(2));
      theVar.position.set(x);
      theVar.velocity.set(v); 
      final int jumpProcessIndex = Integer.parseInt(line.get(3));
      p.process(delta, jumpProcessIndex);
    }
  }

  public abstract Processor createProcessor(ContinuouslyEvolving variable, BPS bps);
}
