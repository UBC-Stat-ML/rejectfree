package ca.ubc.bps.processors;

import java.io.Writer;
import java.util.Collections;

import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.pdmp.Processor;

public class IntegrateTrajectory extends ContinuousStateDependent implements Processor
{
  final ContinuouslyEvolving variable;
  final TrajectoryIntegrator integrator;
  
  public static double integrate(Trajectory trajectory, SegmentIntegrator integral)
  {
    integral.setup(trajectory.dynamics);
    TrajectoryIntegrator integrator = new TrajectoryIntegrator(integral);
    for (TrajectorySegment segment : trajectory.segments)
      integrator.process(segment.deltaTime, segment.startPosition, segment.startVelocity);
    return integrator.integrate();
  }
  
  public double integrate()
  {
    return integrator.integrate();
  }
  
  public IntegrateTrajectory(ContinuouslyEvolving variable, SegmentIntegrator integral)
  {
    super(Collections.singletonList(variable));
    this.variable = variable;
    integral.setup(variable.dynamics);
    this.integrator = new TrajectoryIntegrator(integral);
  }

  @Override
  public void process(double deltaTime, int jumpProcessIndex)
  {
    integrator.process(deltaTime, variable.position.get(), variable.velocity.get());
    if (out != null)
      write();
  }
  
  private void write()
  {
    if (!exponentiallySpaced || counter == next)
    {
      try { 
        out.append("" + counter + "," + integrate() + "\n"); 
        if (exponentiallySpaced && counter > 1000)
          out.flush();
      } catch (Exception e) { throw new RuntimeException(e); }
      next *= 2;
    }
    counter++;
  }

  private Writer out = null;
  private boolean exponentiallySpaced = false;
  private int counter = 0, next = 1;
  public void setOutput(Writer out, boolean exponentiallySpaced)
  {
    this.out = out;
    this.exponentiallySpaced = exponentiallySpaced;
    try { out.append("eventIndex,currentAverage\n"); }
    catch (Exception e) { throw new RuntimeException(e); }
  }
}
