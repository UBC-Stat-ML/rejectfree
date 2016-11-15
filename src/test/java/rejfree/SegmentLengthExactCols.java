package rejfree;

import briefj.OutputManager;
import briefj.run.Mains;
import briefj.run.Results;
import rejfree.local.LocalRFRunner;
import rejfree.local.LocalRFRunnerOptions;
import rejfree.scalings.EstimateESSByDimensionality;
import rejfree.scalings.EstimateESSByDimensionality.ModelSpec;

public class SegmentLengthExactCols implements Runnable
{
  public void run() 
  {
    LocalRFRunnerOptions options = new LocalRFRunnerOptions();
    options.rfOptions.refreshRate = 0.0;
    options.maxSteps = 1;
    OutputManager manager = new OutputManager();
    manager.setOutputFolder(Results.getResultFolder());
    for (int rep = 0; rep < 100_000; rep++)
    {
      for (int d = 1000; d <= 10_000; d *= 2) 
      {
        ModelSpec modelSpec = new EstimateESSByDimensionality.ModelSpec(d, false);
        modelSpec.initFromStatio(options.samplingRandom);
        
        
        LocalRFRunner runner = new LocalRFRunner(options);
        runner.init(modelSpec);
        runner.addMomentRayProcessor();
        runner.run();
        
        manager.printWrite("results",
            "rep", rep,
            "d", d,
            "L", runner.momentRayProcessor.currentTime);
      }
    }
    manager.close();
  }
  
  public static void main(String [] args)
  {
    Mains.instrumentedRun(args, new SegmentLengthExactCols());
  }
}
