package ca.ubc.rejfree.kernels;

import java.util.List;
import java.util.Random;

import ca.ubc.pdmp.JumpKernel;
import ca.ubc.rejfree.state.ContinuousStateDependent;
import ca.ubc.rejfree.state.ContinuouslyEvolving;

public class Refreshment extends ContinuousStateDependent implements JumpKernel
{
  public Refreshment(List<ContinuouslyEvolving> requiredVariables)
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
