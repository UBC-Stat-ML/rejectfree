package ca.ubc.pdmp

import static ca.ubc.bps.factory.BPSFactoryHelpers.*
import ca.ubc.bps.factory.BPSFactory
import briefj.BriefCollections

class NormDepRefTest {
  
  def static public void main(String [] arg) {
    
    val factory = new BPSFactory => [
      refreshment = normDependent
      stoppingRule = StoppingCriterion.byStochasticProcessTime(10_000)
      model = isotropicGlobal(100)
      write = none
      summarize = none
    ]
    val bps = factory.buildAndRun
    val traj = BriefCollections.pick(bps.memorizedTrajectories.values).trajectory
    println("moment-1 = " + traj.moment(1))
    println("moment-2 = " + traj.moment(2))
    println("ESS = " + traj.momentEss(2))
  }
  
}