package ca.ubc.bps.models;

import java.util.List;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.energies.GeneralizedNormalEnergy;
import ca.ubc.bps.factory.ModelBuildingContext;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.state.PiecewiseLinear;
import ca.ubc.bps.timers.LogConcaveDensityTimer;
import ca.ubc.bps.timers.StandardIntensity;
import ca.ubc.bps.timers.UnimodalTimer;
import ca.ubc.pdmp.Clock;

public class GeneralizedNormalModel implements Model
{
  @Arg   @DefaultValue("1.0")
  public double alpha = 1.0;
  
  @Arg @DefaultValue("2")
  public   int size = 2;
  
  @Arg                    @DefaultValue("false")
  public boolean forceLogConcaveSolver = false;

  @Override
  public void setup(ModelBuildingContext context, boolean initializeStatesFromStationary)
  {
    if (alpha < -1.0)
      throw new RuntimeException("Not defined for alpha < -1.0");
    if (initializeStatesFromStationary)
      throw new RuntimeException("Not yet supported");
    List<ContinuouslyEvolving> vars = context.buildAndRegisterContinuouslyEvolvingStates(size);
    GeneralizedNormalEnergy energy = new GeneralizedNormalEnergy(alpha);
    
    if (!(context.dynamics() instanceof PiecewiseLinear))
      throw new RuntimeException();
    StandardIntensity intensity = new StandardIntensity(energy);
    
    Clock timer = 
        alpha < 0.0 || forceLogConcaveSolver ? 
          new LogConcaveDensityTimer(vars, energy) :
          new UnimodalTimer(vars, intensity);
    context.registerBPSPotential(new BPSPotential(energy, timer));
  }

}
