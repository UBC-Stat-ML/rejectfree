package ca.ubc.bps.energies;

public class EnergyUtils
{
  public static boolean isOutOfBound(double [] points)
  {
    for (double p : points)
      if (Double.isNaN(p))
        return true;
    return false;
  }
}
