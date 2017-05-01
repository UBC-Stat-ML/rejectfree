package ca.ubc.bps.timers;

import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.log;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.energies.Energy;
import ca.ubc.bps.energies.HyperbolicJacobianEnergy;
import ca.ubc.bps.factory.ModelBuildingContext;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.state.Hyperbolic;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class HyperbolicJacobianTimer implements Clock
{
  private final ContinuouslyEvolving variable;
  
public HyperbolicJacobianTimer(ContinuouslyEvolving variable)
  {
    if (!(variable.dynamics instanceof Hyperbolic))
      throw new RuntimeException();
    this.variable = variable;
  }
  
  public static void addLocal(ModelBuildingContext context, boolean testAgainstBruteForce)
  {
    
    for (ContinuouslyEvolving var : context.continuouslyEvolvingStates)
    {
      Energy energy = new HyperbolicJacobianEnergy();
      Clock timer = new HyperbolicJacobianTimer(var);
      if (testAgainstBruteForce)
        timer = new CompareTimers(
            Arrays.asList(
                new BruteForceTimer(Collections.singleton(var), 1e-5, energy), 
                timer), 
            1e-4);
      context.registerBPSPotential(
          new BPSPotential(
              energy, 
              timer));
    }
  }

  @Override
  public DeltaTime next(Random random)
  {
    final double b = Hyperbolic.toBdCoord(variable.position.get());
    final double v = variable.velocity.get();
    if (b * v >= 0.0)
      return DeltaTime.infinity(); // isEqualTo(((v >= 0 ? +1 : -1) - b) / v - 1e-5);
    final double h = 2.0 * log(1.0 - abs(b));
    final double e = BPSStaticUtils.sampleUnitRateExponential(random);
    if (e > abs(h))
      return DeltaTime.infinity(); // isEqualTo(((v >= 0 ? +1 : -1) - b) / v - 1e-5);
    final double hPrime = h + e; 
    final double bPrimeAbs = 1.0 - exp(hPrime / 2.0);
    final double deltaXAbs = abs(b) - bPrimeAbs;
    final double result = deltaXAbs / abs(v);
    return DeltaTime.isEqualTo(result);
  }

  @Override
  public Collection<? extends Coordinate> requiredVariables()
  {
    return Collections.singleton(variable);
  }
}
