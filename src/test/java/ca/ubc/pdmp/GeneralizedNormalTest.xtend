package ca.ubc.pdmp

import ca.ubc.bps.BPSFactory

import static ca.ubc.bps.BPSFactoryHelpers.*
import briefj.BriefCollections
import org.apache.commons.math3.special.Gamma
import org.junit.Test
import org.junit.Assert

class GeneralizedNormalTest {
  
  @Test
  def public void test() {
    
    for (var double alpha = 0.0; alpha < 2.0; alpha += 0.1) {
      println("alpha = " + alpha)
      val curAlpha = alpha
      val factory = new BPSFactory => [
        refreshment = 
//          normDependent
          global(1.0)
        stoppingRule = StoppingCriterion.byStochasticProcessTime(1_000_000)
        model = generalizedNormal(1, curAlpha)
        write = none
        summarize = none
      ]
      val bps = factory.buildAndRun
      val traj = BriefCollections.pick(bps.memorizedTrajectories.values).trajectory
//      println("moment-1 = " + traj.moment(1))
      println("moment-2 = " + traj.moment(2))
//      println("ESS = " + traj.momentEss(2))
      
      println("analytic moment-2 = " + analyticSecondMoment(alpha))
      
      Assert.assertEquals(traj.moment(2), analyticSecondMoment(alpha), 0.03)
      println
    }
  }
  
  /**
   * Closed form for 1D case
   */
  def static double analyticSecondMoment(double alpha) 
  {
    val double beta = 1.0 + alpha
    return gamma(3.0/beta) / gamma(1.0/beta )
  }
  
  def static double gamma(double x)
  {
    return Math.exp(Gamma.logGamma(x))
  }
  
}