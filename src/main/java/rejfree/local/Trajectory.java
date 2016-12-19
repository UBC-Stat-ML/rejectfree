package rejfree.local;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import bayonet.math.NumericalUtils;
import bayonet.math.SpecialFunctions;

public class Trajectory
{
  final List<TrajectoryRay> rays;
  final double totalTime;
  
  public Trajectory(List<TrajectoryRay> rays, double totalTime)
  {
    this.rays = rays;
    this.totalTime = totalTime;
  }
  
  @FunctionalInterface
  public static interface SegmentIntegral
  {
    double evaluate(double x, double v, double deltaT);
  }
  
  public static SegmentIntegral monomialIntegral(final int degree)
  {
    return (double x, double v, double deltaT) -> 
    {
      double sum = 0.0;
      for (int k = 0; k <= degree; k++) 
        sum += 
          Math.exp(SpecialFunctions.logBinomial(degree, k)) 
          * Math.pow(x, k) 
          * Math.pow(v, degree - k) 
          * Math.pow(deltaT, degree - k + 1) 
          / (degree - k + 1);
      return sum;
    };
  }
  
  public double momentEss(int degree)
  {
    int nBlocks = (int) Math.sqrt(rays.size());
    List<Trajectory> split = split(nBlocks); 
    
    SummaryStatistics blockStats = new SummaryStatistics();
    for (Trajectory subTraj : split)
      blockStats.addValue(subTraj.moment(degree));
    
    return nBlocks * (moment(degree * 2) - Math.pow(blockStats.getMean(), 2)) / blockStats.getVariance();
  }
  
  public double moment(int degree)
  {
    return integrate(monomialIntegral(degree));
  }
  
  public double integrate(SegmentIntegral integral)
  {
    double sum = 0.0;
    
    for (int i = 0; i < rays.size(); i++)
      sum += integral.evaluate(rays.get(i).position_t, rays.get(i).velocity_t, deltaTimeOfRay(i));
    
    return sum / totalTime;
  }
  
  public List<Trajectory> split(int nBlocks)
  {
    List<Trajectory> result = new ArrayList<>();
    
    final double delta = totalTime / nBlocks;
    
    double currentTime = beginTime();
    double timeNextBlockBegins = currentTime + delta;
    
    Trajectory current = new Trajectory(new ArrayList<>(), delta);
    
    int currentRayIndex = 0; 
    
    while (result.size() < nBlocks)
    {
      current.rays.add(
          NumericalUtils.isClose(rays.get(currentRayIndex).t, currentTime, NumericalUtils.THRESHOLD) 
            ? rays.get(currentRayIndex) 
            : new TrajectoryRay(currentTime, rays.get(currentRayIndex).position(currentTime), rays.get(currentRayIndex).velocity_t));
    
      final double timeCurrentRayEnds = endTimeOfRay(currentRayIndex);
            
      if (timeCurrentRayEnds > timeNextBlockBegins || NumericalUtils.isClose(timeCurrentRayEnds, timeNextBlockBegins, NumericalUtils.THRESHOLD))
      {
        result.add(current);
        current = new Trajectory(new ArrayList<>(), delta);
        currentTime = timeNextBlockBegins;
        timeNextBlockBegins = currentTime + delta;
      }
      else
      {
        currentRayIndex++;
        currentTime = rays.get(currentRayIndex).t;
      }
    }
    
    return result;
  }
  
  public double beginTime()
  {
    return rays.isEmpty() ? 0.0 : rays.get(0).t;
  }
  
  public double endTimeOfRay(int index) 
  {
    if (index == rays.size() - 1)
      return beginTime() + totalTime;
    else
      return rays.get(index + 1).t;
  }
  
  public double deltaTimeOfRay(int index)
  {
    return endTimeOfRay(index) - rays.get(index).t;
  }
}
