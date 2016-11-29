package ca.ubc.rejfree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.UndirectedGraph;

import bayonet.graphs.GraphUtils;
import briefj.BriefCollections;
import briefj.BriefMaps;
import briefj.collections.UnorderedPair;
import rejfree.StaticUtils;
import rejfree.local.EventQueue;

public class PDMPSimulator
{
  // IDEA: revisit HMC dynamics
  
  // IDEA: can use generator adjoint to verify invariance automatically
  

  
  public static class Coordinate<V>
  {
    V variable;
    DeterministicDynamics<V> dynamics;
  }
  
  public static interface StateDependent<V>
  {
    List<V>                requiredVariables();
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
  public static abstract class StateDependentBase<V> implements StateDependent<V>
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
  
  public static class Bounce extends ContinuousStateDependent implements JumpKernel<PositionVelocity>
  {

    @Override
    public void simulate(Random random)
    {
      //StaticUtils.b
      // TODO: array-based setters in ContinuousStateDependent
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
  
  // TODO: HMC dynamics
  
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
  
  public static class PDMP
  {
    List<JumpProcess<?>> jumpProcesses;
    List<Coordinate<?>> dynamics;
    List<Processor<?>> processors;
  }
  
  private final PDMP pdmp;
  
  private static class DependencyGraph
  {
    private final UndirectedGraph<
      Pair<Integer,Boolean>, // nodes take the form (index, isFactor?)
      UnorderedPair<         // vertices are just unordered pairs of nodes
        Pair<Integer,Boolean>, 
        Pair<Integer,Boolean>
      > 
    > graph;
    
    private final IdentityHashMap<Object, Integer> variable2Index;
    
    private DependencyGraph(PDMP pdmp)
    {
      graph = GraphUtils.newUndirectedGraph();
      // index the variables and create variable nodes
      variable2Index = new IdentityHashMap<>();
      for (int variableIndex = 0; variableIndex < pdmp.dynamics.size(); variableIndex++)
      {
        variable2Index.put(pdmp.dynamics.get(variableIndex).variable, variableIndex);
        graph.addVertex(Pair.of(variableIndex, false));
      }
      // create the factor nodes and the edges
      for (int factorIndex = 0; factorIndex < pdmp.jumpProcesses.size(); factorIndex++)
      {
        Pair<Integer,Boolean> currentFactorNode = Pair.of(factorIndex, true);
        graph.addVertex(currentFactorNode);
        JumpProcess<?> jumpProcess = pdmp.jumpProcesses.get(factorIndex);
        // connections are given by union of dependencies of the jump kernel and timer
        for (StateDependent<?> stateDep : new StateDependent[]{jumpProcess.kernel, jumpProcess.timer})
          for (Object connectedVariable : stateDep.requiredVariables())
          {
            int variableIndex = variable2Index.get(connectedVariable);
            Pair<Integer,Boolean> currentVariableNode = Pair.of(variableIndex, false);
            graph.addEdge(currentFactorNode, currentVariableNode);
          }
      }
    }
    
    private Set<Pair<Integer,Boolean>> neighbors(Set<Pair<Integer,Boolean>> set)
    {
      Set<Pair<Integer,Boolean>> result = new LinkedHashSet<>();
      for (Pair<Integer,Boolean> node : set)
        for (UnorderedPair<Pair<Integer,Boolean>,Pair<Integer,Boolean>> edge : graph.edgesOf(node))
          result.add(GraphUtils.pickOther(edge, node));
      return result;
    }
    
  }
  
  private PDMPSimulator(PDMP pdmp) 
  {
    this.pdmp = pdmp; 
    this.numberOfJumpProcesses = pdmp.jumpProcesses.size();
    this.numberOfVariables = pdmp.dynamics.size();
    this.immediateNeighborVariables = new int[numberOfJumpProcesses][];
    this.extendedNeighborVariables = new int[numberOfJumpProcesses][];
    this.neighborFactors = new int[numberOfJumpProcesses][];
    this.processors = new int[numberOfVariables][];
    buildCaches();
  }
  
  private void buildCaches()
  {
    DependencyGraph graph = new DependencyGraph(pdmp);
    
    // some state dependent object may need access to corresponding dynamics
    for (JumpProcess<?> jumpProcess : pdmp.jumpProcesses)
    {
      setDynamics(jumpProcess.kernel, graph.variable2Index);
      setDynamics(jumpProcess.timer, graph.variable2Index);
    }
    for (Processor<?> processor : pdmp.processors)
      setDynamics(processor, graph.variable2Index);
    
    // processors: variable -> processors
    Map<Integer,Set<Integer>> processorMappings = new LinkedHashMap<Integer, Set<Integer>>();
    for (int procIdx = 0; procIdx < pdmp.processors.size(); procIdx++)
    {
      Processor<?> currentProc = pdmp.processors.get(procIdx);
      if (currentProc.requiredVariables().size() != 1)
        throw new RuntimeException("Currently, processors depending on only one variable are " 
            + "supported. \n" 
            + "Other cases can be handled as post-processing without loss of generality.");
      int variableIdx = graph.variable2Index.get(currentProc.requiredVariables().get(0));
      BriefMaps.getOrPutSet(processorMappings, variableIdx).add(procIdx);
    }
    for (int variableIdx = 0; variableIdx < numberOfVariables; variableIdx++)
      processors[variableIdx] = set2Array(processorMappings.get(variableIdx));
    
    // variable and factor hoods
    for (int eventIdx = 0; eventIdx < numberOfJumpProcesses; eventIdx++)
    {
      Set<Pair<Integer, Boolean>> neighbors = graph.neighbors(Collections.singleton(Pair.of(eventIdx, true)));
      immediateNeighborVariables[eventIdx] = set2Array(debox(neighbors));
      Set<Pair<Integer, Boolean>> neighbors2 = graph.neighbors(neighbors);
      neighbors2.remove(Pair.of(eventIdx, true));
      neighborFactors[eventIdx] = set2Array(debox(neighbors2));
      Set<Pair<Integer, Boolean>> neighbors3 = graph.neighbors(neighbors2);
      neighbors3.removeAll(neighbors);
      extendedNeighborVariables[eventIdx] = set2Array(debox(neighbors3));
    }
  }
  
  private List<Integer> debox(Set<Pair<Integer, Boolean>> set)
  {
    return set.stream().map(p -> p.getKey()).collect(Collectors.toList());
  }

  private void setDynamics(StateDependent<?> stateDep, IdentityHashMap<Object, Integer> variable2Index)
  {
    List dynamics = new ArrayList();
    for (Object variable : stateDep.requiredVariables())
      dynamics.add(pdmp.dynamics.get(variable2Index.get(variable)).dynamics);
    stateDep.setDynamics(dynamics);
  }

  private int[] set2Array(Collection<Integer> set)
  {
    if (set.isEmpty())
      return null; // usually bad, but slight optimization used here
    int[] result = new int[set.size()];
    int idx = 0;
    for (int elm : set)
      result[idx++] = elm;
    return null;
  }

  ///// Permanent cachef : 

  private final int numberOfJumpProcesses;
  private final int numberOfVariables;

  // factor -> var
  private final int [][] immediateNeighborVariables;
  
  // factor -> factor
  private final int [][] neighborFactors; // exclude itself
  
  // factor -> var
  private final int [][] extendedNeighborVariables; // excludes immediate 'hood
  
  // variable -> processors
  private final int [][] processors;
  
  ///// Data updated during simulation :
  
  private double               time;
  
  // queue over event sources and their next schedule time
  private EventQueue<Integer>  queue;
  
  // variable -> last updated time
  private double  []           lastUpdateTime; 
  
  // event source -> isBound?
  private boolean []           isBoundIndicators;
  
  private Random random;
  private StoppingCriterion stoppingRule;
  private long startTimeMilliSeconds;
  private int numberOfQueuePolls;
  
  private void init(Random random, StoppingCriterion stoppingRule)
  {
    this.time = 0.0;
    this.queue = new EventQueue<>();
    this.lastUpdateTime = new double[numberOfVariables];
    this.isBoundIndicators = new boolean[numberOfVariables];
    this.stoppingRule = stoppingRule;
    this.random = random;
    this.startTimeMilliSeconds = System.currentTimeMillis();
    this.numberOfQueuePolls = 0;
  }
  
  // TODO: unify notation for factor, jumpProcess, eventSource, et
  
  // TODO: the public version of that will truncate in process time to ensure better numerical properties
  // TODO: test by having a long chain followed by a bunch of short ones; watch moments
  private void simulate(Random random, StoppingCriterion stoppingCriterion)
  {
    init(random, stoppingCriterion);
    
    for (int eventSourceIndex = 0; eventSourceIndex < pdmp.jumpProcesses.size(); eventSourceIndex++)
      simulateNextEventDeltaTime(eventSourceIndex);
    
    while (moreSamplesNeeded())
    {
      // retrieve info about event
      Entry<Double, Integer> event = queue.pollEvent();
      numberOfQueuePolls++;
      time = event.getKey();
      final int eventSourceIndex = event.getValue();
        
      if (isBoundIndicators[eventSourceIndex])  
      {
        // rate hood : update NO commit
        updateVariables(immediateNeighborVariables[eventSourceIndex], false);
        
        // recompute new time
        simulateNextEventDeltaTime(eventSourceIndex);
        
        // undo
        rollBack(eventSourceIndex);
      }
      else
      {
        updateVariables(immediateNeighborVariables[eventSourceIndex], true);
        updateVariables(extendedNeighborVariables[eventSourceIndex], false);
        
        // do the jump
        pdmp.jumpProcesses.get(eventSourceIndex).kernel.simulate(random);
        
        // recompute factor hood new times (including self)
        simulateNextEventDeltaTime(eventSourceIndex);
        simulateNextEventDeltaTimes(neighborFactors[eventSourceIndex]);
        
        // extended hood: undo
        rollBack(extendedNeighborVariables[eventSourceIndex]);
      }
    }
    
    // final update on all variables
    updateAllVariables(); 
  }
  
  private boolean moreSamplesNeeded()
  {
    // Do not need the following, since this is checked in simulateNextEventDeltaTime
    // if (time > stoppingRule.processTime)
    //  return false;
    // instead:
    if (queue.isEmpty())
      return false;
    
    if (System.currentTimeMillis() - startTimeMilliSeconds 
        > stoppingRule.wallClockTimeMilliseconds)
      return false;
    
    if (numberOfQueuePolls > stoppingRule.numberOfEvents)
      return false;
    
    return true;
  }

  private void updateAllVariables()
  {
    for (int varIdx = 0; varIdx < numberOfVariables; varIdx++)
      updateVariable(varIdx, true);
  }
  
  private void updateVariables(int [] variables, boolean commit)
  {
    if (variables == null)
      return;
    
    for (int variableIdx : variables)
      updateVariable(variableIdx, commit);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void updateVariable(int variableIndex, boolean commit)
  {
    if (lastUpdateTime[variableIndex] == time)
      return;
    
    final Coordinate coordinate = pdmp.dynamics.get(variableIndex);
    final double deltaTime = time - lastUpdateTime[variableIndex];
    if (commit)
    {
      int [] processorsForThisVar = processors[variableIndex];
      if (processorsForThisVar != null)
        for (int processorIdx : processorsForThisVar)
          pdmp.processors.get(processorIdx).process(deltaTime);
    }
    coordinate.dynamics.update(coordinate.variable, deltaTime);
    if (commit)
      lastUpdateTime[variableIndex] = time;
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void rollBack(int variableIndex)
  {
    final Coordinate coordinate = pdmp.dynamics.get(variableIndex);
    final double deltaTime = time - lastUpdateTime[variableIndex];
    coordinate.dynamics.update(coordinate.variable, -deltaTime);
  }
  
  private void rollBack(int [] variableIndices)
  {
    if (variableIndices == null)
      return;
    for (int index : variableIndices)
      rollBack(index);
  }
  
  private void simulateNextEventDeltaTimes(int [] eventSourceIndices)
  {
    if (eventSourceIndices == null)
      return;
    for (int index : eventSourceIndices)
      simulateNextEventDeltaTime(index);
  }
  
  private static final double epsilon = 1;
  private void simulateNextEventDeltaTime(int eventSourceIndex)
  {
    DeltaTime nextEvent = pdmp.jumpProcesses.get(eventSourceIndex).timer.next(random);
    double absoluteTime = time + nextEvent.deltaTime;
    if (absoluteTime <= stoppingRule.processTime)
    {
      isBoundIndicators[eventSourceIndex] = nextEvent.isBound;
      absoluteTime = fixNumericalIssue(absoluteTime);
      queue.add(eventSourceIndex, absoluteTime);
    }
  }
  
  private double fixNumericalIssue(double proposedTime)
  {
    if (queue.containsTime(proposedTime))
    {
      System.err.println("The sampler has hit an event of probability zero: "
          + "two collisions scheduled exactly at the same time.");
      System.err.println("Because of numerical precision, this could possibly "
          + "happen, but very rarely.");
      
      System.err.println("For internal implementation reasons, one of the "
          + "collisions at time " + proposedTime + " was moved to " + (proposedTime + epsilon));
      proposedTime += epsilon;
    }
    return proposedTime;
  }
}
