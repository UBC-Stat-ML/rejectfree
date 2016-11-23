package rejfree.logistic;

import java.util.Random;

import org.jblas.DoubleMatrix;

import bayonet.distributions.Exponential;

public class ThinnedLocalBPS
{

  private final LogisticModel model;
  
  public DoubleMatrix position, velocity;
  
  public final double [][] iotaSums;  // {0,1} -> dim
  
  private double rate()
  {
    double sum = 0.0;
    
    for (int dim = 0; dim < model.nDimensions; dim++)
    {
      final double v = velocity.get(dim);
      sum += Math.abs(v) * iotaSums[v < 0 ? 1 : 0][dim];
    }
    
    return sum;
  }
  
  private void iterate(Random random)
  {
    // propose time
    double proposedColTime = Exponential.generate(random, velocity);
    
    // propose refresh
    
    // pick point 
    
    // accept/reject
    
    
  }
  
}
