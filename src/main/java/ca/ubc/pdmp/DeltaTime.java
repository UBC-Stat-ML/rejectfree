package ca.ubc.pdmp;

import java.util.Random;

public class DeltaTime
{
  public final double deltaTime;
  public final boolean isBound;
  
  public DeltaTime(double deltaTime, boolean isBound)
  {
    if (!(deltaTime > 0.0))
      throw new RuntimeException("Bad delta: " + deltaTime);
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
  
  public static DeltaTime infinity()
  {
    if (PDMPSimulator.USING_DEFAULT_CHUNK_LENGTH)
      return DeltaTime.isGreaterThan(LARGE_VALUE);
    else 
      // To avoid collisions in the priority queue
      return DeltaTime.isGreaterThan(LARGE_VALUE + jitter.get().nextDouble());
  }

  @Override
  public String toString()
  {
    return "DeltaTime" + (isBound ? ">" : "=") + deltaTime;
  }
  
  final static double LARGE_VALUE = 1.0 + PDMPSimulator.DEFAULT_CHUNK_LENGTH;
  private static ThreadLocal<Random> jitter = new ThreadLocal<Random>() 
  {
    @Override protected Random initialValue() { return new Random(49586); }
  };
}