package ca.ubc.rejfree.energies;

import xlinear.Matrix;

import static xlinear.MatrixOperations.*;

import xlinear.DenseMatrix;

import static xlinear.MatrixExtensions.*;

public class NormalEnergy implements EnergyGradient
{
  final Matrix precision;
  
  public NormalEnergy(Matrix precision)
  {
    this.precision = precision;
  }

  @Override
  public double[] gradient(double[] point)
  {
    DenseMatrix position = denseCopy(point);
    return vectorToArray(precision.mul(position));
  }

}
