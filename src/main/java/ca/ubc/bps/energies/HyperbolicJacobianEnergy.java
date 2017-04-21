package ca.ubc.bps.energies;

public class HyperbolicJacobianEnergy implements Energy
{
  @Override
  public double[] gradient(double[] point)
  {
    double [] result = new double[point.length];
    for (int i = 0; i < point.length; i++)
      result[i] = -2.0 * Math.signum(point[i]) / (1.0 - Math.abs(point[i]));
    return result;
  }

  @Override
  public double valueAt(double[] point)
  {
    double sum = 0.0;
    for (double b : point)
      sum += Math.log(1 - Math.abs(b));
    return 2.0 * sum;
  }
}
