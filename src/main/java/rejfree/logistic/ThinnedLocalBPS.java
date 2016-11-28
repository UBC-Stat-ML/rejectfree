package rejfree.logistic;

import java.util.Random;

import org.jblas.DoubleMatrix;

import bayonet.distributions.Exponential;
import briefj.opt.Option;

public class ThinnedLocalBPS
{
//  public class TLBPSOptions
//  {
//    @Option
//    public double refreshRate = 1.0;
//  }
//
//  private final LogisticModel model;
//  
//  private final TLBPSOptions options;
//  
//  public DoubleMatrix position, velocity;
//  public double time = 0.0;
//  
//  public final double [][] iotaSums;  // {0,1} -> dim
//  
//  private double rate()
//  {
//    double sum = 0.0;
//    
//    for (int dim = 0; dim < model.nDimensions; dim++)
//    {
//      final double v = velocity.get(dim);
//      sum += Math.abs(v) * iotaSums[v < 0 ? 1 : 0][dim];
//    }
//    
//    return sum;
//  }
//  
//  private void iterate(Random random, double trajectoryLen)
//  {
//    double endTime = time + trajectoryLen;
//    
//    while (time < endTime)
//    {
//    
//      // propose time
//      double proposedColDelta = Exponential.generate(random, rate());
//      
//      // propose refresh
//      double proposedRefDelta = Exponential.generate(random, options.refreshRate);
//      
//      
//      
//      // pick point 
//      
//      // accept/reject
//    
//    }
//  }
  
}
