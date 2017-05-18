package ca.ubc.bps.processors.loaders;

import java.io.File;

import ca.ubc.bps.factory.BPSFactory.BPS;
import ca.ubc.bps.processors.MemorizeTrajectory;
import ca.ubc.bps.processors.Trajectory;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.pdmp.Processor;

public class MemorizeTrajectoryLoader extends TrajectoryLoader
{
  private MemorizeTrajectory processor = null;
  
  public MemorizeTrajectoryLoader(BPS bps, File samples, int index)
  {
    super(bps, samples, index);
  }

  public Trajectory getTrajectory()
  {
    run();
    return processor.getTrajectory();
  }

  @Override
  public Processor createProcessor(PositionVelocity variable, BPS bps)
  {
    return processor = new MemorizeTrajectory(variable);
  }
}
