package ca.ubc.bps.bounces

import java.util.List
import java.util.Random

import static extension xlinear.MatrixExtensions.*
import static xlinear.MatrixOperations.*

import ca.ubc.pdmp.JumpKernel
import ca.ubc.bps.state.PositionVelocity
import ca.ubc.bps.state.PositionVelocityDependent

public class FlipBounce extends PositionVelocityDependent implements JumpKernel  {
  
  new (List<PositionVelocity> requiredVariables)
  {
    super(requiredVariables);
  }

  override simulate(Random random) {
    var oldVelocity = denseCopy(currentVelocity)
    velocity = (-1.0 * oldVelocity).vectorToArray
  }
  
}