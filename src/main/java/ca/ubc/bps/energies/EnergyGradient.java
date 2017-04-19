package ca.ubc.bps.energies;

public interface EnergyGradient
{
  double [] gradient(double [] point);
  double valueAt(double [] point);
}
