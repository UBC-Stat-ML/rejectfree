package ca.ubc.pdmp

import java.util.Collection
import java.util.List
import java.util.Random
import org.junit.Test
import ca.ubc.bps.BPSFactory
import static ca.ubc.bps.BPSFactoryHelpers.*
import ca.ubc.bps.BPSFactory.BPS

class PDMPSimulatorTest {
  /** 
   * Test sparsity works properly.
   */
  @Test def void testSparsity() {
    val BPSFactory factory = new BPSFactory => [
      model = isotropicLocal(10)
      forbidOutputFiles = true
      stoppingRule = StoppingCriterion.byStochasticProcessTime(0.5)
    ]
    val BPS bpsSparse = run(factory, true)
    val BPS bpsDense  = run(factory, false)
    
    println(bpsDense. continuouslyEvolvingStates.get(0).position.get)
    println(bpsSparse.continuouslyEvolvingStates.get(0).position.get)
  }
  
  def static BPS run(BPSFactory factory, boolean sparse) {
    val BPS bps = factory.buildBPS
    var PDMP pdmp = bps.PDMP
    if (sparse) {
      pdmp = forceToDense(pdmp)
    }
    val PDMPSimulator simulator = new PDMPSimulator(pdmp);
    simulator.simulate(new Random(factory.simulationRandom), factory.stoppingRule);
    return bps
  }

  def static PDMP forceToDense(PDMP pdmp) {
    var List<Coordinate> coordinates = pdmp.coordinates
    var PDMP result = new PDMP(coordinates)
    result.processors.addAll(pdmp.processors)
    for (JumpProcess jumpProcess : pdmp.jumpProcesses) {
      var JumpKernel jumpToGlobal = new JumpKernelWithCustomRequirements(coordinates, jumpProcess.kernel)
      var Clock clockToGlobal = new ClockWithCustomRequirements(coordinates, jumpProcess.clock)
      result.jumpProcesses.add(new JumpProcess(clockToGlobal, jumpToGlobal))
    }
    return result
  }

  private static class JumpKernelWithCustomRequirements implements JumpKernel {
    final Collection<? extends Coordinate> requiredVariables
    final JumpKernel kernel

    new(Collection<? extends Coordinate> requiredVariables, JumpKernel kernel) {
      this.requiredVariables = requiredVariables
      this.kernel = kernel
    }

    override Collection<? extends Coordinate> requiredVariables() {
      return requiredVariables
    }

    override void simulate(Random random) {
      kernel.simulate(random)
    }
  }

  private static class ClockWithCustomRequirements implements Clock {
    final Collection<? extends Coordinate> requiredVariables
    final Clock clock

    new(Collection<? extends Coordinate> requiredVariables, Clock clock) {
      this.requiredVariables = requiredVariables
      this.clock = clock
    }

    override DeltaTime next(Random random) {
      return clock.next(random)
    }

    override Collection<? extends Coordinate> requiredVariables() {
      return requiredVariables
    }
  }
}
