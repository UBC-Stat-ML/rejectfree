package ca.ubc.bps.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.PoissonDistribution;

import bayonet.distributions.Random2RandomGenerator;
import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.energies.Energy;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.timers.Superposition;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class Poisson extends SimpleLikelihood<Integer>
{
  @Override
  public Integer sampleDatapoint(double latentVariable, Random random)
  {
    final PoissonDistribution pd = new PoissonDistribution(new Random2RandomGenerator(random), 
        Math.exp(latentVariable), 
        PoissonDistribution.DEFAULT_EPSILON, 
        PoissonDistribution.DEFAULT_MAX_ITERATIONS);
    return pd.sample(); 
  }

  @Override
  public BPSPotential createLikelihoodPotential(ContinuouslyEvolving latentVariable, Integer observation)
  {
    List<BPSPotential> potentials = new ArrayList<>(); 
    potentials.add(linearPotential(latentVariable, -observation));
    potentials.add(new BPSPotential(new PoissonLogNormEnergy(), new PoissonLogNormTimer(latentVariable)));
    return Superposition.createSuperpositionBPSPotential(potentials);
  }
  
  public static BPSPotential linearPotential(ContinuouslyEvolving latentVariable, double coefficient)
  {
    return new BPSPotential(new LinearUnivariateEnergy(coefficient), new LinearUnivariateTimer(latentVariable, coefficient));
  }
  
  private static class PoissonLogNormEnergy implements Energy
  {
    @Override
    public double[] gradient(double[] point)
    {
      if (point.length != 1)
        throw new RuntimeException();
      return new double[]{valueAt(point)};
    }

    @Override
    public double valueAt(double[] point)
    {
      if (point.length != 1)
        throw new RuntimeException();
      return Math.exp(point[0]);
    }
  }
  
  private static class PoissonLogNormTimer implements Clock
  {
    private final ContinuouslyEvolving variable;
    
    public PoissonLogNormTimer(ContinuouslyEvolving variable)
    {
      this.variable = variable;
    }

    @Override
    public Collection<? extends Coordinate> requiredVariables()
    {
      return Collections.singleton(variable);
    }

    @Override
    public DeltaTime next(Random random)
    {
      double x = variable.position.get();
      double v = variable.velocity.get();
      double e = BPSStaticUtils.sampleUnitRateExponential(random);
      double candidate = (Math.log(e + Math.exp(x)) - x) / v;
      if (candidate > 0.0)
        return DeltaTime.isEqualTo(candidate);
      else
        return DeltaTime.infinity();
    }
  }
  
  public static class LinearUnivariateEnergy implements Energy
  {
    private final double coefficient;
    private final double [] coefficientAsArray;
    
    public LinearUnivariateEnergy(double coefficient)
    {
      this.coefficient = coefficient;
      this.coefficientAsArray = new double[]{coefficient};
    }

    @Override
    public double[] gradient(double[] point)
    {
      if (point.length != 1)
        throw new RuntimeException();
      return coefficientAsArray;
    }

    @Override
    public double valueAt(double[] point)
    {
      if (point.length != 1)
        throw new RuntimeException();
      return coefficient * point[0];
    }
  }
  
  public static class LinearUnivariateTimer implements Clock
  {
    private final ContinuouslyEvolving variable;
    private final double coefficient;
    
    public LinearUnivariateTimer(ContinuouslyEvolving variable, double coefficient)
    {
      this.variable = variable;
      this.coefficient = coefficient;
    }

    @Override
    public Collection<? extends Coordinate> requiredVariables()
    {
      return Collections.singleton(variable);
    }

    @Override
    public DeltaTime next(Random random)
    {
      double denom = coefficient * variable.velocity.get();
      if (denom > 0.0)
        return DeltaTime.isEqualTo(BPSStaticUtils.sampleUnitRateExponential(random) / denom);
      else
        return DeltaTime.infinity();
    }
  }
}
