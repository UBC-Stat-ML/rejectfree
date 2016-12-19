package ca.ubc.pdmp;

public class DeltaTime
{
  public final double deltaTime;
  public final boolean isBound;
  
  public DeltaTime(double deltaTime, boolean isBound)
  {
    this.deltaTime = deltaTime;
    this.isBound = isBound;
  }

  public static DeltaTime isEqualTo(double time)
  {
    return new DeltaTime(time, false);
  }
  
  public static DeltaTime isGreaterThan(double time)
  {
    return new DeltaTime(time, true);
  }
}