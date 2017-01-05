package ca.ubc.pdmp;

public class JumpProcess
{
  public final Clock clock;
  public final JumpKernel kernel;
  public JumpProcess(Clock clock, JumpKernel kernel)
  {
    this.clock = clock;
    this.kernel = kernel;
  }
}