package ca.ubc.rejfree.processors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.ubc.pdmp.Processor;
import ca.ubc.rejfree.Trajectory;
import ca.ubc.rejfree.TrajectorySegment;
import ca.ubc.rejfree.state.ContinuousStateDependent;
import ca.ubc.rejfree.state.ContinuouslyEvolving;

public class SaveTrajectory extends ContinuousStateDependent implements Processor
{
  final List<TrajectorySegment> trajectory = new ArrayList<>();
  final ContinuouslyEvolving variable;
  
  public SaveTrajectory(ContinuouslyEvolving variable)
  {
    super(Collections.singletonList(variable));
    this.variable = variable;
  } 
  
  @Override
  public void process(double deltaTime)
  {
    final TrajectorySegment current = new TrajectorySegment(deltaTime, variable.position.get(), variable.velocity.get());
    trajectory.add(current);
  }

  public Trajectory getTrajectory()
  {
    return new Trajectory(variable.dynamics, trajectory);
  }
}
