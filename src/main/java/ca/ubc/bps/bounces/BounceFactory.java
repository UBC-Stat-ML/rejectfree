package ca.ubc.bps.bounces;

import java.util.List;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import blang.inits.Implementations;
import ca.ubc.bps.energies.Energy;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.pdmp.JumpKernel;
import ca.ubc.bps.bounces.BounceFactory.*;

@Implementations({Standard.class, Flip.class, Randomized.class})
@FunctionalInterface
public interface BounceFactory
{
  public JumpKernel build(List<ContinuouslyEvolving> variables, Energy energy);

  public static class Standard implements BounceFactory
  {
    @Override
    public JumpKernel build(List<ContinuouslyEvolving> variables, Energy energy)
    {
      return new StandardBounce(variables, energy);
    }
  }
  
  public static class Flip implements BounceFactory
  {
    @Override
    public JumpKernel build(List<ContinuouslyEvolving> variables, Energy energy)
    {
      return new FlipBounce(variables);
    }
  }
  
  public static class Randomized implements BounceFactory
  {
    @Arg @DefaultValue("false") 
    public boolean ignoreIncomingAngle = false;
    
    @Override
    public JumpKernel build(List<ContinuouslyEvolving> variables, Energy energy)
    {
      return new RandomizedBounce(variables, energy, ignoreIncomingAngle);
    }
  }
}
