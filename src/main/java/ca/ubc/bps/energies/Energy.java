package ca.ubc.bps.energies;

public interface Energy
{
  double [] gradient(double [] point);
  double valueAt(double [] point);
}
