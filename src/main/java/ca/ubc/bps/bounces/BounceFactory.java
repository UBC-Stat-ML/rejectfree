package ca.ubc.bps.bounces;

import java.util.List;

import ca.ubc.bps.energies.EnergyGradient;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.pdmp.JumpKernel;

@FunctionalInterface
public interface BounceFactory
{
  public JumpKernel build(List<ContinuouslyEvolving> variables, EnergyGradient energy);


}
