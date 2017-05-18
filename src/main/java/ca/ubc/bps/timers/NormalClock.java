package ca.ubc.bps.timers;

import java.util.List;
import java.util.Random;

import bayonet.math.NumericalUtils;
import ca.ubc.pdmp.DeltaTime;
import ca.ubc.bps.state.PositionVelocityDependent;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.bps.state.PiecewiseLinear;
import ca.ubc.pdmp.Clock;
import xlinear.DenseMatrix;
import xlinear.Matrix;
import xlinear.MatrixExtensions;
import xlinear.MatrixOperations;

public class NormalClock extends PositionVelocityDependent implements Clock
{
  final Matrix precision;
  
  // Some low level optimization for binary Gaussian potentials 
  // which frequently occur in practice and end up in the inner loop
  final boolean cachedBinary;
  final double p0, p1, d;

  public NormalClock(List<PositionVelocity> requiredVariables, Matrix precision)
  {
    super(requiredVariables);
    this.precision = precision;
    boolean parametersConstant = requiredVariables.size() == precision.nCols();
    boolean isBin = requiredVariables.size() == 2;
    cachedBinary = parametersConstant && isBin;
    p0 = cachedBinary ? precision.get(0,0) : Double.NaN;
    p1 = cachedBinary ? precision.get(1,1) : Double.NaN;
    d  = cachedBinary ? precision.get(0,1) : Double.NaN;
    if (cachedBinary)
      NumericalUtils.checkIsClose(precision.get(0,1), precision.get(1,0));
    if (!(requiredVariables.get(0).dynamics instanceof PiecewiseLinear))
      throw new RuntimeException();
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
    final double e = generateUnitRateExponential(random);
    
    final double delta = normalCollisionTime(e, xv, vv);
    
    return DeltaTime.isEqualTo(delta);
  }
  
  public static double generateUnitRateExponential(Random random)
  {
    return - Math.log(random.nextDouble());
  }
  
  public static double normalCollisionTime(double exponential, double xv, double vv)
  {
    final double s1 = xv < 0 ? - xv / vv : 0.0;
    final double C = - exponential - s1 * (xv + vv * s1 / 2.0);
    final double result = (- xv + Math.sqrt(xv * xv - 2.0 * vv * C)) / vv;
    return result;
  }
  
  private double dotProd(final double [] array0, final double [] array1)
  {
    return 
      array0[0] * (array1[0] * p0 + array1[1] * d) + 
      array0[1] * (array1[0] * d  + array1[1] * p1);
  }
  
  private double dotProd(final DenseMatrix x1, final DenseMatrix x2)
  {
    return MatrixExtensions.dot(x1,precision.mul(x2));
  }
}
