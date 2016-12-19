package ca.ubc.rejfree;

public class TrajectorySegment
{
  public final double deltaTime;
  public final double startPosition;
  public final double startVelocity;
  public TrajectorySegment(double deltaTime, double startPosition, double startVelocity)
  {
    this.deltaTime = deltaTime;
    this.startPosition = startPosition;
    this.startVelocity = startVelocity;
  }
}
