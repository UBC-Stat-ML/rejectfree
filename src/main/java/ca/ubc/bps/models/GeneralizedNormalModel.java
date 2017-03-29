package ca.ubc.bps.models;

import java.util.List;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.Model;
import ca.ubc.bps.BPSFactory.ModelBuildingContext;
import ca.ubc.bps.energies.GeneralizedNormalEnergy;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.state.PiecewiseLinear;
import ca.ubc.bps.timers.StandardIntensity;
import ca.ubc.bps.timers.UnimodalTimer;

public class GeneralizedNormalModel implements Model
{
  @Arg @DefaultValue("1.0")
  public double alpha = 1.0;
  
  @Arg @DefaultValue("2")
  public int size = 2;

  @Override
  public void setup(ModelBuildingContext context, boolean initializeStatesFromStationary)
  {
    if (alpha < 0.0)
      throw new RuntimeException("No solver currently supported for alpha < 0.0");
    if (initializeStatesFromStationary)
      throw new RuntimeException("Not yet supported");
    List<ContinuouslyEvolving> vars = context.buildAndRegisterContinuouslyEvolvingStates(size);
    GeneralizedNormalEnergy energy = new GeneralizedNormalEnergy(alpha);
    
    if (!(context.dynamics() instanceof PiecewiseLinear))
      throw new RuntimeException();
    StandardIntensity intensity = new StandardIntensity(energy);
    
    UnimodalTimer timer = new UnimodalTimer(vars, intensity);
    context.registerBPSPotential(new BPSPotential(energy, timer));
  }

}
