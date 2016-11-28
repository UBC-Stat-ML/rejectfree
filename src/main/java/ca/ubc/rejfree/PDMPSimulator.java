package ca.ubc.rejfree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;


import rejfree.local.EventQueue;

public class PDMPSimulator
{
  // IDEA: revisit HMC dynamics
  
  // IDEA: can use generator adjoint to verify invariance automatically
  
  public static class PDMP
  {
    List<JumpProcess<?>> jumpProcesses;
    List<Coordinate<?>> dynamics;
    List<Processor<?>> processors;
  }
  
  public static class Coordinate<V>
  {
    V variable;
    DeterministicDynamics<V> dynamics;
  }
  
  public static interface StateDependent<V>
  {
    List<V>                requiredVariables();
  }
  
  public static interface DynamicsAware<V>
  {
    void                   setDynamics(List<DeterministicDynamics<V>> dynamics);
  }
  
//  public static interface PoissonProcess extends StateDependent
//  {
//    DeltaTime           nextTime(Random random);
//    double              rate();
//  }
  
  // covers both deterministic and Poisson process jumps
  
  // NB: PoissonProcess will extend EventTimer but only needed outside of this
  
  public static interface EventTimer<V> extends StateDependent<V>
  {
    DeltaTime           next(Random random);
  }
  
  public static interface JumpKernel<V> extends StateDependent<V>
  {
    void                simulate(Random random);
  }
  
  public static class JumpProcess<V> // jump process?
  {
    private final EventTimer<V> timer;
    private final JumpKernel<V> kernel;
  }
  
  // add 
  public static abstract class StateDependentBase<V> implements StateDependent<V>, DynamicsAware<V>
  {
    protected final List<V> requiredVariables;
    protected List<DeterministicDynamics<V>> dynamics; // this gets filled in automatically by the PDMP simulator
    
    public StateDependentBase(List<V> requiredVariables)
    {
      this.requiredVariables = requiredVariables;
    }
    
    public void setDynamics(List<DeterministicDynamics<V>> dynamics)
    {
      this.dynamics = dynamics;
    }
    
    public List<V> requiredVariables()
    {
      return requiredVariables;
    }
  }
  
  // NB: gradient is something orthogonal, based on some gradient calculator
  public static abstract class ContinuousStateDependent extends StateDependentBase<PositionVelocity>
  {
    public ContinuousStateDependent(List<PositionVelocity> requiredVariables)
    {
      super(requiredVariables);
    }

    private Boolean isPiecewiseLinear = null;
    private void extrapolate(double deltaTime)
    {
      for (int i = 0; i < requiredVariables.size(); i++)
        dynamics.get(i).update(requiredVariables.get(i), deltaTime);
      if (isPiecewiseLinear == null)
        isPiecewiseLinear = checkIfPiecewiseLinear();
    }
 
    private boolean checkIfPiecewiseLinear()
    {
      for (DeterministicDynamics<?> dyn : dynamics)
        if (!(dyn instanceof PiecewiseLinearDynamics))
          return false;
      return true;
    }

    private double [] extrapolateVector(double deltaTime, boolean forPosition)
    {
      double [] result = new double[requiredVariables.size()];
      extrapolate(deltaTime);
      for (int i = 0; i < requiredVariables.size(); i++)
      {
        final PositionVelocity z = requiredVariables.get(i);
        result[i] = forPosition ? z.position() : z.velocity();
      }
      extrapolate( - deltaTime);
      return result;
    }
    
    public double [] currentVelocity()
    {
      return extrapolateVector(0.0, false);
    }
    
    public double [] extrapolateVelocity(double deltaTime)
    {
      return isPiecewiseLinear ? currentVelocity() : extrapolateVector(deltaTime, false);
    }
    
    public double [] currentPosition()
    {
      return extrapolateVector(0.0, true);
    }
    
    public double [] extrapolatePosition(double deltaTime)
    {
      return extrapolateVector(deltaTime, true);
    }
  }
  
  public static interface PositionVelocity
  {
    public double position();
    public double velocity();
  }
  
  public static interface WritablePositionVelocity extends PositionVelocity
  {
    public void setPosition(double newPosition);
    public void setVelocity(double newVelocity);
  }
  
  // version with no extrapolator
  
  // version with all continuous, augmented variables + gradient

  
  
//  public static class Extrapolator
//  {
//    private boolean active = false;
//    private double sumDeltaTimes = 0.0;
//    
////    public double peekRate(double delta)
////    {
////      // TODO
////    }
//    
//    public void extrapolate(double deltaTime)
//    {
//      sumDeltaTimes += deltaTime;
//      // TODO
//    }
//    
//  }
  
  // impls:
  
  // - thin: need interface for direction derivative at
  // - constrained
  
  // - 
  
  // danger: update should only come from PDMP class
  // TODO: to avoid issues, lets make this qbstract, protected and implement it here
//  public static interface Variable
//  {
//    protected void updateVariable(double deltaTime, boolean process);
//  }
  
  public static interface DeterministicDynamics<V>
  {
    // in place
    void update(V startState, double deltaTime);
  }
  
  public static class PiecewiseLinearDynamics implements DeterministicDynamics<WritablePositionVelocity>
  {
    @Override
    public void update(WritablePositionVelocity startState, double deltaTime)
    {
      startState.setPosition(startState.position() + startState.velocity() * deltaTime);
    }
  }
  
  
  
  //  note: some processors are sensitive to the dynamics, 
  //  others are not; but leave this to some builder's discretion
  public static interface Processor<V> extends StateDependent<V>
  {
    void process(double deltaTime);
  }
  
  // save: no need to know details
  
  // moments: do need
  
  // list of triplets (V, DeterministicDyn, Processors)

  
//  public static abstract class PositionVelocityPair implements Variable
//  {
//    // keep update un-implemented
//  }
  
//  public static final List<Integer> ALL = new ArrayList<>(); 
  
  public static class DeltaTime
  {
    public final double deltaTime;
    public final boolean isBound;
    
    public DeltaTime(double deltaTime, boolean isBound)
    {
      this.deltaTime = deltaTime;
      this.isBound = isBound;
    }

    public static DeltaTime isEqualTo(double time)
    {
      return new DeltaTime(time, false);
    }
    
    public static DeltaTime isGreaterThan(double time)
    {
      return new DeltaTime(time, true);
    }
  }
  
  public static class StoppingCriterion
  {
    public double processTime = Double.POSITIVE_INFINITY;
    public long wallClockTimeMilliseconds = Long.MAX_VALUE;
    public long numberOfEvents = Long.MAX_VALUE;
  }
  
  private final PDMP process;
  
  private PDMPSimulator(PDMP process) 
  {
    this.process = process; 
    this.numberOfEventSources = process.eventSources.size();
    this.immediateNeighborVariables = new int[numberOfEventSources][];
    this.extendedNeighborVariables = new int[numberOfEventSources][];
    this.neighborFactors = new int[numberOfEventSources][];
    // TODO: init those
    
    // also, need to 
  }
  
  private final int numberOfEventSources;
  private final int numberOfVariables;

  private final int [][] immediateNeighborVariables;
  private final int [][] neighborFactors; // ***** exclude itself
  private final int [][] extendedNeighborVariables;
  
  // encapsulate that stuff?
  private double               time;
  private EventQueue<Integer>  queue;
  private double  []           lastUpdateTime; 
  private boolean []           isBoundIndicators;
  
  private void init()
  {
    time = 0.0;
    queue = new EventQueue<>();
    lastUpdateTime = new double[numberOfVariables];
    isBoundIndicators = new boolean[numberOfVariables];
  }
  
  // TODO: the public version of that will truncate in process time to ensure better numerical properties
  // TODO: test by having a long chain followed by a bunch of short ones; watch moments
  private void simulate(Random random, StoppingCriterion stoppingCriterion)
  {
    init();
    
    for (int eventSourceIndex = 0; eventSourceIndex < process.numberOfEventSources(); eventSourceIndex++)
      simulateNextEventDeltaTime(eventSourceIndex);
    
    while (moreSamplesNeeded())
    {
      // retreive info about event
      Entry<Double, Integer> event = queue.pollEvent(); 
      time = event.getKey();
      int eventSourceIndex = event.getValue();
      boolean isBound = isBoundIndicators[eventSourceIndex];
      
      // update variables
      int [] variablesToUpdate = 
        isBound ? immediateNeighborVariables[eventSourceIndex] 
                : extendedNeighborVariables[eventSourceIndex];
      for (int variableIndex : variablesToUpdate)
        updateVariable(variableIndex);
      
      // simulate event
      process.simulateEvent(random, eventSourceIndex);
      
      // update event times
      simulateNextEventDeltaTime(eventSourceIndex);
      if (!isBound)
        for (int neighborFactor : neighborFactors[eventSourceIndex])
          simulateNextEventDeltaTime(neighborFactor);
    }
    
    // final update on variables
    for (int variableIndex = 0; variableIndex < process.numberOfVariables(); variableIndex++)
      if (lastUpdateTime[variableIndex] != time)
        updateVariable(variableIndex);
    
  }

  private void updateVariable(int variableIndex)
  {
    process.extrapolateVariable(variableIndex, time - lastUpdateTime[variableIndex]);
    lastUpdateTime[variableIndex] = time;
  }
  
  private static final double epsilon = 1;
  private void simulateNextEventDeltaTime(int eventSourceIndex)
  {
    DeltaTime nextEvent = process.simulateNextEventDeltaTime(random, eventSourceIndex);
    double absoluteTime = time + nextEvent.deltaTime;
    if (absoluteTime <= stoppingCriterion.processTime)
    {
      isBoundIndicators[eventSourceIndex] = nextEvent.isBound;
      if (queue.containsTime(absoluteTime))
      {
        System.err.println("The sampler has hit an event of probability zero: "
            + "two collisions scheduled exactly at the same time.");
        System.err.println("Because of numerical precision, this could possibly "
            + "happen, but very rarely.");
        
        System.err.println("For internal implementation reasons, one of the "
            + "collisions at time " + absoluteTime + " was moved to " + (absoluteTime + epsilon));
        absoluteTime += epsilon;
      }
      queue.add(eventSourceIndex, absoluteTime);
    }
  }
}
