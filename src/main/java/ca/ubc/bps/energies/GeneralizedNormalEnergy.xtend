package ca.ubc.bps.energies

import org.eclipse.xtend.lib.annotations.Data
import xlinear.DenseMatrix

import static xlinear.MatrixOperations.denseCopy
import static extension xlinear.MatrixExtensions.*

/**
 * target proportional to exp( - ||x||^{alpha + 1} )
 */
@Data
class GeneralizedNormalEnergy implements Energy {
  val double alpha
  override double[] gradient(double[] point) {
    val DenseMatrix position = denseCopy(point)
    var double norm = position.norm
    if (alpha < 1.0 && norm == 0.0) {
      norm = 1 // probability zero but needed in some cases to get out of initialization
    }
    return ((alpha + 1.0) * (norm ** (alpha - 1.0)) * position).vectorToArray
  }
  
  override double valueAt(double[] point) {
    if (EnergyUtils::isOutOfBound(point)) {
      return Double.POSITIVE_INFINITY;
    }
    val DenseMatrix position = denseCopy(point)
    var double norm = position.norm
    return (norm ** (alpha + 1.0))
  }
  
}
