package ca.ubc.bps.processors;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.TreeMultimap;

import bayonet.math.NumericalUtils;
import ca.ubc.bps.state.PositionVelocity;

public class ConvertToGlobalProcessor
{
  final TreeMultimap<Double, LabeledSegment> sortedSegments = TreeMultimap.create();
  final List<PositionVelocity> allVariables = new ArrayList<>();
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
    PositionVelocity variable = new PositionVelocity(trajectory.dynamics, key);
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
  
  public double tolerance = 10e-12;
  public class GlobalProcessorContext
  {
    private double globalDelta;
    private double interpolatedDelta = 0.0;
    public double getGlobalDelta()
    {
      return globalDelta;
    }
    public List<PositionVelocity> allVariables()
    {
      return allVariables;
    }
    public void interpolate(double delta)
    {
      interpolatedDelta += delta;
      if (interpolatedDelta < -tolerance || interpolatedDelta > globalDelta + tolerance)
        throw new RuntimeException("Invalid interpolation: " + delta + "");
      for (PositionVelocity var : allVariables)
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
      for (PositionVelocity var : allVariables)
        var.extrapolateInPlace(context.globalDelta - context.interpolatedDelta);
      context.interpolatedDelta = 0.0;
      
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
    final PositionVelocity variable;
    
    public LabeledSegment(PositionVelocity variable, TrajectorySegment next)
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
