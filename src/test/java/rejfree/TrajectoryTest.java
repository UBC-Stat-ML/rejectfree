package rejfree;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jblas.DoubleMatrix;
import org.junit.Assert;
import org.junit.Test;

import bayonet.coda.EffectiveSize;
import bayonet.math.NumericalUtils;
import blang.annotations.DefineFactor;
import blang.variables.RealVariable;
import rejfree.local.LocalRFRunner;
import rejfree.local.LocalRFRunnerOptions;
import rejfree.local.Trajectory;
import rejfree.models.normal.NormalFactor;
import rejfree.processors.SaveRaysProcessor;

public class TrajectoryTest
{
  
  final LocalRFRunner runner;
  final Spec spec;
  
  public TrajectoryTest()
  {
    LocalRFRunnerOptions options = new LocalRFRunnerOptions();
    options.rfOptions.refreshRate = 1;
    options.maxSteps = 50000; //Integer.MAX_VALUE;
    options.maxRunningTimeMilli = Long.MAX_VALUE;
    options.maxTrajectoryLength = Double.POSITIVE_INFINITY;
    
    spec = new Spec();
    spec.variable0.setValue(options.samplingRandom.nextGaussian());
    spec.variable1.setValue(options.samplingRandom.nextGaussian());
    
    List<RealVariable> vars = new ArrayList<>();
    vars.add(spec.variable0);
    vars.add(spec.variable1);
    
    DoubleMatrix covarMtx = new DoubleMatrix(new double[][]{{1.0, 0.0}, {0.0, 1.0}});
    spec.factor = NormalFactor.withCovariance(covarMtx, vars);
    
    runner = new LocalRFRunner(options);
    runner.init(spec);
    runner.addSaveAllRaysProcessor();
    runner.addMomentRayProcessor();
    runner.run();
  }
  
  @Test
  public void testExpectation()
  {
    Assert.assertEquals(
        1.0,                                                                  
        runner.saveRaysProcessor.getTrajectory(spec.variable0).moment(0), 
        NumericalUtils.THRESHOLD);
    
    // check moments 1, 2 coincide with old impl
    Assert.assertEquals(
        runner.momentRayProcessor.getMeanEstimate(spec.variable0),            
        runner.saveRaysProcessor.getTrajectory(spec.variable0).moment(1), 
        NumericalUtils.THRESHOLD);
    Assert.assertEquals(
        runner.momentRayProcessor.getSquaredVariableEstimate(spec.variable0), 
        runner.saveRaysProcessor.getTrajectory(spec.variable0).moment(2), 
        NumericalUtils.THRESHOLD);
    
    // check higher moments do not crash
    for (int d = 3; d < 10; d++)
      System.out.println(runner.saveRaysProcessor.getTrajectory(spec.variable0).moment(d));
  }
  
  @Test
  public void testSplitTraj()
  {
    Trajectory fullTraj = runner.saveRaysProcessor.getTrajectory(spec.variable0);
    List<Trajectory> splitTraj = fullTraj.split(10);
    
    for (int d = 0; d < 10; d++)
    {
      double direct = fullTraj.moment(d);
      
      SummaryStatistics stats = new SummaryStatistics();
      for (Trajectory subTraj : splitTraj)
        stats.addValue(subTraj.moment(d));
      
      Assert.assertEquals(direct, stats.getMean(), NumericalUtils.THRESHOLD);
    }
  }
  
  @Test
  public void testESS()
  {
    List<Double> convertToSample = runner.saveRaysProcessor.convertToSample(spec.variable0, 0.01);
    System.out.println("Old: " + EffectiveSize.effectiveSize(convertToSample));
    
    System.out.println("New: " + runner.saveRaysProcessor.getTrajectory(spec.variable0).momentEss(1));
    
    System.out.println("New, 2nd moment: " + runner.saveRaysProcessor.getTrajectory(spec.variable0).momentEss(2));
    System.out.println("New, 3rd moment: " + runner.saveRaysProcessor.getTrajectory(spec.variable0).momentEss(3));
  }
  
  private static class Spec
  {
    RealVariable variable0 = new RealVariable(1), variable1 = new RealVariable(1);
    
    @DefineFactor
    public NormalFactor factor;

  }
}
