package ca.ubc.sparsenets;

import java.util.List;

import ca.ubc.bps.energies.Energy;

public class LinearEnergy implements Energy
{
  private final List<Integer> ms;
  private final double sigma;
  
  public LinearEnergy(List<Integer> ms, double sigma)
  {
    this.ms = ms;
    this.sigma = sigma;
  }

  double [] gradient = null;
  @Override
  public double[] gradient(double[] point)
  {
    if (gradient == null)
    {
      gradient = new double[point.length];
      for (int i = 0; i < point.length; i++)
        gradient[i] = (sigma - ms.get(i));
    }
    return gradient;
  }

  @Override
  public double valueAt(double[] point)
  {
    double sum = 0.0;
    for (int i = 0; i < point.length; i++)
      sum += (sigma - ms.get(i)) * point[i];
    return sum;
  }

}
