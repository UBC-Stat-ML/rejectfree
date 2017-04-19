package ca.ubc.bps.state;

public class SimpleMutableDouble implements MutableDouble
{
  double value = 0.0;

  @Override
  public void set(double value)
  {
    this.value = value;
  }

  @Override
  public double get()
  {
    return value;
  }
  
}