package ca.ubc.rejfree.timers;

import java.util.List;
import java.util.Random;

import org.jblas.DoubleMatrix;

import bayonet.math.NumericalUtils;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;
import ca.ubc.pdmp.EventTimer;
import ca.ubc.rejfree.ContinuousStateDependent;
import rejfree.StaticUtils;
import rejfree.models.normal.NormalFactor;

public class NormalTimer extends ContinuousStateDependent implements EventTimer
{
  final DoubleMatrix precision;
  
  // Some low level optimization for binary Gaussian potentials 
  // which frequently occurs in practice and ends up in the inner loop
  final boolean cachedBinary;
  final double p0, p1, d;

  public NormalTimer(List<Coordinate> requiredVariables, DoubleMatrix precision)
  {
    super(requiredVariables);
    this.precision = precision;
    boolean parametersConstant = requiredVariables.size() == precision.getColumns();
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
    final DoubleMatrix x = new DoubleMatrix(currentPosition());
    final DoubleMatrix v = new DoubleMatrix(currentVelocity());
    
    final double xv = dotProd(x, v);
    final double vv = dotProd(v, v);
    final double e = StaticUtils.generateUnitRateExponential(random);
    
    return DeltaTime.isEqualTo(NormalFactor.normalCollisionTime(e, xv, vv));
  }
  
  private double dotProd(final DoubleMatrix x1, final DoubleMatrix x2)
  {
    // from NormalFactor.java in previous version
    if (cachedBinary)
    { // optimization of computational inner-loop bottleneck
      final double [] 
        array0 = x1.data,
        array1 = x2.data;
      return 
        array0[0] * (array1[0] * p0 + array1[1] * d) + 
        array0[1] * (array1[0] * d  + array1[1] * p1);
    }
    else
      return x1.transpose().mmuli(precision).dot(x2);
  }
}
