package ca.ubc.rejfree.timers;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import bayonet.math.NumericalUtils;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;
import ca.ubc.rejfree.BPSPotential;
import ca.ubc.rejfree.energies.EnergyGradient;
import ca.ubc.rejfree.state.ContinuousStateDependent;

public class Superposition extends ContinuousStateDependent implements Clock, EnergyGradient
{
  public final List<BPSPotential> potentials;

  public Superposition(List<BPSPotential> potentials)
  {
    super(unionOfRequiredVariables(potentials));
    if (potentials.isEmpty())
      throw new RuntimeException();
    this.potentials = potentials;
  }


  private static Collection<Coordinate> unionOfRequiredVariables(List<BPSPotential> potentials)
  {
    Set<Coordinate> result = new LinkedHashSet<>();
    for (BPSPotential potential : potentials)
      result.addAll(potential.clock.requiredVariables());
    return result;
  }

  @Override
  public DeltaTime next(Random random)
  {
    double min = Double.POSITIVE_INFINITY;
    boolean isMinBound = false;
    
    for (BPSPotential potential : potentials)
    {
      DeltaTime current = potential.clock.next(random);
      if (current.deltaTime < min)
      {
        min = current.deltaTime;
        isMinBound = current.isBound;
      }
    }
    
    if (isMinBound)
      return DeltaTime.isGreaterThan(min);
    
    if (random.nextDouble() < acceptanceRate(min))
      return DeltaTime.isEqualTo(min);
    else
      return DeltaTime.isGreaterThan(min);
  }

  private double acceptanceRate(double deltaT)
  {
    double [] velocity = extrapolateVelocity(deltaT);
    double [] position = extrapolatePosition(deltaT);
    
    double denom = 0.0;
    for (BPSPotential potential : potentials)
      denom += canonicalRate(velocity, potential.energy.gradient(position));
    return canonicalRate(velocity, gradient(position)) / denom;
  }

  @Override
  public double[] gradient(double[] point)
  {
    double [] result = new double[point.length];
    
    for (BPSPotential potential : potentials)
    {
      double [] currentGradient = potential.energy.gradient(point);
      for (int i = 0; i < point.length; i++)
        result[i] += currentGradient[i];
    }
        
    return result;
  }

  public static double canonicalRate(double [] velocity, double [] gradient)
  {
    return Math.max(0.0, NumericalUtils.dot(velocity, gradient));
  }
}
