package ca.ubc.pdmp

import static ca.ubc.bps.BPSFactoryHelpers.*
import ca.ubc.bps.BPSFactory
import ca.ubc.bps.BPSFactoryHelpers.NormDependentRefreshment
import ca.ubc.bps.models.FixedPrecisionNormalModel

class NormDepRefTest {
  
  def static public void main(String [] arg) {
    
    val factory = new BPSFactory => [
      refreshment = 
        global(1.0)
//        new NormDependentRefreshment
      stoppingRule = StoppingCriterion.byStochasticProcessTime(100_000)
      model = isotropicGlobal(1000)
      save = subset(#[1])
    ]
    val bps = factory.buildAndRun
    val traj = bps.savedTrajectories.values.iterator.next.trajectory
    println("moment-1 = " + traj.moment(1))
    println("moment-2 = " + traj.moment(2))
    println("ESS = " + traj.momentEss(2))
  }
  
}