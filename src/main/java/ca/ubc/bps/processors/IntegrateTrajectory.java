package ca.ubc.bps.processors;

import java.util.Collections;

import bayonet.math.SpecialFunctions;
import ca.ubc.bps.Trajectory;
import ca.ubc.bps.TrajectorySegment;
import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.state.Dynamics;
import ca.ubc.bps.state.PiecewiseLinear;
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
    this.integrator = new TrajectoryIntegrator(integral);
  }

  @Override
  public void process(double deltaTime)
  {
    integrator.process(deltaTime, variable.position.get(), variable.velocity.get());
  }
  
  private static class TrajectoryIntegrator
  {
    final SegmentIntegrator integral;
    double totalLength = 0.0, sum = 0.0;
    TrajectoryIntegrator(SegmentIntegrator integral)
    {
      this.integral = integral;
    }
    void process(double deltaTime, double x, double v)
    {
      totalLength += deltaTime;
      sum += integral.evaluate(x, v, deltaTime);
    }
    double integrate()
    {
      if (totalLength == 0.0)
        throw new RuntimeException();
      return sum / totalLength;
    }
  }
  
  public static interface SegmentIntegrator
  {
    void setup(Dynamics dynamics);
    double evaluate(double x, double v, double deltaT);
  }
  
  public static class MomentIntegrator implements SegmentIntegrator
  {
    final int degree;
    
    public MomentIntegrator(int degree)
    {
      if (degree < 0)
        throw new RuntimeException();
      this.degree = degree;
    }

    @Override
    public void setup(Dynamics dynamics)
    {
      if (!(dynamics instanceof PiecewiseLinear))
        throw new RuntimeException("Other dynamics not yet implemented");
    }

    @Override
    public double evaluate(double x, double v, double deltaT)
    {
      double sum = 0.0;
      for (int k = 0; k <= degree; k++) 
        sum += 
          Math.exp(SpecialFunctions.logBinomial(degree, k)) 
          * Math.pow(x, k) 
          * Math.pow(v, degree - k) 
          * Math.pow(deltaT, degree - k + 1) 
          / (degree - k + 1);
      return sum;
    }
  }
}
