package ca.ubc.bps.energies;

import java.util.List;

public class EnergySum implements Energy
{
  private final List<Energy> energies;
  
  public EnergySum(List<Energy> energies)
  {
    this.energies = energies;
  }

  @Override
  public double[] gradient(double[] point)
  {
    double [] result = new double[point.length];
    for (Energy energy : energies)
    {
      double [] currentGradient = energy.gradient(point);
      for (int i = 0; i < point.length; i++)
        result[i] += currentGradient[i];
    }
    return result;
  }

  @Override
  public double valueAt(double[] point)
  {
    double result = 0.0;
    for (Energy energy : energies)
      result += energy.valueAt(point);
    return result;
  }
}
