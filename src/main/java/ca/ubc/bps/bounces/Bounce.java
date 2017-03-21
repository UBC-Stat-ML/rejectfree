package ca.ubc.bps.bounces;

import java.util.List;
import java.util.Random;

import org.jblas.DoubleMatrix;

import ca.ubc.bps.energies.EnergyGradient;
import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.pdmp.JumpKernel;
import rejfree.StaticUtils;

public class Bounce extends ContinuousStateDependent implements JumpKernel
{
  private final EnergyGradient energy;

  public Bounce(List<ContinuouslyEvolving> requiredVariables, EnergyGradient energy)
  {
    super(requiredVariables);
    this.energy = energy;
  }

  @Override
  public void simulate(Random random)
  {
    DoubleMatrix oldVelocity = new DoubleMatrix(currentVelocity());
    DoubleMatrix gradient = new DoubleMatrix(energy.gradient(currentPosition()));
    setVelocity(StaticUtils.bounce(oldVelocity, gradient).data);
  }
}
