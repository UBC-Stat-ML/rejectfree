package ca.ubc.bps.models;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import xlinear.Matrix;
import xlinear.MatrixOperations;
import xlinear.SparseMatrix;

public class BrownianMotion implements PrecisionBuilder
{
  @Arg @DefaultValue("2")
  public int size = 2;
  
  @Arg @DefaultValue("2")
  public double sigma = 1;

  @Override
  public Matrix build()
  {
    SparseMatrix result = MatrixOperations.sparse(size, size);
    for (int i = 0; i < size; i++) 
    {
      result.set(i, i, 2.0 * sigma);
      if (i - 1 >= 0.0)
        result.set(i, i - 1, -1.0 * sigma);
      if (i + 1 < size)
        result.set(i, i + 1, -1.0 * sigma);
    }
    return result;
  }

}
