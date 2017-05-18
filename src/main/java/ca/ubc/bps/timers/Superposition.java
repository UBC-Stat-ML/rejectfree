package ca.ubc.bps.timers;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.energies.Energy;
import ca.ubc.bps.energies.EnergySum;
import ca.ubc.bps.state.PositionVelocityDependent;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class Superposition extends PositionVelocityDependent implements PoissonProcess
{
  public final List<BPSPotential> potentials;
  
  public Superposition(List<BPSPotential> potentials)
  {
    super(unionOfRequiredVariables(potentials));
    this.potentials = potentials;
  }

  public static BPSPotential createSuperpositionBPSPotential(List<BPSPotential> potentials)
  {
    List<Energy> energies = potentials.stream().map(p -> p.energy).collect(Collectors.toList());
    EnergySum energySum = new EnergySum(energies);
    ThinningTimer thinning = new ThinningTimer(
        unionOfRequiredVariables(potentials), 
        energySum, 
        new Superposition(potentials));
    return new BPSPotential(energySum, thinning); 
  }

  private static Collection<Coordinate> unionOfRequiredVariables(List<BPSPotential> potentials)
  {
    Set<Coordinate> result = new LinkedHashSet<>();
    for (BPSPotential potential : potentials)
      result.addAll(potential.clock.requiredVariables());
    return result;
  }

  @Override
  public double evaluate(double deltaT)
  {
    double [] velocity = extrapolateVelocity(deltaT);
    double [] position = extrapolatePosition(deltaT); 
    double intensity = 0.0;
    for (BPSPotential potential : potentials)
      intensity += StandardIntensity.canonicalRate(velocity, potential.energy.gradient(position));
    return intensity;
  }

  @Override
  public DeltaTime next(Random random)
  {
    DeltaTime result = null;
    double min = Double.POSITIVE_INFINITY;
    
    for (BPSPotential potential : potentials)
    {
      DeltaTime current = potential.clock.next(random);
      if (current.deltaTime < min)
      {
        min = current.deltaTime;
        result = current;
      }
    }
    if (result == null)
      throw new RuntimeException();
    return result;
  }
}
