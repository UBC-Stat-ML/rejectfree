package ca.ubc.bps.processors;

import static ca.ubc.bps.processors.IntegrateTrajectory.integrate;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.google.common.collect.Lists;

import ca.ubc.bps.Trajectory;
import ca.ubc.bps.processors.IntegrateTrajectory.MomentIntegrator;
import ca.ubc.bps.processors.IntegrateTrajectory.SegmentIntegrator;

public class EffectiveSampleSize
{
  // For trajectories:
  
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
    return ess(squareIntegral, blockStats);
  }
  
  public static double momentEss(Trajectory trajectory, int degree)
  {
    return ess(
        trajectory, 
        new MomentIntegrator(degree), 
        new MomentIntegrator(2 * degree));
  }
  
  // For samples:
  
  public static double ess(List<Double> samples)
  {
    int nBlocks = 1 + (int) Math.sqrt((double) samples.size());
    int partitionSize = 1 + samples.size() / nBlocks;
    List<List<Double>> split = Lists.partition(samples, partitionSize);
    
    SummaryStatistics blockStats = new SummaryStatistics();
    for (List<Double> block : split)
      blockStats.addValue(average(block.stream()));
    
    double squareIntegral = average(samples.stream(), x -> x*x);
    return ess(squareIntegral, blockStats);
  }
  
  private static double ess(double squareIntegral, SummaryStatistics blockStats) 
  {
    double variance = squareIntegral - Math.pow(blockStats.getMean(), 2);
    return blockStats.getN() * variance / blockStats.getVariance();
  }
  
  private static double average(Stream<Double> stream, Function<Double, Double> f)
  {
    return average(stream.map(f));
  }

  private static double average(Stream<Double> stream)
  {
    return stream.mapToDouble(a -> a).average().orElseGet(() -> Double.NaN);
  }
  

}
