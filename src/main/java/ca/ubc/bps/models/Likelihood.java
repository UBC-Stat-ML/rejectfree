package ca.ubc.bps.models;

import java.util.List;

import blang.inits.Implementations;
import ca.ubc.bps.factory.ModelBuildingContext;
import ca.ubc.bps.state.PositionVelocity;

/**
 * Used to add a likelihood after a prior has setup a set of continuously evolving variables (parameters).
 * 
 * @author bouchard
 *
 */
@Implementations({Poisson.class, Likelihood.None.class})
public interface Likelihood
{
  public void setup(ModelBuildingContext context, List<PositionVelocity> vars);
  
  
  public static Likelihood none = new Likelihood.None();
  
  public static class None implements Likelihood
  {
    @Override
    public void setup(ModelBuildingContext context, List<PositionVelocity> vars)
    {
    }
  }
}
