package ca.ubc.bps.state;

public class MonitoredMutableDouble implements MutableDouble
{
  double value = 0.0;
  public final ModCount modCount;
  
  public MonitoredMutableDouble(ModCount modCount)
  {
    this.modCount = modCount;
  }

  @Override
  public void set(double value)
  {
    modCount.count++;
    this.value = value;
  }

  @Override
  public double get()
  {
    return value;
  }
  
  public static class ModCount
  {
    public long count = 0;
  }
  
}