package ca.ubc.rejfree;

import java.util.List;
import java.util.Random;

import ca.ubc.pdmp.PDMP;
import ca.ubc.rejfree.kernels.IndependentRefreshment;
import ca.ubc.rejfree.state.ContinuouslyEvolving;
import ca.ubc.rejfree.state.PiecewiseLinear;

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
            PiecewiseLinear.instance);
    
    IndependentRefreshment.simulate(velocityRandom, states);
    
    return new PDMP(states);
  }
}
