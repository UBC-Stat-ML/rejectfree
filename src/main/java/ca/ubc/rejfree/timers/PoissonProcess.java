package ca.ubc.rejfree.timers;

import ca.ubc.pdmp.EventTimer;

public interface PoissonProcess extends EventTimer
{
  double rate(double deltaTime);
}
