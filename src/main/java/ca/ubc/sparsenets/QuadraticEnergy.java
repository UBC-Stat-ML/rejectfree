package ca.ubc.sparsenets;

import ca.ubc.bps.energies.Energy;

public class QuadraticEnergy implements Energy
{
  private final double wStar;
  private final double tau;
  
  public QuadraticEnergy(double wStar, double tau)
  {
    this.wStar = wStar;
    this.tau = tau;
  }

  @Override
  public double[] gradient(double[] point)
  {
    double sumExp = sumExp(point);
    double constant = tau + 2.0 * (sumExp + wStar);
    double [] result = new double[point.length];
    for (int i = 0; i < point.length; i++)
      result[i] = constant * Math.exp(point[i]);
    return result;
  }

  private double sumExp(double[] point)
  {
    double result = 0.0;
    for (int i = 0; i < point.length; i++)
      result += Math.exp(point[i]);
    return result;
  }

  @Override
  public double valueAt(double[] point)
  {
    double sumExp = sumExp(point);
    double value = sumExp + wStar;
    return value * value + tau * sumExp;
  }
  
}
