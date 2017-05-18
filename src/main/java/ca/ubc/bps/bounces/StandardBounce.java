package ca.ubc.bps.bounces;

import java.util.List;
import java.util.Random;

import org.jblas.DoubleMatrix;

import ca.ubc.bps.energies.Energy;
import ca.ubc.bps.state.PositionVelocityDependent;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.pdmp.JumpKernel;

public class StandardBounce extends PositionVelocityDependent implements JumpKernel
{
  private final Energy energy;

  public StandardBounce(List<PositionVelocity> requiredVariables, Energy energy)
  {
    super(requiredVariables);
    this.energy = energy;
  }

  @Override
  public void simulate(Random random)
  {
    DoubleMatrix oldVelocity = new DoubleMatrix(currentVelocity());
    DoubleMatrix gradient = new DoubleMatrix(energy.gradient(currentPosition()));
    setVelocity(bounce(oldVelocity, gradient).data);
  }
  
  /**
   * 
   * @param oldVelocity Row vector of velocities before collision
   * @param gradient Row vector of the gradient of the energy at collision
   * @return Row vector of updated velocities
   */
  public static DoubleMatrix bounce(DoubleMatrix oldVelocity, DoubleMatrix gradient)
  {
    final double scale = 2.0 * gradient.dot(oldVelocity) / gradient.dot(gradient);
    return oldVelocity.sub(gradient.mul(scale)); 
  }
}
