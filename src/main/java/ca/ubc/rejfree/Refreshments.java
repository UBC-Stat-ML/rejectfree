package ca.ubc.rejfree;

import java.util.Collection;
import static java.util.Collections.singleton;

import ca.ubc.pdmp.JumpProcess;
import ca.ubc.pdmp.PDMP;
import ca.ubc.rejfree.kernels.IndependentRefreshment;
import ca.ubc.rejfree.state.ContinuouslyEvolving;
import ca.ubc.rejfree.timers.HomogeneousPP;

import static ca.ubc.rejfree.StaticUtils.*;

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
