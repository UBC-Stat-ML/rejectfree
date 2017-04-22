package ca.ubc.bps.models;

import java.util.Arrays;
import java.util.List;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.energies.GeneralizedNormalEnergy;
import ca.ubc.bps.factory.ModelBuildingContext;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.state.Hyperbolic;
import ca.ubc.bps.state.PiecewiseLinear;
import ca.ubc.bps.timers.NumericalUnimodalInversionTimer;
import ca.ubc.bps.timers.NumericalUnimodalInversionTimer.Optimizer;
import ca.ubc.bps.timers.StandardIntensity;
import ca.ubc.bps.timers.BruteForceTimer;
import ca.ubc.bps.timers.CompareTimers;
import ca.ubc.bps.timers.ConstantIntensityAdaptiveThinning;
import ca.ubc.bps.timers.HyperbolicJacobianTimer;
import ca.ubc.pdmp.Clock;

public class GeneralizedNormalModel implements Model
{
  @Arg   @DefaultValue("1.0")
  public double alpha = 1.0;
  
  @Arg @DefaultValue("2")
  public   int size = 2;
  
  @Arg                    @DefaultValue("false")
  public boolean forceLogConcaveSolver = false;
  
  @Arg                    @DefaultValue("BRENT")
  public Optimizer optimizer = Optimizer.BRENT;

  @Override
  public void setup(ModelBuildingContext context, boolean initializeStatesFromStationary)
  {
    if (alpha < -1.0)
      throw new RuntimeException("Not defined for alpha < -1.0");
    if (alpha < 0.0 && size == 1)
      throw new RuntimeException("Current implementation not irreducible in 1d for very fat tails");
    if (initializeStatesFromStationary)
      throw new RuntimeException("Not yet supported");
    List<ContinuouslyEvolving> vars = context.buildAndRegisterContinuouslyEvolvingStates(size);
    GeneralizedNormalEnergy energy = new GeneralizedNormalEnergy(alpha);
    
    if (context.dynamics() instanceof PiecewiseLinear)
    {
      Clock timer = 
          alpha < 0.0 || forceLogConcaveSolver ? 
            new NumericalUnimodalInversionTimer(vars, energy, optimizer) :
            new ConstantIntensityAdaptiveThinning(vars, new StandardIntensity(energy));
      context.registerBPSPotential(new BPSPotential(energy, timer));
    } 
    else if (context.dynamics() instanceof Hyperbolic)
    {
      Clock timer0 = new BruteForceTimer(vars, 1e-5, energy);
      Clock timer1 = new NumericalUnimodalInversionTimer(vars, energy, optimizer);
      Clock timer = new CompareTimers(Arrays.asList(timer0, timer1), 1e-4);
      context.registerBPSPotential(new BPSPotential(energy, timer));
      System.out.println("WARNING!!!!!!!");
      HyperbolicJacobianTimer.addLocal(context);
    } 
    else
      throw new RuntimeException();
  }

}
