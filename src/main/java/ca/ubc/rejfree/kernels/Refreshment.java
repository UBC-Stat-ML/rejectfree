package ca.ubc.rejfree.kernels;

import java.util.List;
import java.util.Random;

import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.JumpKernel;
import ca.ubc.rejfree.ContinuousStateDependent;

public class Refreshment extends ContinuousStateDependent implements JumpKernel
{
  public Refreshment(List<Coordinate> requiredVariables)
  {
    super(requiredVariables);
  }

  @Override
  public void simulate(Random random)
  {
    for (int i = 0; i < requiredVariables.size(); i++)
      continuousCoordinates.get(i).velocity.set(random.nextGaussian());
  }

}
