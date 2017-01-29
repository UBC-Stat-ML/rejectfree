package ca.ubc.bps.energies;

/**
 * Adds a computational model on top of gradient evaluation, where 
 * evaluation of a d-dimensional gradient incurs cost d.
 * 
 * @author bouchard
 */
public class MonitoredEnergy implements EnergyGradient
{
  private final EnergyGradient enclosed;
  private long cost = 0;
  
  public long getCost()
  {
    return cost;
  }

  public MonitoredEnergy(EnergyGradient enclosed)
  {
    this.enclosed = enclosed;
  }

  @Override
  public double[] gradient(double[] point)
  {
    cost += point.length;
    return enclosed.gradient(point);
  }

}
