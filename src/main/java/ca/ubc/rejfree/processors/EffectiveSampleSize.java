package ca.ubc.rejfree.processors;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import ca.ubc.rejfree.Trajectory;
import ca.ubc.rejfree.processors.IntegrateTrajectory.SegmentIntegrator;
import ca.ubc.rejfree.processors.IntegrateTrajectory.MomentIntegrator;

import static ca.ubc.rejfree.processors.IntegrateTrajectory.integrate;

public class EffectiveSampleSize
{
  public static double ess(
      Trajectory trajectory, 
      SegmentIntegrator testFunction, 
      SegmentIntegrator testFunctionSquared)
  {
    int nBlocks = 1 + (int) Math.sqrt(trajectory.numberOfSegments());
    List<Trajectory> split = trajectory.split(nBlocks);
    
    SummaryStatistics blockStats = new SummaryStatistics();
    for (Trajectory subTrajectory : split)
      blockStats.addValue(integrate(subTrajectory, testFunction));
    
    double squareIntegral = integrate(trajectory, testFunctionSquared);
    double variance = squareIntegral - Math.pow(blockStats.getMean(), 2);
    return nBlocks * variance / blockStats.getVariance();
  }
  
  public static double momentEss(Trajectory trajectory, int degree)
  {
    return ess(
        trajectory, 
        new MomentIntegrator(degree), 
        new MomentIntegrator(2 * degree));
  }
}
