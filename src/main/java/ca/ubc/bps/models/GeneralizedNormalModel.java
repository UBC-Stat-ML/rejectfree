package ca.ubc.bps.models;

import java.util.Arrays;
import java.util.List;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.energies.EnergyComposedWithHyperbolic;
import ca.ubc.bps.energies.GeneralizedNormalEnergy;
import ca.ubc.bps.factory.ModelBuildingContext;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.state.Hyperbolic;
import ca.ubc.bps.state.PiecewiseLinear;
import ca.ubc.bps.timers.QuasiConvexTimer;
import ca.ubc.bps.timers.QuasiConvexTimer.Optimizer;
import ca.ubc.bps.timers.StandardIntensity;
import ca.ubc.bps.timers.BruteForceTimer;
import ca.ubc.bps.timers.CompareTimers;
import ca.ubc.bps.timers.ConstantIntensityAdaptiveThinning;
import ca.ubc.bps.timers.HyperbolicJacobianTimer;
import ca.ubc.pdmp.Clock;
import xlinear.Matrix;

public class GeneralizedNormalModel implements Model
{
  @Arg   @DefaultValue("1.0")
  public double alpha = 1.0;
  
  @Arg @DefaultValue("2")
  public   int size = 2;
  
  @Arg                    @DefaultValue("false")
  public boolean forceQuasiConvexSolver = false;
  
  @Arg                               @DefaultValue("BRENT")
  public Optimizer quasiConvexOptimizer = Optimizer.BRENT;
  
  @Arg                   @DefaultValue("false")
  public boolean testAgainstBruteForce = false;
  
  @Arg                       @DefaultValue("None")
  public Likelihood likelihood = Likelihood.none;

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
    likelihood.setup(context, vars);
    GeneralizedNormalEnergy energy = new GeneralizedNormalEnergy(alpha);
    if (context.dynamics() instanceof PiecewiseLinear)
    {
      if (alpha == 1.0) 
      {
        // special case: normal
        DiagonalPrecision dp = new DiagonalPrecision();
        dp.size = size;
        dp.diagonalPrecision = 2.0;
        Matrix precisionMatrix = dp.build();
        context.registerBPSPotential(FixedPrecisionNormalModel.potential(precisionMatrix, vars));
      }
      else 
      {
        Clock timer = 
            alpha < 1.0 || forceQuasiConvexSolver ? 
              new QuasiConvexTimer(vars, energy, quasiConvexOptimizer) :
              new ConstantIntensityAdaptiveThinning(vars, new StandardIntensity(energy));
        context.registerBPSPotential(new BPSPotential(energy, timer));
      }
    } 
    else if (context.dynamics() instanceof Hyperbolic)
    {
      Clock timer = new QuasiConvexTimer(vars, energy, quasiConvexOptimizer);
      if (testAgainstBruteForce)
        timer = new CompareTimers(
            Arrays.asList(
                new BruteForceTimer(vars, 1e-5, energy), 
                timer), 
            1e-4);
      context.registerBPSPotential(new BPSPotential(new EnergyComposedWithHyperbolic(energy), timer));
      HyperbolicJacobianTimer.addLocal(context);
    } 
    else
      throw new RuntimeException();
  }
}
