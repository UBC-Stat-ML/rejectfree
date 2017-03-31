package ca.ubc.bps.processors;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.TreeMultimap;

import bayonet.math.NumericalUtils;
import ca.ubc.bps.Trajectory;
import ca.ubc.bps.TrajectorySegment;
import ca.ubc.bps.state.ContinuouslyEvolving;

public class ConvertToGlobalProcessor
{
  final TreeMultimap<Double, LabeledSegment> sortedSegments = TreeMultimap.create();
  final SortedSet<ContinuouslyEvolving> allVariables = new TreeSet<>();
  final GlobalProcessor processor;
  
  public ConvertToGlobalProcessor(GlobalProcessor processor)
  {
    this.processor = processor;
  }

  public GlobalProcessor getProcessor()
  {
    return processor;
  }

  public void addTrajectory(Object key, Trajectory trajectory)
  {
    ContinuouslyEvolving variable = new ContinuouslyEvolving(trajectory.dynamics, key);
    allVariables.add(variable);
    double globalTime = 0.0;
    for (int i = 0; i < trajectory.segments.size(); i++)
    {
      TrajectorySegment 
        current = trajectory.segments.get(i),
        next    = i + 1 < trajectory.segments.size() ? 
                    trajectory.segments.get(i + 1)   :
                    null;
      if (i == 0)
      {
        variable.position.set(current.startPosition);
        variable.velocity.set(current.startVelocity);
      }
      globalTime += current.deltaTime;
      sortedSegments.put(globalTime, new LabeledSegment(variable, next));
    }
  }
  
  public interface GlobalProcessor
  {
    public void process(GlobalProcessorContext context);
  }
  
  public double tolerance = 10e-12;
  public class GlobalProcessorContext
  {
    private double globalDelta;
    private double interpolatedDelta;
    public double getGlobalDelta()
    {
      return globalDelta;
    }
    Collection<ContinuouslyEvolving> allVariables()
    {
      return allVariables;
    }
    public void interpolate(double delta)
    {
      interpolatedDelta += delta;
      if (interpolatedDelta < -tolerance || interpolatedDelta > globalDelta + tolerance)
        throw new RuntimeException("Invalid interpolation");
      for (ContinuouslyEvolving var : allVariables)
        var.extrapolateInPlace(delta);
    }
    private GlobalProcessorContext() {}
  }
  
  public void convert()
  {
    TreeMultimap<Double, LabeledSegment> sortedSegments = mergeCloseTimes(this.sortedSegments);
    GlobalProcessorContext context = new GlobalProcessorContext();
    
    double lastEventTime = 0.0;
    for (double globalTime : sortedSegments.keySet())
    {
      // find out the time
      context.globalDelta = globalTime - lastEventTime;
      
      // process here - provide delta and call back
      processor.process(context);
      
      // move all the variables 
      for (ContinuouslyEvolving var : allVariables)
        var.extrapolateInPlace(context.globalDelta - context.interpolatedDelta);
      
      // update the variables involved
      for (LabeledSegment alteredSegment : sortedSegments.get(globalTime))
        if (alteredSegment.next != null)
        {
          alteredSegment.variable.position.set(alteredSegment.next.startPosition);
          alteredSegment.variable.velocity.set(alteredSegment.next.startVelocity);
        }
      
      lastEventTime = globalTime;
    }
  }
  
  private static TreeMultimap<Double, LabeledSegment> mergeCloseTimes(TreeMultimap<Double, LabeledSegment> input)
  {
    TreeMultimap<Double, LabeledSegment> sortedSegments = TreeMultimap.create();
    
    double last = -1.0;
    
    for (double current : input.keySet())
    {
      double mergedTime = NumericalUtils.isClose(last, current, 1e-12) ?
                             last :
                               current;
      sortedSegments.putAll(mergedTime, input.get(current));
      last = current;
    }
    
    return sortedSegments;
  }

  private static class LabeledSegment implements Comparable<LabeledSegment>
  {
    final TrajectorySegment next; // could be null
    final ContinuouslyEvolving variable;
    
    public LabeledSegment(ContinuouslyEvolving variable, TrajectorySegment next)
    {
      this.next = next;
      this.variable = variable;
    }

    @Override
    public int compareTo(LabeledSegment o)
    {
      return variable.key.toString().compareTo(o.variable.key.toString());
    }
  }
}
