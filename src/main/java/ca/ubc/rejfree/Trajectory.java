package ca.ubc.rejfree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import bayonet.math.NumericalUtils;
import ca.ubc.rejfree.processors.EffectiveSampleSize;
import ca.ubc.rejfree.processors.IntegrateTrajectory;
import ca.ubc.rejfree.processors.IntegrateTrajectory.SegmentIntegrator;
import ca.ubc.rejfree.state.Dynamics;

public class Trajectory
{
  public final Dynamics dynamics;
  public final List<TrajectorySegment> segments;
  public final double totalTime;
  
  public Trajectory(Dynamics dynamics, List<TrajectorySegment> segments)
  {
    this.dynamics = dynamics;
    this.segments = segments;
    this.totalTime = segments.stream()
         .mapToDouble(s -> s.deltaTime)
         .sum();
  }
  
  public List<Trajectory> split(int nBlocks)
  {
    final double delta = totalTime / nBlocks;
    List<Double> blockSizes = Collections.nCopies(nBlocks, delta);
    return split(blockSizes);
  }
  
  public List<Trajectory> splitInTwo(double fractionForFirstHalf)
  {
    if (fractionForFirstHalf < 0.0 || fractionForFirstHalf > 1.0)
      throw new RuntimeException();
    
    List<Double> blockSizes = Arrays.asList(fractionForFirstHalf * totalTime, (1.0 - fractionForFirstHalf) * totalTime);
    return split(blockSizes);
  }
  
  public List<Trajectory> split(List<Double> blockSizes)
  {
    final double sumBlockSizes = blockSizes.stream().reduce(0.0, Double::sum);
    
    if (!NumericalUtils.isClose(totalTime, sumBlockSizes, NumericalUtils.THRESHOLD))
      throw new RuntimeException();
    
    final int nBlocks = blockSizes.size();
    List<Trajectory> result = new ArrayList<>(nBlocks);
    
    if (segments.isEmpty())
      return result;
    
    double remainingLenCurrentBlock = blockSizes.get(0);
    double remainingLenCurrentSegment = segments.get(0).deltaTime;
    
    Trajectory currentTraj = new Trajectory(this.dynamics, new ArrayList<>());
    
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
      if (NumericalUtils.isClose(0.0, remainingLenCurrentBlock, NumericalUtils.THRESHOLD))
      {
        result.add(currentTraj);
        if (result.size() == nBlocks)
          break loop;
        currentTraj = new Trajectory(this.dynamics, new ArrayList<>());
        remainingLenCurrentBlock = blockSizes.get(++currentBlockIndex);
      }
      if (NumericalUtils.isClose(0.0, remainingLenCurrentSegment, NumericalUtils.THRESHOLD))
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
    return integrate(new IntegrateTrajectory.MomentIntegrator(degree));
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
  
  // viz
  // discretizations
  // CIs
}
