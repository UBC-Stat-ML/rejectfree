package ca.ubc.bps.factory

import ca.ubc.bps.models.Model
import ca.ubc.bps.factory.MonitoredIndices
import ca.ubc.bps.refresh.RefreshmentFactory.Standard
import ca.ubc.bps.bounces.BounceFactory
import ca.ubc.bps.models.FixedPrecisionNormalModel
import ca.ubc.bps.models.DiagonalPrecision
import ca.ubc.bps.state.PiecewiseLinear
import java.util.List
import ca.ubc.bps.models.GeneralizedNormalModel

import ca.ubc.bps.refresh.RefreshmentFactory.NormDependent
import ca.ubc.bps.refresh.RefreshmentFactory.Local
import ca.ubc.bps.factory.InitializationStrategy.Zero
import ca.ubc.bps.factory.InitializationStrategy.Stationary

class BPSFactoryHelpers { 
  
  
  // Refreshments
  
  
  def static Standard global(double globalRate) {
    return new Standard => [
      rate = globalRate
    ]
  }
  
  def static Local local(double globalRate) {
    return new Local => [
      rate = globalRate
    ]
  }

  def static NormDependent normDependent() {
    return new NormDependent
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
  public static final MonitoredIndices all  = MonitoredIndices.all
  public static final MonitoredIndices none = MonitoredIndices.none
  def static MonitoredIndices subset(List<Integer> indices) {
  	return MonitoredIndices.subset(indices) 
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
  
  def static Model generalizedNormal(int dim, double inputAlpha) {
    return new GeneralizedNormalModel => [
      size = dim
      alpha = inputAlpha
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
  
  // Initializations
  
  public static final Zero zero = new Zero
  public static final Stationary stationary = new Stationary
  
}