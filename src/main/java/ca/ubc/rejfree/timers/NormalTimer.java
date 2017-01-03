package ca.ubc.rejfree.timers;

import java.util.List;
import java.util.Random;

import bayonet.math.NumericalUtils;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;
import ca.ubc.pdmp.EventTimer;
import ca.ubc.rejfree.state.ContinuousStateDependent;
import ca.ubc.rejfree.state.ContinuouslyEvolving;
import rejfree.StaticUtils;
import rejfree.models.normal.NormalFactor;
import xlinear.DenseMatrix;
import xlinear.Matrix;
import xlinear.MatrixExtensions;
import xlinear.MatrixOperations;

public class NormalTimer extends ContinuousStateDependent implements EventTimer
{
  final Matrix precision;
  
  // Some low level optimization for binary Gaussian potentials 
  // which frequently occur in practice and end up in the inner loop
  final boolean cachedBinary;
  final double p0, p1, d;

  public NormalTimer(List<Coordinate> requiredVariables, Matrix precision)
  {
    super(requiredVariables);
    
    // check dynamics are linear
    for (ContinuouslyEvolving continuous : continuousCoordinates)
      if (!ca.ubc.rejfree.StaticUtils.isPiecewiseLinear(continuous))
        throw new RuntimeException();
    
    this.precision = precision;
    boolean parametersConstant = requiredVariables.size() == precision.nCols();
    boolean isBin = requiredVariables.size() == 2;
    cachedBinary = parametersConstant && isBin;
    p0 = cachedBinary ? precision.get(0,0) : Double.NaN;
    p1 = cachedBinary ? precision.get(1,1) : Double.NaN;
    d  = cachedBinary ? precision.get(0,1) : Double.NaN;
    if (cachedBinary)
      NumericalUtils.checkIsClose(precision.get(0,1), precision.get(1,0));
  }

  @Override
  public DeltaTime next(Random random)
  {
    double xv, vv;
    
    double [] currentPos = currentPosition();
    double [] currentVel = currentVelocity();
    
    if (cachedBinary)
    {
      xv = dotProd(currentPos, currentVel);
      vv = dotProd(currentVel, currentVel);
    }
    else
    {
      final DenseMatrix x = MatrixOperations.denseCopy(currentPos);
      final DenseMatrix v = MatrixOperations.denseCopy(currentVel);
      xv = dotProd(x, v);
      vv = dotProd(v, v);
    }
    final double e = StaticUtils.generateUnitRateExponential(random);
    
    final double delta = NormalFactor.normalCollisionTime(e, xv, vv);
    return DeltaTime.isEqualTo(delta);
  }
  
  private double dotProd(final double [] array0, final double [] array1)
  {
    return 
      array0[0] * (array1[0] * p0 + array1[1] * d) + 
      array0[1] * (array1[0] * d  + array1[1] * p1);
  }
  
  private double dotProd(final DenseMatrix x1, final DenseMatrix x2)
  {
    return MatrixExtensions.dot(x1.transpose().mul(precision),x2);
  }
}
