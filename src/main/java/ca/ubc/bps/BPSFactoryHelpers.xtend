package ca.ubc.bps

import ca.ubc.bps.BPSFactory.Model
import ca.ubc.bps.BPSFactory.MonitoredIndices
import ca.ubc.bps.RefreshmentFactory.Standard
import ca.ubc.bps.bounces.BounceFactory
import ca.ubc.bps.models.FixedPrecisionNormalModel
import ca.ubc.bps.models.FixedPrecisionNormalModel.DiagonalPrecision
import ca.ubc.bps.state.PiecewiseLinear
import java.util.ArrayList
import java.util.List

class BPSFactoryHelpers {
  
  
  // Refreshments
  
  def static Standard local(double globalRate) {
    return new Standard => [
      local = true
      rate = globalRate
    ]
  }
  def static Standard global(double globalRate) {
    return new Standard => [
      local = false
      rate = globalRate
    ]
  }

  
  // Bounces
  public static BounceFactory standard = new BounceFactory.Standard
  public static final BounceFactory flip = new BounceFactory.Flip
  def static BounceFactory fullyRandomized() {
    return new BounceFactory.Randomized => [
      ignoreIncomingAngle = true
    ]
  } 
  def static BounceFactory dependentRandom() {
    return new BounceFactory.Randomized => [
      ignoreIncomingAngle = false
    ]
  }
  
  // Dynamics
  def static PiecewiseLinear linear() { new PiecewiseLinear }
   
  // Monitors
  public static final MonitoredIndices all = new MonitoredIndices(null)
  public static final MonitoredIndices none = subset(new ArrayList())
  def static MonitoredIndices subset(List<Integer> indices) {
  	return new MonitoredIndices(indices) 
  }
  
  // Models
  def static Model isotropicLocal(int dim) {
    return new FixedPrecisionNormalModel => [
      useLocal = true
      precision = new DiagonalPrecision => [
        size = dim
      ]
    ]
  }
  
  def static Model isotropicGlobal(int dim) {
    return new FixedPrecisionNormalModel => [
      useLocal = false
      precision = new DiagonalPrecision => [
        size = dim
      ]
    ]
  }
  
  
}