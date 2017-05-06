package ca.ubc.bps.processors;

import bayonet.math.SpecialFunctions;
import ca.ubc.bps.state.Dynamics;
import ca.ubc.bps.state.PiecewiseLinear;

public class MomentIntegrator implements SegmentIntegrator
{
  final int degree;
  
  private SegmentIntegrator numericalDelegate = null;
  
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
    {
      numericalDelegate = new NumericalIntegrator(d -> Math.pow(d, degree));
      numericalDelegate.setup(dynamics);
    }
  }

  @Override
  public double evaluate(double x, double v, double deltaT)
  {
    if (numericalDelegate != null)
      return numericalDelegate.evaluate(x, v, deltaT);
    
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