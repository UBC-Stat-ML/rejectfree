package ca.ubc.bps.processors;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;

import ca.ubc.bps.state.Dynamics;
import ca.ubc.bps.state.MutableDouble;
import ca.ubc.bps.state.SimpleMutableDouble;

public class NumericalIntegrator implements SegmentIntegrator
{
  public int maxIntegrationSteps = Integer.MAX_VALUE;
  private final UnivariateFunction testFunction;
  private Dynamics dynamics = null;
  private final MutableDouble 
    dummyPosition = new SimpleMutableDouble(),
    dummyVelocity = new SimpleMutableDouble();
  
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