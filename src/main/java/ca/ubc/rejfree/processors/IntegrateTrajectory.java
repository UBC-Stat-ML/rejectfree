package ca.ubc.rejfree.processors;

import java.util.Collections;

import bayonet.math.SpecialFunctions;
import ca.ubc.pdmp.Processor;
import ca.ubc.rejfree.Trajectory;
import ca.ubc.rejfree.TrajectorySegment;
import ca.ubc.rejfree.state.ContinuousStateDependent;
import ca.ubc.rejfree.state.ContinuouslyEvolving;
import ca.ubc.rejfree.state.Dynamics;
import ca.ubc.rejfree.state.PiecewiseLinear;

public class IntegrateTrajectory extends ContinuousStateDependent implements Processor
{
  final ContinuouslyEvolving variable;
  final Integrator integrator;
  
  public static double evaluateIntegral(Trajectory trajectory, SegmentIntegral integral)
  {
    integral.setup(trajectory.dynamics);
    Integrator integrator = new Integrator(integral);
    for (TrajectorySegment segment : trajectory.segments)
      integrator.process(segment.deltaTime, segment.startPosition, segment.startVelocity);
    return integrator.evaluateIntegral();
  }
  
  public double evaluateIntegral()
  {
    return integrator.evaluateIntegral();
  }
  
  public IntegrateTrajectory(ContinuouslyEvolving variable, SegmentIntegral integral)
  {
    super(Collections.singletonList(variable));
    this.variable = variable;
    this.integrator = new Integrator(integral);
  }

  @Override
  public void process(double deltaTime)
  {
    integrator.process(deltaTime, variable.position.get(), variable.velocity.get());
  }
  
  private static class Integrator
  {
    final SegmentIntegral integral;
    double totalLength = 0.0, sum = 0.0;
    Integrator(SegmentIntegral integral)
    {
      this.integral = integral;
    }
    void process(double deltaTime, double x, double v)
    {
      totalLength += deltaTime;
      sum += integral.evaluate(x, v, deltaTime);
    }
    double evaluateIntegral()
    {
      if (totalLength == 0.0)
        throw new RuntimeException();
      return sum / totalLength;
    }
  }
  
  public static interface SegmentIntegral
  {
    void setup(Dynamics dynamics);
    double evaluate(double x, double v, double deltaT);
  }
  
  public static class MonomialIntegral implements SegmentIntegral
  {
    final int degree;
    
    public MonomialIntegral(int degree)
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
