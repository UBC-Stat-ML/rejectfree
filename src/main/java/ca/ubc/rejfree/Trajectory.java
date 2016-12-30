package ca.ubc.rejfree;

import java.util.List;

import ca.ubc.rejfree.state.Dynamics;

public class Trajectory
{
  public final Dynamics dynamics;
  public final List<TrajectorySegment> segments;
  
  public Trajectory(Dynamics dynamics, List<TrajectorySegment> segments)
  {
    this.dynamics = dynamics;
    this.segments = segments;
  }
  
  // viz
  // moments
  // ESS
  // discretizations
  // CIs
}
