package rejfree;

import java.util.Random;

import org.jblas.DoubleMatrix;

import bayonet.opt.DifferentiableFunction;
import rejfree.RFSamplerOptions.RefreshmentMethod;
import rejfree.global.GlobalRFSampler;
import rejfree.models.normal.NormalEnergy;

public class SegmentLength
{
//  private static DoubleMatrix covar = new DoubleMatrix(new double[][]{{1.0,0},{0,1.0}});
//  private static DifferentiableFunction energy = NormalEnergy.withCovariance(covar);
  private static Random rand = new Random(1);
  private static RFSamplerOptions options = new RFSamplerOptions();
  
  public static void main(String [] args)
  {
    options.collectRate = 0.0;
    options.refreshmentMethod = RefreshmentMethod.GLOBAL;
    
    for (int d = 2; d <= 1024; d *= 2) 
    {
      System.out.println(d);
      DifferentiableFunction energy = NormalEnergy.isotropic(d);
      GlobalRFSampler sampler = new GlobalRFSampler(energy, new DoubleMatrix(d));
      sampler.iterate(rand, 100_000);
    }
    
  }
}
