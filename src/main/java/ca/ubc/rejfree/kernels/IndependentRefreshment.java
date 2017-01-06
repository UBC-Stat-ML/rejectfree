package ca.ubc.rejfree.kernels;

import java.util.Collection;
import java.util.Random;

import ca.ubc.pdmp.JumpKernel;
import ca.ubc.rejfree.state.ContinuousStateDependent;
import ca.ubc.rejfree.state.ContinuouslyEvolving;

public class IndependentRefreshment extends ContinuousStateDependent implements JumpKernel
{
  public IndependentRefreshment(Collection<ContinuouslyEvolving> requiredVariables)
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
