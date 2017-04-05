package ca.ubc.pdmp

import java.util.Collection
import java.util.List
import java.util.Random
import org.junit.Test
import ca.ubc.bps.factory.BPSFactory
import static ca.ubc.bps.factory.BPSFactoryHelpers.*
import ca.ubc.bps.factory.BPSFactory.BPS
import ca.ubc.bps.processors.Trajectory

// TODO: make this into bps-test-densify

class PDMPSimulatorTest {
  /** 
   * Test sparsity works properly.
   */
  @Test def void testSparsity() {
    val BPSFactory factory = new BPSFactory => [
      model = isotropicLocal(10)
      forbidOutputFiles = true
      stoppingRule = StoppingCriterion.byStochasticProcessTime(1_000_000)
    ]
    println("Running sparse")
    val BPS bpsSparse = run(factory, false)
    println("---")
    println("Running dense")
    val BPS bpsDense  = run(factory, true)
    println("---")
    val Trajectory sparseTraj = bpsSparse.memorizedTrajectories.get(bpsSparse.continuouslyEvolvingStates.get(0)).trajectory
    val Trajectory denseTraj =  bpsDense. memorizedTrajectories.get(bpsDense. continuouslyEvolvingStates.get(0)).trajectory
    val List<Trajectory> trajectories = #[sparseTraj, denseTraj]
    for (var int degree = 1; degree < 5; degree++) {
      for (Trajectory traj : trajectories) {
        println(traj.moment(degree))
      }
      println("--")
    }
    
//    println(bpsDense. continuouslyEvolvingStates.get(0).position.get)
//    println(bpsSparse.continuouslyEvolvingStates.get(0).position.get)
  }
  
  def static BPS run(BPSFactory factory, boolean forceToDense) {
    val BPS bps = factory.buildBPS
    val PDMP pdmp = instrument(bps.PDMP, forceToDense)
    val PDMPSimulator simulator = new PDMPSimulator(pdmp);
    simulator.simulate(new Random(factory.simulationRandom), factory.stoppingRule);
    return bps
  }

  def static PDMP instrument(PDMP pdmp, boolean forceToDense) {
    val List<Coordinate> coordinates = pdmp.coordinates
    val PDMP result = new PDMP(coordinates)
    result.processors.addAll(pdmp.processors)
    for (JumpProcess jumpProcess : pdmp.jumpProcesses) {
      val JumpKernel jumpToGlobal = 
        if (forceToDense) {
          new InstrumentedJumpKernel(coordinates, jumpProcess.kernel)
        } else {
          new InstrumentedJumpKernel(jumpProcess.kernel)
        }
      val Clock clockToGlobal = 
        if (forceToDense) {
          new InstrumentedClock(coordinates, jumpProcess.clock)
        } else {
          new InstrumentedClock(jumpProcess.clock)
        }
      result.jumpProcesses.add(new JumpProcess(clockToGlobal, jumpToGlobal))
    }
    return result
  }
  
  public var static boolean printInstrumented = false

  private static class InstrumentedJumpKernel implements JumpKernel {
    final Collection<? extends Coordinate> requiredVariables
    final JumpKernel kernel
    new(Collection<? extends Coordinate> requiredVariables, JumpKernel kernel) {
      this.requiredVariables = requiredVariables
      this.kernel = kernel
    }
    
    new(JumpKernel kernel) {
      this(kernel.requiredVariables, kernel)
    }

    override Collection<? extends Coordinate> requiredVariables() {
      return requiredVariables
    }

    override void simulate(Random random) {
      if (printInstrumented) {
        println("Jump triggered: " + kernel.class.simpleName) 
      }
      kernel.simulate(random)
    }
  }

  private static class InstrumentedClock implements Clock {
    final Collection<? extends Coordinate> requiredVariables
    final Clock clock

    new(Collection<? extends Coordinate> requiredVariables, Clock clock) {
      this.requiredVariables = requiredVariables
      this.clock = clock
    }
    
    new(Clock clock) {
      this(clock.requiredVariables, clock)
    }

    override DeltaTime next(Random random) {
      val DeltaTime result = clock.next(random)
      if (printInstrumented) {
        println("Clock triggered: " + clock.class.simpleName + " (" + result + ")") 
      }
      return result
    }

    override Collection<? extends Coordinate> requiredVariables() {
      return requiredVariables
    }
  }
}
