package rejfree;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

import briefj.opt.Option;
import briefj.opt.OptionSet;
import briefj.run.Mains;
import rejfree.TestAnalyticNormal.NormalModel;
import rejfree.local.LocalRFRunner;
import rejfree.local.LocalRFRunnerOptions;
import rejfree.local.TrajectoryRay;
import rejfree.scalings.EstimateESSByDimensionality;
import rejfree.scalings.EstimateESSByDimensionality.ModelSpec;

public class NewEstimator implements Runnable
{
  public int dim = 1;
  
  @OptionSet(name = "runner")
  public LocalRFRunnerOptions options = new LocalRFRunnerOptions();
  
  public static void main(String [] args)
  {
    Mains.instrumentedRun(args, new NewEstimator());
  }
  
  public void run() 
  {
    if (dim != 1)
      throw new RuntimeException("assumes dim = 1");
    
    ModelSpec modelSpec = new EstimateESSByDimensionality.ModelSpec(dim, false);
    LocalRFRunner runner = new LocalRFRunner(options);
    runner.init(modelSpec);
    runner.addSaveAllRaysProcessor();
    runner.addMomentRayProcessor();
    runner.run();
    
    System.out.println("---");
    
    List<TrajectoryRay> collisionData = runner.saveRaysProcessor.samples.get(modelSpec.variables.get(0));
    
    System.out.println("E[X_1] approximated from...");
    System.out.println("\t full trajectory :" + runner.momentRayProcessor.getMeanEstimate(modelSpec.variables.get(0)));
    System.out.println("\t new estimator   :" + 
        newEstimator(collisionData, (Double x) -> x) / 
        newEstimator(collisionData, (Double x) -> 1.0));
    System.out.println("\t new estimatorAlex:" + 
        newEstimatorAlex(collisionData, (Double x) -> x) / 
        newEstimatorAlex(collisionData, (Double x) -> 1.0));
    System.out.println("\t new estimatorArno:" + 
        newEstimatorArnaud(collisionData, (Double x) -> x) / 
        newEstimatorArnaud(collisionData, (Double x) -> 1.0));
    System.out.println("---");
    
    System.out.println("E[X_1^2] approximated from...");
    System.out.println("\t full trajectory :" + runner.momentRayProcessor.getSquaredVariableEstimate(modelSpec.variables.get(0)));
    System.out.println("\t new estimator   :" + 
        newEstimator(collisionData, (Double x) -> x*x) / 
        newEstimator(collisionData, (Double x) -> 1.0));
    System.out.println("\t new estimatorAlex:" + 
        newEstimatorAlex(collisionData, (Double x) -> x*x) / 
        newEstimatorAlex(collisionData, (Double x) -> 1.0));
    System.out.println("\t new estimatorArno:" + 
        newEstimatorArnaud(collisionData, (Double x) -> x*x) / 
        newEstimatorArnaud(collisionData, (Double x) -> 1.0));
    System.out.println("---");
    
    
    System.out.println("E[X_1^4] approximated from...");
    System.out.println("\t new estimator   :" + 
        newEstimator(collisionData, (Double x) -> x*x*x*x) / 
        newEstimator(collisionData, (Double x) -> 1.0));
    System.out.println("\t new estimatorAlex:" + 
        newEstimatorAlex(collisionData, (Double x) -> x*x*x*x) / 
        newEstimatorAlex(collisionData, (Double x) -> 1.0));
    System.out.println("\t new estimatorArno:" + 
        newEstimatorArnaud(collisionData, (Double x) -> x*x*x*x) / 
        newEstimatorArnaud(collisionData, (Double x) -> 1.0));
    System.out.println("---");
    
  }
  
  private static double newEstimatorAlex(List<TrajectoryRay> list, Function<Double, Double> function)
  {
    double sum = 0.0;
    for (int colIdx = 0; colIdx < list.size() - 1; colIdx++) 
    {
      double curPt = list.get(colIdx).position_t;
      double nxtPt = list.get(colIdx + 1).position_t;
      sum += function.apply(curPt) / rate(nxtPt, list.get(colIdx).velocity_t);
    }
    return sum;
  }
  
  private static double newEstimatorArnaud(List<TrajectoryRay> list, Function<Double, Double> function)
  {
    double sum = 0.0;
    for (int colIdx = 0; colIdx < list.size() - 1; colIdx++) 
    {
      double curPt = list.get(colIdx).position_t;
      sum += function.apply(curPt) / rate(curPt, -list.get(colIdx).velocity_t);
    }
    return sum;
  }

  private static double newEstimator(List<TrajectoryRay> list, Function<Double, Double> function)
  {
    double sum = 0.0;
    for (int colIdx = 0; colIdx < list.size() - 1; colIdx++) 
    {
      double curPt = list.get(colIdx).position_t;
      sum += function.apply(curPt) / rate(curPt, list.get(colIdx).velocity_t);
    }
    return sum;
  }
  
  private static double rate(double x, double v) 
  {
    return 1.0 + Math.max(0, x * v);
  }

}
