package ca.ubc.bps.energies;

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

  @Override
  public double valueAt(double[] point)
  {
    DenseMatrix position = denseCopy(point); 
    return 0.5 * position.transpose().mul(precision).mul(position).get(0);
  }

}
