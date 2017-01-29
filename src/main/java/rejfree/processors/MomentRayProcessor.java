package rejfree.processors;

import rejfree.local.LocalRFSampler;
import rejfree.local.TrajectoryRay;
import blang.variables.RealVariable;
import briefj.collections.Counter;



public class MomentRayProcessor implements RayProcessor
{
  public Counter<RealVariable> 
    sum   = new Counter<RealVariable>(),
    sumSq = new Counter<RealVariable>(),
    nUpdates = new Counter<RealVariable>();
  public double currentTime = 0.0;

  public double getMeanEstimate(RealVariable variable) 
  { 
    return sum.getCount(variable) / currentTime; 
  }
  
  public double getVarianceEstimate(RealVariable variable)
  {
    final double muBar = getMeanEstimate(variable);
    return sumSq.getCount(variable) / currentTime - (muBar * muBar);
  }
  
  public double getSquaredVariableEstimate(RealVariable variable)
  {
    return sumSq.getCount(variable) / currentTime;
  }
  
  public double meanSegmentLength(RealVariable variable)
  {
    return currentTime / nUpdates.getCount(variable);
  }
  
  @Override
  public void init(LocalRFSampler sampler) {}
  
  @Override
  public void processRay(RealVariable var, TrajectoryRay ray, double time,
      LocalRFSampler sampler)
  {
    sum.incrementCount(var, 
        indefIntegralForMean(ray.position_t, ray.velocity_t, time - ray.t));
    
    sumSq.incrementCount(var, 
        indefIntegralForSecondMoment(ray.position_t, ray.velocity_t, time - ray.t));
    
    nUpdates.incrementCount(var, 1.0);
    
    currentTime = time;
  }
  
  public static double indefIntegralForMean(double x0, double v, double t)
  {
    return x0 * t + v * t*t / 2.0;
  }
  
  public static double indefIntegralForSecondMoment(double x0, double v, double t)
  {
    return x0*x0 * t + x0 * v * t*t + v*v * t*t*t / 3.0;
  }
}