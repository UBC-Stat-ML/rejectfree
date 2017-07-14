package ca.ubc.bps.models;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import xlinear.Matrix;
import xlinear.MatrixOperations;
import xlinear.SparseMatrix;

public class RadfordNealExample implements PrecisionBuilder
{
  @Arg @DefaultValue("2")
  public int size = 2;

  @Override
  public Matrix build()
  {
    double delta = 1.0 / ((double) size);
    SparseMatrix result = MatrixOperations.sparse(size, size);
    for (int i = 0; i < size; i++) 
    {
      double stdDev = ((double) (i+1)) * delta;
      double variance = stdDev * stdDev;
      // for convenience, fill such that variable 0 has unit variance
      result.set(size - i - 1, size - i - 1, variance);
    }
    return result;
  }

}
