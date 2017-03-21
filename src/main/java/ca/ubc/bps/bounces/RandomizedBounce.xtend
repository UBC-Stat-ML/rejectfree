package ca.ubc.bps.bounces

import java.util.List
import ca.ubc.bps.state.ContinuouslyEvolving
import java.util.Random
import ca.ubc.bps.energies.EnergyGradient
import ca.ubc.bps.state.ContinuousStateDependent

import static extension xlinear.MatrixExtensions.*
import static xlinear.MatrixOperations.*

import static java.lang.Math.*
import ca.ubc.pdmp.JumpKernel

public class RandomizedBounce extends ContinuousStateDependent implements JumpKernel  {
  
  val EnergyGradient energy
  var boolean completelyIgnoreIncomingAngle

  new (List<ContinuouslyEvolving> requiredVariables, EnergyGradient energy, boolean completelyIgnoreIncomingAngle)
  {
    super(requiredVariables);
    this.energy = energy;
  }

  override simulate(Random random) {
    
    var oldVelocity = denseCopy(currentVelocity)
    val oldNorm = oldVelocity.norm
    oldVelocity = oldVelocity / oldNorm
    val dim = oldVelocity.nEntries
    val gradient = denseCopy(energy.gradient(currentPosition))
    
    if (completelyIgnoreIncomingAngle) {
      // randomize oldVelocity subject to positive dot product with energy gradient
      for (var int i = 0; i < dim; i++) {
        oldVelocity.set(i, random.nextGaussian);
      }
      oldVelocity = oldVelocity / oldVelocity.norm
      if (oldVelocity.dot(gradient) < 0) {
        oldVelocity = oldVelocity * 1.0
      }
    }
      
    val n1 = -1.0 * gradient / gradient.norm 
    var n2 = oldVelocity - n1 * (oldVelocity.dot(n1))
    n2 = n2 / n2.norm
    
    val a = random.nextDouble() ** (1.0 / (dim - 1.0))
    val b = sqrt(1.0 - a*a)

    velocity = ((a * n2 + b * n1) * oldNorm).vectorToArray
  }
  
}