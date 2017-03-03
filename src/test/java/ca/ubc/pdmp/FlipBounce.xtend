package ca.ubc.pdmp

import java.util.List
import ca.ubc.bps.state.ContinuouslyEvolving
import java.util.Random
import ca.ubc.bps.energies.EnergyGradient
import ca.ubc.bps.state.ContinuousStateDependent

import static extension xlinear.MatrixExtensions.*
import static xlinear.MatrixOperations.*

import static java.lang.Math.*

public class FlipBounce extends ContinuousStateDependent implements JumpKernel  {
  
  val EnergyGradient energy;

  new (List<ContinuouslyEvolving> requiredVariables, EnergyGradient energy)
  {
    super(requiredVariables);
    this.energy = energy;
  }

  override simulate(Random random) {
    
    var oldVelocity = denseCopy(currentVelocity)

    velocity = (-1.0 * oldVelocity).vectorToArray
  }
  
}