package ca.ubc.bps;

import java.util.List;
import java.util.Random;

import ca.ubc.bps.refresh.IndependentRefreshment;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.state.PiecewiseLinear;
import ca.ubc.pdmp.PDMP;

public class PDMPs
{
  /**
   * Create an empty PDMP with d coordinates with positions 
   * initialized at zero and velocities initialized from an 
   * isotropic normal based on the provided random object.
   */
  public static PDMP withLinearDynamics(
      int d, 
      Random velocityRandom)
  {
    List<ContinuouslyEvolving> states = 
        ContinuouslyEvolving.buildArray(
            d, 
            new PiecewiseLinear());
    
    IndependentRefreshment.simulate(velocityRandom, states);
    
    return new PDMP(states);
  }
}
