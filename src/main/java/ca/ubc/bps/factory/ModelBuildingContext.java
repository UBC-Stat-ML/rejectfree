package ca.ubc.bps.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.bounces.BounceFactory;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.bps.state.Dynamics;
import ca.ubc.bps.state.MonitoredMutableDouble.ModCount;
import ca.ubc.bps.state.PiecewiseConstant;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.JumpKernel;
import ca.ubc.pdmp.JumpProcess;

public class ModelBuildingContext
{
  public final Random initializationRandom;
  List<JumpProcess> jumpProcesses = new ArrayList<>();
  public List<PositionVelocity> positionVelocityCoordinates = null;
  Set<PositionVelocity> setOfVariables = null;
  LinkedHashSet<PiecewiseConstant<?>> piecewiseConstantStates = new LinkedHashSet<>();
  public ModCount modCount = new ModCount();
  
  private final Dynamics dynamics;
  private final BounceFactory bounce;
  
  public ModelBuildingContext(Random initializationRandom, Dynamics dynamics, BounceFactory bounce)
  {
    this.initializationRandom = initializationRandom;
    this.dynamics = dynamics;
    this.bounce = bounce;
  }
  public Dynamics dynamics()
  {
    return dynamics;
  }
  public List<PositionVelocity> buildAndRegisterPositionVelocityCoordinates(int dim) 
  {
    if (positionVelocityCoordinates != null)
      throw new RuntimeException();
    positionVelocityCoordinates = PositionVelocity.buildArray(dim, dynamics, modCount);
    setOfVariables = new HashSet<>(positionVelocityCoordinates);
    return positionVelocityCoordinates;
  }
  public void registerBPSPotential(BPSPotential potential)
  {
    // find which variables bounce: those that are continuously evolving
    List<? extends Coordinate> allReqVars = new ArrayList<>(potential.clock.requiredVariables());
    List<PositionVelocity> continuousCoordinates = BPSStaticUtils.continuousCoordinates(allReqVars);
    // check they form the prefix (to make correspondence with energy indices straightforward)
    if (!continuousCoordinates.equals(allReqVars.subList(0, continuousCoordinates.size())))
      throw new RuntimeException();
    JumpKernel kernel = bounce.build(continuousCoordinates, potential.energy);
    JumpProcess process = new JumpProcess(potential.clock, kernel);
    registerJumpProcess(process);
  }
  List<Coordinate> coordinates() 
  {
    List<Coordinate> result = new ArrayList<>();
    result.addAll(positionVelocityCoordinates);
    result.addAll(piecewiseConstantStates);
    return result;
  }
  public void registerJumpProcess(JumpProcess process)
  {
    registerVariables(process.clock.requiredVariables());
    registerVariables(process.kernel.requiredVariables());
    jumpProcesses.add(process);
  }
  private void registerVariables(Collection<? extends Coordinate> vars)
  {
    for (Coordinate c : vars)
    {
      if (c instanceof PositionVelocity && !setOfVariables.contains(c))
          throw new RuntimeException();
      if (c instanceof PiecewiseConstant)
        piecewiseConstantStates.add((PiecewiseConstant<?>) c);
    }
  }
}