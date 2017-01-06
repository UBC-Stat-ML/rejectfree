package ca.ubc.rejfree.kernels;

import java.util.List;
import java.util.Random;

import org.jblas.DoubleMatrix;

import ca.ubc.pdmp.JumpKernel;
import ca.ubc.rejfree.energies.EnergyGradient;
import ca.ubc.rejfree.state.ContinuousStateDependent;
import ca.ubc.rejfree.state.ContinuouslyEvolving;
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
