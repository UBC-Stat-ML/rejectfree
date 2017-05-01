package ca.ubc.bps.energies;

import ca.ubc.bps.state.Hyperbolic;

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
    for (double x : point)
      sum += Math.log(1 - Math.abs(Hyperbolic.toBdCoord(x)));
    return 2.0 * sum;
  }
  
  public static void main(String [] args)
  {
    HyperbolicJacobianEnergy instance = new HyperbolicJacobianEnergy();
    final double x = 13.234;
    final double delta = 1e-6;
    
    double [] xs = new double[]{x};
    
    double [] analytic = instance.gradient(xs);
    
    System.out.println(analytic[0]);
    
    double numeric = (instance.valueAt(xs) - instance.valueAt(new double[]{x + delta}))/delta;
    
    System.out.println(numeric);
  }
}
