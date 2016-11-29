package ca.ubc.pdmp;

public class JumpProcess
{
  public final EventTimer timer;
  public final JumpKernel kernel;
  public JumpProcess(EventTimer timer, JumpKernel kernel)
  {
    this.timer = timer;
    this.kernel = kernel;
  }
}