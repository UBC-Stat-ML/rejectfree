package ca.ubc.bps.processors;

import static ca.ubc.bps.processors.IntegrateTrajectory.integrate;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import ca.ubc.bps.Trajectory;
import ca.ubc.bps.processors.IntegrateTrajectory.MomentIntegrator;
import ca.ubc.bps.processors.IntegrateTrajectory.SegmentIntegrator;

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
