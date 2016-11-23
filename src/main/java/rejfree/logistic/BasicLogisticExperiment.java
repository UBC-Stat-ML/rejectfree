package rejfree.logistic;

import briefj.OutputManager;
import briefj.opt.OptionSet;
import briefj.run.Mains;
import briefj.run.Results;
import rejfree.RFSamplerOptions.RefreshmentMethod;
import rejfree.local.LocalRFRunner;

public class BasicLogisticExperiment implements Runnable
{
  @OptionSet(name = "modelOpts")
  public LogisticModel lm = new LogisticModel();
  
  public static void main(String [] args)
  {
    Mains.instrumentedRun(args, new BasicLogisticExperiment());
    
    // - increase the number of data point, check scaling
    // - test different distribs for covariates
    // - check also CI's shrink
    // - then, investigate collision rate, etc  
    // - might need some more ESS computation validataion as well
    // - try 
  }

  @Override
  public void run()
  {
    LocalRFRunner runner = new LocalRFRunner();
    runner.options.maxSteps = Integer.MAX_VALUE;
    runner.options.maxTrajectoryLength = 100;
    runner.options.rfOptions.collectRate = 0.0;
    runner.options.rfOptions.refreshmentMethod = RefreshmentMethod.GLOBAL;
    runner.init(lm.getModelSpec());
    
    // burn-in
    if (runner.saveRaysProcessor != null)
      throw new RuntimeException(); // make sure we are not collecting during burn-in
    runner.run();
    
    // do the actual sampling
    runner = new LocalRFRunner(runner.options);
    runner.init(lm.getModelSpec());
    runner.addSaveAllRaysProcessor();
    runner.run();
    
    OutputManager manager = new OutputManager();
    manager.setOutputFolder(Results.getResultFolder());
    
    // compute ESS
    for (int dim = 0; dim < lm.nDimensions; dim++)
      for (int deg = 1; deg < 3; deg++) 
      {
        double ess = runner.saveRaysProcessor.getTrajectory(lm.variables.get(dim)).momentEss(deg);
        manager.printWrite("ess", 
          "nDim", lm.nDimensions,
          "dim", dim,
          "deg", deg,
          "ess", ess
        );
      }
    
    manager.close();
  }
}
