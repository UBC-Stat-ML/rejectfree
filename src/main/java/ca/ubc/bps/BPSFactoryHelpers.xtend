package ca.ubc.bps

import ca.ubc.bps.state.ContinuouslyEvolving
import ca.ubc.bps.bounces.Bounce
import ca.ubc.bps.energies.EnergyGradient
import java.util.List
import ca.ubc.bps.bounces.RandomizedBounce
import ca.ubc.bps.bounces.FlipBounce
import ca.ubc.bps.bounces.BounceFactory
import ca.ubc.bps.state.PiecewiseLinear
import ca.ubc.bps.BPSFactory.MonitoredIndices
import java.util.ArrayList
import ca.ubc.bps.BPSFactory.Model
import ca.ubc.bps.models.FixedPrecisionNormalModel
import ca.ubc.bps.models.FixedPrecisionNormalModel.DiagonalPrecision
import ca.ubc.pdmp.PDMP
import blang.inits.Arg
import ca.ubc.bps.BPSFactory.RefreshmentFactory
import blang.inits.DefaultValue
import static ca.ubc.bps.StaticUtils.*
import ca.ubc.pdmp.JumpProcess
import ca.ubc.bps.kernels.IndependentRefreshment
import ca.ubc.bps.timers.ConvexTimer
import java.util.function.Function

import static xlinear.MatrixOperations.*
import static extension xlinear.MatrixExtensions.*

class BPSFactoryHelpers {
  
  
  // Refreshments
  
  def static StandardRefreshment local(double rate) {
    return new StandardRefreshment => [
      useLocal = true
      globalRate = rate
    ]
  }
  def static StandardRefreshment global(double rate) {
    return new StandardRefreshment => [
      useLocal = false
      globalRate = rate
    ]
  }

  
  // Bounces
  public static BounceFactory standard = [List<ContinuouslyEvolving> variables, EnergyGradient energy | new Bounce(variables,energy)]
  public static final BounceFactory flip = [List<ContinuouslyEvolving> variables, EnergyGradient energy | new FlipBounce(variables,energy)]
  public static final BounceFactory fullyRandomized = [List<ContinuouslyEvolving> variables, EnergyGradient energy | new RandomizedBounce(variables,energy,true)]
  public static final BounceFactory dependentRandomized = [List<ContinuouslyEvolving> variables, EnergyGradient energy | new RandomizedBounce(variables,energy,false)]
  
  
  // Dynamics
  public static final PiecewiseLinear linear = PiecewiseLinear.instance
   
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
  
  static class StandardRefreshment implements RefreshmentFactory {
	  @Arg @DefaultValue("1.0")
	  public double globalRate = 1.0
	
	  @Arg @DefaultValue("true")
	  public boolean useLocal = true
	  
  	override void addRefreshment(PDMP pdmp) {
  		if (useLocal) 
  		  Refreshments.addLocal(pdmp, globalRate / ((continuousCoordinates(pdmp.coordinates).size() as double)))  
  		else 
  		  Refreshments.addGlobal(pdmp, globalRate) 
  	}
  }
  
  static class NormDependentRefreshment implements RefreshmentFactory {
    
    @Arg @DefaultValue("0.5")
    public double power = 0.5
    
    def private Function<double[],Double> normPotential() {
      return [double [] input |
        denseCopy(input).norm ** power
      ]
    }
    
    override void addRefreshment(PDMP pdmp) {
      val List<ContinuouslyEvolving> continuousCoordinates = continuousCoordinates(pdmp.coordinates)
      pdmp.jumpProcesses.add(
        new JumpProcess(
          new ConvexTimer(
            continuousCoordinates,
            normPotential,
            1.0
          ),
          new IndependentRefreshment(continuousCoordinates)
        )
      )
    }
  }
}