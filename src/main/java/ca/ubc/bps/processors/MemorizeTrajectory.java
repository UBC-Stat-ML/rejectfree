package ca.ubc.bps.processors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.pdmp.Processor;

public class MemorizeTrajectory extends ContinuousStateDependent implements Processor
{
  final List<TrajectorySegment> trajectory = new ArrayList<>();
  final ContinuouslyEvolving variable;
  
  public MemorizeTrajectory(ContinuouslyEvolving variable)
  {
    super(Collections.singletonList(variable));
    this.variable = variable;
  } 
  
  @Override
  public void process(double deltaTime, int jumpProcessIndex)
  {
    final TrajectorySegment current = new TrajectorySegment(deltaTime, variable.position.get(), variable.velocity.get());
    trajectory.add(current);
  }

  public Trajectory getTrajectory()
  {
    return new Trajectory(variable.dynamics, trajectory);
  }
}
