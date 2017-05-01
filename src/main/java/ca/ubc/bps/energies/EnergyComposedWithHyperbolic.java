package ca.ubc.bps.energies;

import ca.ubc.bps.state.Hyperbolic;

public class EnergyComposedWithHyperbolic implements Energy
{
  private final Energy basicEnergy;
  
  public EnergyComposedWithHyperbolic(Energy basicEnergy)
  {
    this.basicEnergy = basicEnergy;
  }

  @Override
  public double[] gradient(double[] point)
  {
    double [] result = basicEnergy.gradient(point);
    for (int i = 0; i < point.length; i++)
      result[i] *= Math.pow(1.0 - Math.abs(Hyperbolic.toBdCoord(point[i])), -2.0);
    return result;
  }

  @Override
  public double valueAt(double[] point)
  {
    return basicEnergy.valueAt(point);
  }

}
