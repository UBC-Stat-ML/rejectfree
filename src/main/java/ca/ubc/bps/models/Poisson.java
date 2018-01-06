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
import ca.ubc.bps.energies.EnergySum;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.bps.state.IsotropicHamiltonian;
import ca.ubc.bps.state.PiecewiseLinear;
import ca.ubc.bps.timers.PoissonProcess;
import ca.ubc.bps.timers.Superposition;
import ca.ubc.bps.timers.ThinningTimer;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class Poisson extends SimpleLikelihood<Integer>
{
  @Override
  protected Integer sampleDatapoint(double latentVariable, Random random)
  {
    final PoissonDistribution pd = new PoissonDistribution(new Random2RandomGenerator(random), 
        Math.exp(latentVariable), 
        PoissonDistribution.DEFAULT_EPSILON, 
        PoissonDistribution.DEFAULT_MAX_ITERATIONS);
    return pd.sample(); 
  } 
  
  @Override
  protected Integer parse(String string)
  {
    return Integer.parseInt(string);
  }

  @Override
  public BPSPotential createLikelihoodPotential(PositionVelocity latentVariable, Integer observation)
  {
    if (latentVariable.dynamics instanceof PiecewiseLinear)
    {
      List<BPSPotential> potentials = new ArrayList<>(); 
      potentials.add(linearPotential(latentVariable, -observation));
      potentials.add(new BPSPotential(new PoissonLogNormEnergy(), new PoissonLogNormTimer(latentVariable)));
      return Superposition.createSuperpositionBPSPotential(potentials);
    } 
    else if (latentVariable.dynamics instanceof IsotropicHamiltonian)
    {
      List<Energy> energies = new ArrayList<>();
      energies.add(new LinearUnivariateEnergy(-observation));
      energies.add(new PoissonLogNormEnergy());
      Energy sum = new EnergySum(energies);
      HamiltonianPoissonBound bound = new HamiltonianPoissonBound(latentVariable, observation);
      ThinningTimer timer = new ThinningTimer(Collections.singleton(latentVariable), sum, bound);
      return new BPSPotential(sum, timer);
    }
    else
      throw new RuntimeException();
  }
  
  public static class HamiltonianPoissonBound implements PoissonProcess
  {
    private final PositionVelocity variable;
    private final double precision;
    private final int observation;
    
    public HamiltonianPoissonBound(PositionVelocity variable, int observation)
    {
      this.variable = variable;
      this.observation = observation;
      this.precision = ((IsotropicHamiltonian) (variable.dynamics)).getPrecision();
    }

    @Override
    public DeltaTime next(Random random)
    {
      return DeltaTime.isEqualTo(BPSStaticUtils.sampleExponential(random, rate()));
    }
    
    private double B(double xi) 
    {
      return Math.exp(xi) + observation;
    }

    @Override
    public double evaluate(double delta)
    {
      return rate();
    }
    
    public double rate()
    {
      double x = variable.position.get();
      double v = variable.velocity.get();
      double a = - precision * x;
      double b = v;
      double c = v / precision;
      double d = x;
      return Math.sqrt(a*a + b*b) * B(Math.sqrt(c*c + d * d));
    }
    
  }
  
  public static BPSPotential linearPotential(PositionVelocity latentVariable, double coefficient)
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
    private final PositionVelocity variable;
    
    public PoissonLogNormTimer(PositionVelocity variable)
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
    private final PositionVelocity variable;
    private final double coefficient;
    
    public LinearUnivariateTimer(PositionVelocity variable, double coefficient)
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
