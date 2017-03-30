package ca.ubc.bps.processors;

import java.io.Writer;
import java.util.Collections;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;

import bayonet.math.SpecialFunctions;
import ca.ubc.bps.Trajectory;
import ca.ubc.bps.TrajectorySegment;
import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.state.Dynamics;
import ca.ubc.bps.state.MutableDouble;
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
  
  public static class NumericalIntegrator implements SegmentIntegrator
  {
    public int maxIntegrationSteps = 100;
    private final UnivariateFunction testFunction;
    private Dynamics dynamics = null;
    private final MutableDouble 
      dummyPosition = new ContinuouslyEvolving.MutableDoubleImplementation(),
      dummyVelocity = new ContinuouslyEvolving.MutableDoubleImplementation();
    
    public NumericalIntegrator(UnivariateFunction testFunction)
    {
      this.testFunction = testFunction;
    }

    @Override
    public void setup(Dynamics dynamics)
    {
      this.dynamics = dynamics;
    }

    @Override
    public double evaluate(double x0, double v0, double deltaT)
    {
      final UnivariateFunction f = (double currentT) -> {
        dummyPosition.set(x0);
        dummyVelocity.set(v0);
        dynamics.extrapolateInPlace(currentT, dummyPosition, dummyVelocity);
        double currentX = dummyPosition.get();
        return testFunction.value(currentX);
      };
      return new RombergIntegrator().integrate(maxIntegrationSteps, f, 0, deltaT);
    }
    
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
