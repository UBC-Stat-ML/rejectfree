package rejfree;

import java.util.ArrayList;
import java.util.List;

import org.jblas.DoubleMatrix;

import bayonet.coda.EffectiveSize;
import blang.annotations.DefineFactor;
import blang.variables.RealVariable;
import briefj.opt.Option;
import briefj.run.Mains;
import rejfree.local.LocalRFRunner;
import rejfree.local.LocalRFRunnerOptions;
import rejfree.models.normal.NormalFactor;

public class VelocityNormDistribution implements Runnable
{
  @Option
  public double varScaling = 100;
  
  public static void main(String [] args)
  {
    Mains.instrumentedRun(args, new VelocityNormDistribution());
  }

  @Override
  public void run()
  {
    LocalRFRunnerOptions options = new LocalRFRunnerOptions();
    options.rfOptions.refreshRate = 1;
    options.maxSteps = 1000; //Integer.MAX_VALUE;
    options.maxRunningTimeMilli = Long.MAX_VALUE;
    options.maxTrajectoryLength = Double.POSITIVE_INFINITY;
    
    Spec spec = new Spec();
    spec.variable0.setValue(options.samplingRandom.nextGaussian());
    spec.variable1.setValue(options.samplingRandom.nextGaussian());
    
    List<RealVariable> vars = new ArrayList<>();
    vars.add(spec.variable0);
    vars.add(spec.variable1);
    
    DoubleMatrix covarMtx = new DoubleMatrix(new double[][]{{varScaling, 0.0}, {0.0, varScaling}});
    spec.factor = NormalFactor.withCovariance(covarMtx, vars);
    
    LocalRFRunner runner = new LocalRFRunner(options);
    runner.init(spec);
    runner.addSaveAllRaysProcessor();
    runner.run();
    double trajLen = runner.saveRaysProcessor.time;
    System.out.println(trajLen);
    List<Double> convertToSample = runner.saveRaysProcessor.convertToSample(spec.variable0, trajLen / 10000);
    double ess = EffectiveSize.effectiveSize(convertToSample);
    System.out.println(ess);
  }
  
  private static class Spec
  {
    RealVariable variable0 = new RealVariable(1), variable1 = new RealVariable(1);
    
    @DefineFactor
    public NormalFactor factor;

  }
}
