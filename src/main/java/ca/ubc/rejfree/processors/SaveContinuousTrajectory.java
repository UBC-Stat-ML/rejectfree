package ca.ubc.rejfree.processors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.ubc.pdmp.Processor;
import ca.ubc.rejfree.ContinuousStateDependent;
import ca.ubc.rejfree.ContinuouslyEvolving;
import ca.ubc.rejfree.TrajectorySegment;

public class SaveContinuousTrajectory extends ContinuousStateDependent implements Processor
{
  final List<TrajectorySegment> trajectory = new ArrayList<>();
  double time = 0.0;
  final ContinuouslyEvolving variable;
  
  public SaveContinuousTrajectory(ContinuouslyEvolving variable)
  {
    super(Collections.singletonList(variable));
    this.variable = variable;
  } 
  
  @Override
  public void process(double deltaTime)
  {
    final TrajectorySegment current = new TrajectorySegment(deltaTime, variable.position.get(), variable.velocity.get());
    trajectory.add(current);
    time += deltaTime;
  }

  public double getTime()
  {
    return time;
  }

  public List<TrajectorySegment> getTrajectory()
  {
    return trajectory;
  }
}
