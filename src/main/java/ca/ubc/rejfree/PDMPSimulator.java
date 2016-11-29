package ca.ubc.rejfree;

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

import com.google.common.collect.FluentIterable;

import bayonet.graphs.GraphUtils;
import briefj.BriefMaps;
import briefj.collections.UnorderedPair;
import rejfree.local.EventQueue;

/**
 * A simulator for Piecewise Deterministic Poisson Processes (DPDMP. 
 * 
 * See Davis 1993, Markov models and optimization for background on PDMPs.
 * 
 * This implementation is tailored to situations where the rates and jumps 
 * act on sparse subsets of variables, as in the local BPS algorithm.
 * 
 * See Bouchard et al. 2015, The Bouncy particle sampler.
 * 
 * @author bouchard
 *
 */
public class PDMPSimulator
{
  // IDEA: revisit HMC dynamics
  
  // IDEA: can use generator adjoint to verify invariance automatically
  
  public static interface Coordinate
  {
    void extrapolate(double deltaTime);
  }
  
  // not needed by PDMPSimulator
  public static abstract class ContinuouslyEvolving implements Coordinate
  {
    public final MutableDouble position;
    
    // since it is continuously evolving, it necessarily has a velocity
    public final MutableDouble velocity;
    
    public ContinuouslyEvolving(MutableDouble position, MutableDouble velocity)
    {
      super();
      this.position = position;
      this.velocity = velocity;
    }
  }
  
  public static class PiecewiseLinear extends ContinuouslyEvolving
  {
    public PiecewiseLinear(MutableDouble position, MutableDouble velocity)
    {
      super(position, velocity);
    }
    @Override
    public void extrapolate(double deltaTime)
    {
      position.set(position.get() + deltaTime * velocity.get());
    } 
  }
  
  // TODO: HMC
  
  public static class PiecewiseConstant<T> implements Coordinate
  {
    public final MutableObject<T> contents;
    public PiecewiseConstant(MutableObject<T> contents)
    {
      super();
      this.contents = contents;
    }
    @Override
    public void extrapolate(double deltaTime)
    {
      // nothing to do
    }
  }
  
  public static interface MutableDouble
  {
    public void set(double value);
    public double get();
  }
  
  public static interface MutableObject<T>
  {
    public void set(T value);
    public T get();
  }
  
  
  /**
   * A function, distribution or kernel that depends on the current state of the
   * PDMP. 
   * 
   * @author bouchard
   */
  public static interface StateDependent
  {
    /**
     * 
     * @return The variables that need to be up to date to compute the present 
     * distribution, function or kernel.
     */
    List<Coordinate> requiredVariables();

  }
  
  
//  public static interface PoissonProcess extends StateDependent
//  {
//    DeltaTime           nextTime(Random random);
//    double              rate();
//  }
  
  // covers both deterministic and Poisson process jumps
  
  // NB: PoissonProcess will extend EventTimer but only needed outside of this
  
  /**
   * A simulator for the time of the next event. The two main uses are:
   * (1) A Poisson process.
   * (2) A deterministic time to hit a boundary.
   * 
   * @author bouchard
   */
  public static interface EventTimer extends StateDependent
  {
    /**
     * 
     * @param random
     * @return
     */
    DeltaTime next(Random random);
  }
  
  /**
   * 
   * @author bouchard
   */
  public static interface JumpKernel extends StateDependent
  {
    void simulate(Random random);
  }
  
  public static class JumpProcess
  {
    public final EventTimer timer;
    public final JumpKernel kernel;
    public JumpProcess(EventTimer timer, JumpKernel kernel)
    {
      this.timer = timer;
      this.kernel = kernel;
    }
  }
  
  // add 
  public static abstract class StateDependentBase implements StateDependent
  {
    protected final List<Coordinate> requiredVariables;
    
    public StateDependentBase(List<Coordinate> requiredVariables)
    {
      this.requiredVariables = requiredVariables;
    }
  }
  
  public static abstract class ContinuousStateDependent extends StateDependentBase
  {
    private final List<ContinuouslyEvolving> continuousCoordinates;
    private final boolean isPiecewiseLinear;
    
    public ContinuousStateDependent(List<Coordinate> requiredVariables)
    {
      super(requiredVariables);
      this.continuousCoordinates = FluentIterable.from(requiredVariables).filter(ContinuouslyEvolving.class).toList();
      isPiecewiseLinear = isPiecewiseLinear();
    }

    private void extrapolate(double deltaTime)
    {
      for (ContinuouslyEvolving coordinate : continuousCoordinates)
        coordinate.extrapolate(deltaTime);
    }
    
    private boolean isPiecewiseLinear()
    {
      for (ContinuouslyEvolving coordinate : continuousCoordinates)
        if (!(coordinate instanceof PiecewiseLinear))
          return false;
      return true;
    }

    private double [] extrapolateVector(double deltaTime, boolean forPosition)
    {
      double [] result = new double[continuousCoordinates.size()];
      extrapolate(deltaTime);
      for (int i = 0; i < continuousCoordinates.size(); i++)
      {
        final ContinuouslyEvolving z = continuousCoordinates.get(i);
        result[i] = forPosition ? z.position.get() : z.velocity.get();
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
    
    private void set(double [] vector, boolean forPosition)
    {
      if (vector.length != continuousCoordinates.size())
        throw new RuntimeException();
      for (int i = 0; i < vector.length; i++)
      {
        ContinuouslyEvolving coordinate = continuousCoordinates.get(i);
        (forPosition ? coordinate.position : coordinate.velocity).set(vector[i]);
      }
    }
    
    public void setPosition(double [] position)
    {
      set(position, true);
    }
    
    public void setVelocity(double [] velocity)
    {
      set(velocity, false);
    }
  }
  
//  public static class Bounce extends ContinuousStateDependent implements JumpKernel<PositionVelocity>
//  {
//
//    @Override
//    public void simulate(Random random)
//    {
//      //StaticUtils.b
//      // TODO: array-based setters in ContinuousStateDependent
//    }
//    
//  }
  
//  public static interface PositionVelocity
//  {
//    public double position();
//    public double velocity();
//  }
//  
//  public static interface WritablePositionVelocity extends PositionVelocity
//  {
//    public void setPosition(double newPosition);
//    public void setVelocity(double newVelocity);
//  }
  
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
  
//  public static interface DeterministicDynamics<V>
//  {
//    // in place
//    void update(V startState, double deltaTime);
//  }
//  
//  public static class PiecewiseLinearDynamics implements DeterministicDynamics<WritablePositionVelocity>
//  {
//    @Override
//    public void update(WritablePositionVelocity startState, double deltaTime)
//    {
//      startState.setPosition(startState.position() + startState.velocity() * deltaTime);
//    }
//  }
  
  // TODO: HMC dynamics
  
  //  note: some processors are sensitive to the dynamics, 
  //  others are not; but leave this to some builder's discretion
  public static interface Processor extends StateDependent
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
    List<JumpProcess> jumpProcesses;
    List<Coordinate> dynamics;
    List<Processor> processors;
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
        variable2Index.put(pdmp.dynamics.get(variableIndex), variableIndex);
        graph.addVertex(Pair.of(variableIndex, false));
      }
      // create the factor nodes and the edges
      for (int factorIndex = 0; factorIndex < pdmp.jumpProcesses.size(); factorIndex++)
      {
        Pair<Integer,Boolean> currentFactorNode = Pair.of(factorIndex, true);
        graph.addVertex(currentFactorNode);
        JumpProcess jumpProcess = pdmp.jumpProcesses.get(factorIndex);
        // connections are given by union of dependencies of the jump kernel and timer
        for (StateDependent stateDep : new StateDependent[]{jumpProcess.kernel, jumpProcess.timer})
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
    
    // processors: variable -> processors
    Map<Integer,Set<Integer>> processorMappings = new LinkedHashMap<Integer, Set<Integer>>();
    for (int procIdx = 0; procIdx < pdmp.processors.size(); procIdx++)
    {
      Processor currentProc = pdmp.processors.get(procIdx);
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
    coordinate.extrapolate(deltaTime);
    if (commit)
      lastUpdateTime[variableIndex] = time;
  }
  
  private void rollBack(int variableIndex)
  {
    final Coordinate coordinate = pdmp.dynamics.get(variableIndex);
    final double deltaTime = time - lastUpdateTime[variableIndex];
    coordinate.extrapolate(-deltaTime);
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
