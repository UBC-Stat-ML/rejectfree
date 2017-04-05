package ca.ubc.bps.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import bayonet.math.NumericalUtils;
import ca.ubc.bps.state.Dynamics;

public class Trajectory
{
  public final Dynamics dynamics;
  public final List<TrajectorySegment> segments;
  
  public Trajectory(Dynamics dynamics, List<TrajectorySegment> segments)
  {
    this.dynamics = dynamics;
    this.segments = segments;
  }
  
  public Trajectory(Dynamics dynamics)
  {
    this(dynamics, new ArrayList<>());
  }
  
  public double totalTime()
  {
    return segments.stream()
      .mapToDouble(s -> s.deltaTime)
      .sum();
  }
  
  public List<Trajectory> split(int nBlocks)
  {
    final double delta = totalTime() / nBlocks;
    List<Double> blockSizes = Collections.nCopies(nBlocks, delta);
    return split(blockSizes);
  }
  
  public List<Trajectory> splitInTwo(double fractionForFirstHalf)
  {
    if (fractionForFirstHalf < 0.0 || fractionForFirstHalf > 1.0)
      throw new RuntimeException();
    
    double totalTime = totalTime();
    List<Double> blockSizes = Arrays.asList(fractionForFirstHalf * totalTime, (1.0 - fractionForFirstHalf) * totalTime);
    return split(blockSizes);
  }
  
  public Trajectory burnOut(double fractionToBurnOut)
  {
    return splitInTwo(fractionToBurnOut).get(1);
  }
  
  public List<Trajectory> split(List<Double> blockSizes)
  {
    final double sumBlockSizes = blockSizes.stream().reduce(0.0, Double::sum);
    
    if (!NumericalUtils.isClose(totalTime()/sumBlockSizes, 1.0, 1e-6))
      throw new RuntimeException();
    
    final int nBlocks = blockSizes.size();
    List<Trajectory> result = new ArrayList<>(nBlocks);
    
    if (segments.isEmpty())
      return result;
    
    double remainingLenCurrentBlock = blockSizes.get(0);
    double remainingLenCurrentSegment = segments.get(0).deltaTime;
    
    Trajectory currentTraj = new Trajectory(this.dynamics);
    
    int currentSegIndex = 0;
    int currentBlockIndex = 0;
    
    loop:while (true)
    {
      TrajectorySegment currentSeg = segments.get(currentSegIndex);
      double consumedLength = Math.min(
          remainingLenCurrentBlock, 
          remainingLenCurrentSegment);
      TrajectorySegment current = new TrajectorySegment(
          consumedLength, 
          currentSeg.startPosition, 
          currentSeg.startVelocity);
      currentTraj.segments.add(current);
      remainingLenCurrentBlock -= consumedLength;
      remainingLenCurrentSegment -= consumedLength;
      if (NumericalUtils.isClose(0.0, remainingLenCurrentBlock, 1e-4) || currentSegIndex + 1 == segments.size())
      {
        result.add(currentTraj);
        if (result.size() == nBlocks)
          break loop;
        currentTraj = new Trajectory(this.dynamics, new ArrayList<>());
        remainingLenCurrentBlock = blockSizes.get(++currentBlockIndex);
      }
      if (NumericalUtils.isClose(0.0, remainingLenCurrentSegment, 1e-4))
        remainingLenCurrentSegment = segments.get(++currentSegIndex).deltaTime;
    }
    
    return result;
  }

  public double numberOfSegments()
  {
    return segments.size();
  }
  
  public double integrate(SegmentIntegrator integrator)
  {
    return IntegrateTrajectory.integrate(this, integrator);
  }
  
  public double moment(int degree)
  {
    return integrate(new MomentIntegrator(degree));
  }
  
  public double ess(SegmentIntegrator testFunction, 
      SegmentIntegrator testFunctionSquared)
  {
    return EffectiveSampleSize.ess(this, testFunction, testFunctionSquared);
  }
  
  public double momentEss(int degree)
  {
    return EffectiveSampleSize.momentEss(this, degree);
  }
  
  public SummaryStatistics segmentLengthSummaryStatistics()
  {
    SummaryStatistics result = new SummaryStatistics();
    for (TrajectorySegment segment : segments)
      result.addValue(segment.deltaTime);
    return result;
  }
  
  // viz
  // discretizations
  // CIs
}
