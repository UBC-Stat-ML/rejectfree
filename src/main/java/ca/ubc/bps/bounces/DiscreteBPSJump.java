package ca.ubc.bps.bounces;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jblas.DoubleMatrix;

import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.energies.EnergyInPlace;
import ca.ubc.bps.state.PositionVelocityDependent;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.JumpKernel;

public abstract class DiscreteBPSJump extends PositionVelocityDependent implements JumpKernel 
{
  private final List<EnergyInPlace> energies;
  private double[] startPosition; 
  private double cachedDensity = Double.NaN;
  private final LinkedHashSet<Coordinate> requiredVariables;
  private final int[][] gradientProjections;
  private final int nVariablesTransformed;
  
  public final double discretizationSize;
  
  /**
   * @param requiredVariables
   * @param energy Should correspond to the Markov blanket of the requiredVariables
   * @param stepSize
   */
  public DiscreteBPSJump(
      Collection<? extends Coordinate> variables, 
      List<EnergyInPlace> energies,
      double discretizationSize)
  {
    super(variables);
    if (discretizationSize <= 0.0)
      throw new RuntimeException();
    this.energies = energies;
    this.discretizationSize = discretizationSize;
    this.requiredVariables = new LinkedHashSet<>();
    this.requiredVariables.addAll(variables);
    for (EnergyInPlace energy : energies)
      this.requiredVariables.addAll(energy.requiredVariables());
    gradientProjections = gradientProjections(energies, variables);
    this.nVariablesTransformed = variables.size();
  }

  @Override
  public void simulate(Random random)
  {
    double oldLogDensity = oldLogDensity();
    applyFlow();
    double newLogDensity = evalLogDensity();
    cachedDensity = newLogDensity;
    if (newLogDensity > oldLogDensity)
      ; // nothing to do
    else
    {
      double logSlice = oldLogDensity - BPSStaticUtils.sampleUnitRateExponential(random);
      if (logSlice < newLogDensity)
        ; // nothing to do
      else
      {
        cachedDensity = oldLogDensity;
        reset();
        bounce();
        flip();
        applyFlow();
        double mirrorLogDensity = evalLogDensity();
        reset();
        if (logSlice < mirrorLogDensity)
          flip();  // dynamics outside but mirror inside -> need to flip
        else
          bounce();
      }
    }
    // TODO: could add auto-regressive update on the velocity here
    // TODO: acceleration version by lifting the auto-regressive updates?
  }
  
  private void flip()
  {
    double [] velocity = currentVelocity();
    for (int i = 0; i < velocity.length; i++)
      velocity[i] = -velocity[i];
    setVelocity(velocity); 
  }

  private void bounce()
  {
    DoubleMatrix oldVelocity = new DoubleMatrix(currentVelocity());
    DoubleMatrix gradient = new DoubleMatrix(gradient());
    setVelocity(StandardBounce.bounce(oldVelocity, gradient).data);
  }

  public abstract void applyFlow();
  
  private double oldLogDensity()
  {
    startPosition = currentPosition();
    if (!Double.isNaN(cachedDensity))
      return cachedDensity;
    return evalLogDensity();
  }
  
  private double evalLogDensity()
  {
    double sum = 0.0;
    for (EnergyInPlace e : energies)
      sum -= e.valueAt();
    return sum;
  }
  
  private double [] gradient() 
  {
    double [] result = new double[nVariablesTransformed];
    for (int eIndex = 0; eIndex < gradientProjections.length; eIndex++)
    {
      double [] current = energies.get(eIndex).gradient();
      for (int cIndex = 0; cIndex < current.length; cIndex++)
      {
        int projectedIndex = gradientProjections[eIndex][cIndex];
        if (projectedIndex != -1)
          result[projectedIndex] += current[cIndex];
      }
    }
    return result;
  }
  
  private void reset() 
  {
    setPosition(startPosition);
  }
  
  private static int[][] gradientProjections(List<EnergyInPlace> energies, Collection<? extends Coordinate> variables)
  {
    Map<Coordinate,Integer> reverseIndex = new HashMap<>();
    int cIndex = 0;
    for (Coordinate c : variables)
      reverseIndex.put(c, cIndex++);
    
    int[][] result = new int[energies.size()][];
    for (int eIndex = 0; eIndex < energies.size(); eIndex++)
    {
      EnergyInPlace currentE = energies.get(eIndex);
      result[eIndex] = new int[currentE.requiredVariables().size()];
      cIndex = 0;
      for (Coordinate c : currentE.requiredVariables())
      {
        Integer index = reverseIndex.get(c);
        result[eIndex][cIndex++] = index == null ? -1 : index;
      }
    }
    return result;
  }

  @Override
  public final Collection<? extends Coordinate> requiredVariables()
  {
    return requiredVariables;
  }
}
