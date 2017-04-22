package ca.ubc.bps.timers;

import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.log;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.BPSStaticUtils;
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
  
  public static void addLocal(ModelBuildingContext context)
  {
    for (ContinuouslyEvolving var : context.continuouslyEvolvingStates)
      context.registerBPSPotential(
          new BPSPotential(
              new HyperbolicJacobianEnergy(), 
              new HyperbolicJacobianTimer(var)));
  }
  
//  public static void addGlobal(ModelBuildingContext context)
//  {
//    START WITH LOCAL!
//    
//    // First, prepare a superposition clock
//    List<BPSPotential> potentials = new ArrayList<>();
//    for (ContinuouslyEvolving var : context.continuouslyEvolvingStates)
//      potentials.add(new BPSPotential(energy, new HyperbolicJacobianTimer(var)))
//      
//      
//    StandardBounce bounce = new StandardBounce(context.continuouslyEvolvingStates, new HyperbolicJacobianEnergy());
//    Superposition superposition = new Superposition(potentials)
//  }

  @Override
  public DeltaTime next(Random random)
  {
    final double b = Hyperbolic.toBdCoord(variable.position.get());
    final double v = variable.velocity.get();
    if (b * v >= 0.0)
      return DeltaTime.infinity(); 
    final double h = 2.0 * log(1.0 - abs(b));
    final double e = BPSStaticUtils.sampleUnitRateExponential(random);
    if (abs(h) > e)
      return DeltaTime.infinity();
    return DeltaTime.isEqualTo( ( exp((h + e)/2.0) + 1 - abs(b) ) / abs(v));
  }

  @Override
  public Collection<? extends Coordinate> requiredVariables()
  {
    return Collections.singleton(variable);
  }
}
