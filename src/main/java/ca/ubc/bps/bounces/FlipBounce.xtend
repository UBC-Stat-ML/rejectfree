package ca.ubc.bps.bounces

import java.util.List
import ca.ubc.bps.state.ContinuouslyEvolving
import java.util.Random
import ca.ubc.bps.state.ContinuousStateDependent

import static extension xlinear.MatrixExtensions.*
import static xlinear.MatrixOperations.*

import ca.ubc.pdmp.JumpKernel

public class FlipBounce extends ContinuousStateDependent implements JumpKernel  {
  
  new (List<ContinuouslyEvolving> requiredVariables)
  {
    super(requiredVariables);
  }

  override simulate(Random random) {
    var oldVelocity = denseCopy(currentVelocity)
    velocity = (-1.0 * oldVelocity).vectorToArray
  }
  
}