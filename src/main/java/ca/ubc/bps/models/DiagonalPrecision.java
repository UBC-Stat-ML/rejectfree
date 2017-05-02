package ca.ubc.bps.models;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import xlinear.Matrix;
import xlinear.MatrixOperations;

public class DiagonalPrecision implements PrecisionBuilder
{
  @Arg @DefaultValue("2")
  public int size = 2;
  
  @Arg               @DefaultValue("1.0")
  public double diagonalPrecision = 1.0;

  @Override
  public Matrix build()
  {
    return MatrixOperations.identity(size).mul(diagonalPrecision); 
  }
}