package ca.ubc.pdmp;

import org.jblas.DoubleMatrix;

import blang.variables.RealVariable;
import rejfree.RFSamplerOptions.RefreshmentMethod;
import rejfree.local.LocalRFRunner;
import rejfree.models.normal.NormalChain;
import rejfree.models.normal.NormalChainOptions;
import rejfree.models.normal.NormalChain.NormalChainModel;

public class NormalTestOld
{
  public static void main(String [] args)
  {
    NormalChainOptions options = new NormalChainOptions();
    options.nPairs = 1;
    options.offDiag = 0.0;
    options.diag = 1.0;
    
    LocalRFRunner runner = new LocalRFRunner();
    NormalChain chain = new NormalChain(options);
    DoubleMatrix exactSample = chain.exactSample();
    NormalChainModel modelSpec = chain.new NormalChainModel(exactSample.data);
    runner.init(modelSpec);
    runner.addMomentRayProcessor();
    runner.options.maxSteps = Integer.MAX_VALUE;
    runner.options.maxTrajectoryLength = 1_000_000;
    runner.options.rfOptions.refreshmentMethod = RefreshmentMethod.GLOBAL;
    runner.options.rfOptions.refreshRate = 1.0;
    runner.options.rfOptions.collectRate = 0.0;
    runner.run();
    
    for (int d = 0; d < chain.dim(); d++)
    {
      RealVariable variable = modelSpec.variables.get(d);
      System.out.println("" + d);
      System.out.println(runner.momentRayProcessor.getMeanEstimate(variable));
      System.out.println(runner.momentRayProcessor.getVarianceEstimate(variable));
      System.out.println(runner.momentRayProcessor.meanSegmentLength(variable));
    }
  }
}
