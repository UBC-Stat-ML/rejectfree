package rejfree;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;
import org.jblas.DoubleMatrix;

import com.google.common.primitives.Doubles;

import bayonet.distributions.Exponential;
import blang.annotations.DefineFactor;
import blang.variables.RealVariable;
import rejfree.global.GlobalRFSampler;
import rejfree.local.LocalRFRunner;
import rejfree.local.LocalRFRunnerOptions;
import rejfree.models.normal.NormalFactor;

public class TestUnbiasedSampler
{

  private static class Spec
  {
    RealVariable variable0 = new RealVariable(1), variable1 = new RealVariable(1);
    
    
    @DefineFactor
    public NormalFactor factor;

  }
  
  
  public static void main(String [] args) 
  {
    LocalRFRunnerOptions options = new LocalRFRunnerOptions();
    options.rfOptions.refreshRate = 1.0;
    options.maxSteps = Integer.MAX_VALUE;
    options.maxRunningTimeMilli = Long.MAX_VALUE;
    options.silent = true;
    
    // collect some samples from the new chain
    double targetLen = 10.0;
    
    List<Double> testedSamples = new ArrayList<>();
    int nSamples = 1_000;
    for (int i = 0; i < nSamples; i++)
    {
      
      Spec spec = new Spec();
      spec.variable0.setValue(options.samplingRandom.nextGaussian());
      spec.variable1.setValue(options.samplingRandom.nextGaussian());
      List<RealVariable> vars = new ArrayList<>();
      vars.add(spec.variable0);
      vars.add(spec.variable1);
      
//      double sumSqrs = 0.0;
      double trajLen = 0.0;
      innerLoop:while (true)
      {
        boolean done = false;
        options.maxTrajectoryLength = Exponential.generate(options.samplingRandom, 1.0);
        
        if (trajLen + options.maxTrajectoryLength > targetLen)
        {
          done = true;
          options.maxTrajectoryLength = targetLen - trajLen;
        }
        
        // sample the unbiased energy
        double var = 1.0 + (options.samplingRandom.nextBoolean() ? -0.5 : +0.5);
        DoubleMatrix covarMtx = new DoubleMatrix(new double[][]{{var, 0.0}, {0.0, var}});
        spec.factor = NormalFactor.withCovariance(covarMtx, vars);
        LocalRFRunner runner = new LocalRFRunner(options);
        runner.init(spec);
        runner.addMomentRayProcessor();
        runner.run();
        
//        sumSqrs += runner.momentRayProcessor.sumSq.getCount(spec.variable0);
        trajLen += runner.momentRayProcessor.currentTime;
        
        if (done)
        {
          testedSamples.add(spec.variable0.getValue() * spec.variable0.getValue() * spec.variable0.getValue() * spec.variable0.getValue());
          break innerLoop;
        }
      }
    } 
    
    List<Double> trueGaussians = new ArrayList<>();
    for (int i = 0; i < nSamples; i++)
    {
      double value = options.samplingRandom.nextGaussian();
      trueGaussians.add(value * value * value * value);
    }
    
    System.out.println("pValue (T test) = " + TestUtils.tTest(Doubles.toArray(testedSamples), Doubles.toArray(trueGaussians)));
    System.out.println("pValue (MW) = " + new MannWhitneyUTest().mannWhitneyUTest(Doubles.toArray(testedSamples), Doubles.toArray(trueGaussians)));
    System.out.println("pValue (WSR) = " + new WilcoxonSignedRankTest().wilcoxonSignedRankTest(Doubles.toArray(testedSamples), Doubles.toArray(trueGaussians), false));
    
//    System.out.println("Estimate: " + (sumSqrs / trajLen));
  }
}
