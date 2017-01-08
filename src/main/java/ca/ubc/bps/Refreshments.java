package ca.ubc.bps;

import java.util.Collection;

import static ca.ubc.bps.StaticUtils.*;
import static java.util.Collections.singleton;

import ca.ubc.bps.kernels.IndependentRefreshment;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.timers.HomogeneousPP;
import ca.ubc.pdmp.JumpProcess;
import ca.ubc.pdmp.PDMP;

public class Refreshments
{
  public static void addGlobal(PDMP pdmp, double rate)
  {
    add(pdmp, rate, continuousCoordinates(pdmp.coordinates));
  }
  
  public static void addLocal(PDMP pdmp, double rate)
  {
    for (ContinuouslyEvolving coordinate : continuousCoordinates(pdmp.coordinates))
      add(pdmp, rate, singleton(coordinate));
  }
  
  public static void add(
      PDMP pdmp, 
      double rate, 
      Collection<ContinuouslyEvolving> continuousCoordinates)
  {
    pdmp.jumpProcesses.add(
        new JumpProcess(
            new HomogeneousPP(rate), 
            new IndependentRefreshment(continuousCoordinates)));
  }
}
