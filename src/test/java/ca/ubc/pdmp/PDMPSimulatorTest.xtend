package ca.ubc.pdmp

import java.util.Collection
import java.util.List
import java.util.Random
import org.junit.Test
import ca.ubc.bps.BPSFactory
import ca.ubc.bps.BPSFactoryHelpers.*

class PDMPSimulatorTest {
  /** 
   * Test sparsity works properly.
   */
  @Test def void testSparsity() {
//    val BPSFactory factory = new BPSFactory => [
//      model = isotropicNormal(10)
//    ]
    
  }

  def static PDMP forceToDense(PDMP pdmp) {
    var List<Coordinate> coordinates = pdmp.coordinates
    var PDMP result = new PDMP(coordinates)
    result.processors.addAll(pdmp.processors)
    for (JumpProcess jumpProcess : pdmp.jumpProcesses) {
      var JumpKernel jumpToGlobal = new JumpKernelWithCustomRequirements(coordinates, jumpProcess.kernel)
      var Clock clockToGlobal = new ClockWithCustomRequirements(coordinates, jumpProcess.clock)
      pdmp.jumpProcesses.add(new JumpProcess(clockToGlobal, jumpToGlobal))
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
