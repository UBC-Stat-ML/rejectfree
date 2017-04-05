package ca.ubc.bps.processors;

import ca.ubc.bps.state.Dynamics;

public interface SegmentIntegrator
{
  void setup(Dynamics dynamics);
  double evaluate(double x, double v, double deltaT);
}