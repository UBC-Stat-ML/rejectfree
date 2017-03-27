package ca.ubc.pdmp;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import briefj.BriefMaps;
import rejfree.local.EventQueue;

/**
 * A simulator for Piecewise Deterministic Poisson Processes (DPMP). 
 * 
 * See Davis 1993, Markov models and optimization for background on PDMPs.
 * 
 * This implementation is tailored to situations where the rates and jumps 
 * act on sparse subsets of variables, as in the local BPS algorithm.
 * 
 * See Bouchard, Vollmer, Doucet 2015, The Bouncy particle sampler.
 * 
 * @author bouchard
 *
 */
public class PDMPSimulator
{
  private final PDMP pdmp;
   
  public PDMPSimulator(PDMP pdmp) 
  {
    this.pdmp = pdmp; 
    this.numberOfJumpProcesses = pdmp.jumpProcesses.size();
    this.numberOfVariables = pdmp.coordinates.size();
    this.nd = new int[numberOfJumpProcesses][];
    this.nk = new int[numberOfJumpProcesses][];
    this.Nd_nk_plus_id = new int[numberOfJumpProcesses][];
    this.nd_Nd_nk_plus_nd_minus_nk = new int[numberOfJumpProcesses][];
    this.processors = new int[numberOfVariables][];
    buildCaches();
  }
  
  
  ///// Permanent caches : 

  private final int numberOfJumpProcesses;
  private final int numberOfVariables;
  
  /*
   * Conventions for variables with pattern {n,N}{d,k}:
   * 
   * prefix n : neighbors (obtained via StateDependent.requiredVariables())
   *            takes as input JumpProcess indices
   * prefix N : inverse neighbors (i.e. set of timers/kernels that depend on given variable index)
   *            takes as input variable indices
   * 
   * suffix k : related to jump *k*ernels
   * suffix d : related to timers (which provide *d*elta times)
   * 
   * For example, Nd_nk[j]: 
   *   for given JumpProcess index j, this returns Nd(nk(j)), 
   *   a list of variables.
   */

  // JumpProcess -> Coordinate
  private final int [][] nd, nk;
  
  // JumpProcess -> JumpProcess
  private final int [][] Nd_nk_plus_id;
  
  // JumpProcess -> Coordinate
  private final int [][] nd_Nd_nk_plus_nd_minus_nk; 
  
  // Coordinate -> Processors
  private final int [][] processors;
  
  
  ///// Data updated during simulation :
  
  private double               time;
  
  // queue over jump processes and their next schedule time
  private EventQueue<Integer>  queue;
  
  // variable -> last updated time
  private double  []           lastUpdateTimes; 
  
  // jump processes -> isBound?
  private boolean []           isBoundIndicators;
  
  private Random               random;
  private StoppingCriterion    stoppingRule;
  private long                 numberOfQueuePolls, 
                               numberOfJumps;  // number of jumps
  private long                 startTimeMilliSeconds;
  private double               totalProcessTime;
  
  private double               maxTrajectoryLengthPerChunk = 10_000;
  private boolean              printSummaryStatistics = true;
  
  private void init()
  {
    this.time = 0.0;
    this.queue = new EventQueue<>();
    this.lastUpdateTimes = new double[numberOfVariables];
    this.isBoundIndicators = new boolean[numberOfJumpProcesses];
  }
  
  public void simulate(Random random, StoppingCriterion inputStoppingRule)
  {
    this.totalProcessTime = 0.0;
    this.numberOfQueuePolls = 0;
    this.numberOfJumps = 0;
    this.startTimeMilliSeconds = System.currentTimeMillis();
    this.random = random;
    
    loop:while (inputStoppingRule.stochasticProcessTime - totalProcessTime > 0)
    {
      double processIncrementTime = 
         Math.min(
             maxTrajectoryLengthPerChunk, 
             inputStoppingRule.stochasticProcessTime - totalProcessTime);
      this.stoppingRule = new StoppingCriterion(
          processIncrementTime,
          inputStoppingRule.wallClockTimeMilliseconds,
          inputStoppingRule.numberOfQueuePolls
          );
      simulateChunk();
      totalProcessTime += processIncrementTime;
      
      if (!computeBudgetPositive())
        break loop;
    }
    
    printSummaryStatistics();
  }
  
  private void simulateChunk()
  {
    init();
    
    for (int jumpProcessIndex = 0; jumpProcessIndex < pdmp.jumpProcesses.size(); jumpProcessIndex++)
      simulateNextEventDeltaTime(jumpProcessIndex);
    
    while (computeBudgetPositive() && !queue.isEmpty())
    {
      // retrieve info about event
      final Entry<Double, Integer> event = queue.pollEvent();
      numberOfQueuePolls++;

      time = event.getKey();
      final int eventJumpProcessIndex = event.getValue();
      
      if (isBoundIndicators[eventJumpProcessIndex])  
      {
        updateVariables(nd[eventJumpProcessIndex], false, -1);
        
        // recompute new time
        simulateNextEventDeltaTime(eventJumpProcessIndex);
        
        // undo
        rollBack(nd[eventJumpProcessIndex]);
      }
      else
      {
        numberOfJumps++;
        updateVariables(nk[eventJumpProcessIndex], true, eventJumpProcessIndex);
        updateVariables(nd_Nd_nk_plus_nd_minus_nk[eventJumpProcessIndex], false, -1);
        
        // do the jump
        pdmp.jumpProcesses.get(eventJumpProcessIndex).kernel.simulate(random);
        
        // recompute factor 'hood new times (including self) 
        simulateNextEventDeltaTimes(Nd_nk_plus_id[eventJumpProcessIndex]);
        
        // extended 'hood: undo
        rollBack(nd_Nd_nk_plus_nd_minus_nk[eventJumpProcessIndex]);
      }
    }
    
    // final update on all variables
    updateAllVariables(true, -1); 
  }
  
  private boolean computeBudgetPositive()
  {
    if (System.currentTimeMillis() - startTimeMilliSeconds 
        > stoppingRule.wallClockTimeMilliseconds)
      return false;
    
    if (numberOfQueuePolls > stoppingRule.numberOfQueuePolls)
      return false;
    
    return true;
  }

  private void updateAllVariables(boolean commit, int source)
  {
    for (int varIdx = 0; varIdx < numberOfVariables; varIdx++)
      _updateVariable(varIdx, commit, source);
  }
  
  private void updateVariables(int [] variables, boolean commit, int source)
  {
    if (variables == null)
      return;
    
    if (variables[0] == -1)
      updateAllVariables(commit, source);
    else
      for (int variableIdx : variables)
        _updateVariable(variableIdx, commit, source);
  }

  private void _updateVariable(int variableIndex, boolean commit, int source)
  {
    // avoid extraneous processing calls
    if (lastUpdateTimes[variableIndex] == time)
      return;
    
    final Coordinate coordinate = pdmp.coordinates.get(variableIndex);
    final double deltaTime = time - lastUpdateTimes[variableIndex];
    if (commit)
    {
      final int [] processorsForThisVar = processors[variableIndex];
      if (processorsForThisVar != null)
        for (int processorIdx : processorsForThisVar)
          pdmp.processors.get(processorIdx).process(deltaTime, source);
      lastUpdateTimes[variableIndex] = time;
    }
    coordinate.extrapolateInPlace(deltaTime);
  }
  
  private void _rollBack(int variableIndex)
  {
    final Coordinate coordinate = pdmp.coordinates.get(variableIndex);
    final double deltaTime = time - lastUpdateTimes[variableIndex];
    coordinate.extrapolateInPlace(-deltaTime);
  }
  
  private void rollBack(int [] variableIndices)
  {
    if (variableIndices == null)
      return;
    if (variableIndices[0] == -1)
      for (int i = 0; i < numberOfVariables; i++)
        _rollBack(i);
    else
      for (int index : variableIndices)
        _rollBack(index);
  }
  
  private void simulateNextEventDeltaTimes(int [] jumpProcessIndices)
  {
    if (jumpProcessIndices == null)
      return;
    if (jumpProcessIndices[0] == -1)
      for (int i = 0; i < numberOfJumpProcesses; i++)
        simulateNextEventDeltaTime(i);
    else
      for (int index : jumpProcessIndices)
        simulateNextEventDeltaTime(index);
  }
  
  private void simulateNextEventDeltaTime(int jumpProcessIndex)
  {
    queue.remove(jumpProcessIndex);
    final DeltaTime nextEvent = pdmp.jumpProcesses.get(jumpProcessIndex).clock.next(random);
    double absoluteTime = time + nextEvent.deltaTime;
    if (absoluteTime <= stoppingRule.stochasticProcessTime)
    {
      isBoundIndicators[jumpProcessIndex] = nextEvent.isBound;
      absoluteTime = fixNumericalIssue(absoluteTime);
      queue.add(jumpProcessIndex, absoluteTime);
    }
  }
  
  //// Some low-level details
  
  private static final double epsilon = 1;
  private double fixNumericalIssue(double proposedTime)
  {
    if (queue.containsTime(proposedTime))
    {
      System.err.println("The sampler has hit an event of probability zero: "
          + "two events scheduled exactly at the same time.");
      System.err.println("Because of numerical precision, this could possibly "
          + "happen, but very rarely.");
      
      System.err.println("For internal implementation reasons, one of the "
          + "events at time " + proposedTime + " was moved to " + (proposedTime + epsilon));
      proposedTime += epsilon;
    }
    return proposedTime;
  }
  
  private void buildCaches()
  {
    Dependencies deps = new Dependencies(pdmp);
    
    // processors: variable -> processors
    Map<Integer,Set<Integer>> processorMappings = new LinkedHashMap<Integer, Set<Integer>>();
    for (int procIdx = 0; procIdx < pdmp.processors.size(); procIdx++)
    {
      Processor currentProc = pdmp.processors.get(procIdx);
      if (currentProc.requiredVariables().size() != 1)
        throw new RuntimeException("Currently, processors depending on only one variable are " 
            + "supported. \n" 
            + "Other cases can be handled as post-processing without loss of generality.");
      int variableIdx = deps.variable2Index.get(currentProc.requiredVariables().iterator().next());
      BriefMaps.getOrPutSet(processorMappings, variableIdx).add(procIdx);
    }
    for (int variableIdx = 0; variableIdx < numberOfVariables; variableIdx++)
      processors[variableIdx] = deps.convert(processorMappings.get(variableIdx), Integer.MAX_VALUE);
    
    // variable and factor neighborhoods
    for (int jumpProcessIdx = 0; jumpProcessIdx < numberOfJumpProcesses; jumpProcessIdx++)
    {
      final Set<Integer> _nd = deps.nd(jumpProcessIdx);
      final Set<Integer> _nk = deps.nk(jumpProcessIdx);
      final Set<Integer> _Nd_nk = deps.Nd(_nk);
      final Set<Integer> _nd_Nd_nk = deps.nd(_Nd_nk);
      _nd_Nd_nk.addAll(_nd);
      _nd_Nd_nk.removeAll(_nk);
      final Set<Integer> _nd_Nd_nk_plus_nd_minus_n_k = _nd_Nd_nk;
      
      nd[jumpProcessIdx] = deps.convert(_nd, true);
      nk[jumpProcessIdx] = deps.convert(_nk, true);
      _Nd_nk.add(jumpProcessIdx);
      Nd_nk_plus_id[jumpProcessIdx] = deps.convert(_Nd_nk, false);
      nd_Nd_nk_plus_nd_minus_nk[jumpProcessIdx] = deps.convert(_nd_Nd_nk_plus_nd_minus_n_k, true);
    }
  }
  
  private static class Dependencies
  {
    // var -> jump timers that refer to it
    private final Map<Integer, Set<Integer>> _Nds = new LinkedHashMap<>();
    
    private final IdentityHashMap<Object, Integer> variable2Index = new IdentityHashMap<>();
    
    private final PDMP pdmp;
    
    private Dependencies(PDMP pdmp)
    {
      this.pdmp = pdmp;
      // index the variables and create variable nodes
      for (int variableIndex = 0; variableIndex < pdmp.coordinates.size(); variableIndex++)
        variable2Index.put(pdmp.coordinates.get(variableIndex), variableIndex);
      for (int factorIndex = 0; factorIndex < pdmp.jumpProcesses.size(); factorIndex++)
        for (Object var : pdmp.jumpProcesses.get(factorIndex).clock.requiredVariables())
          BriefMaps.getOrPutSet(_Nds, variable2Index.get(var)).add(factorIndex);
    }
    
    private Set<Integer> nd(int j)
    {
      return n_(j, pdmp.jumpProcesses.get(j).clock);
    }
    
    private Set<Integer> nd(Set<Integer> js)
    {
      return lift(js, (Integer j) -> nd(j));
    }
    
    private Set<Integer> Nd(int v)
    {
      return _Nds.get(v);
    }
    
    private Set<Integer> Nd(Set<Integer> vs)
    {
      return lift(vs, (Integer j) -> Nd(j));
    }
    
    private Set<Integer> nk(int j)
    {
      return n_(j, pdmp.jumpProcesses.get(j).kernel);
    }
    
    private Set<Integer> n_(int j, StateDependent stateDep)
    {
      LinkedHashSet<Integer> result = new LinkedHashSet<>();
      for (Object variable : stateDep.requiredVariables())
        result.add(variable2Index.get(variable));
      return result;
    }
    
    private static <T> Set<T> lift(Set<T> input, Function<T, Set<T>> singleInputVersion)
    {
      Set<T> result = new LinkedHashSet<T>();
      for (T item : input)
        result.addAll(singleInputVersion.apply(item));
      return result;
    }
    
    private int [] convert(Collection<Integer> collection, boolean isVar)
    {
      return convert(collection, (isVar ? pdmp.coordinates.size() : pdmp.jumpProcesses.size()));
    }
    
    private int [] convert(Collection<Integer> collection, int size)
    {
      if (collection == null || collection.isEmpty())
        return null;
      
      if (collection.size() == size)
        return new int[]{-1};
      
      int [] result = new int[collection.size()];
      
      int idx = 0;
      for (Integer item : collection)
        result[idx++] = item;
      
      return result;
    }
  }
  
  
  //// Setters and getters
  
  public double getMaxTrajectoryLengthPerChunk()
  {
    return maxTrajectoryLengthPerChunk;
  }

  public void setMaxTrajectoryLengthPerChunk(double maxTrajectoryLengthPerChunk)
  {
    this.maxTrajectoryLengthPerChunk = maxTrajectoryLengthPerChunk;
  }

  public long getNumberOfQueuePolls()
  {
    return numberOfQueuePolls;
  }

  public long getNumberOfJumps()
  {
    return numberOfJumps;
  }
  
  public void setPrintSummaryStatistics(boolean value) 
  {
    this.printSummaryStatistics = value;
  }
  
  private void printSummaryStatistics()
  {
    if (!printSummaryStatistics)
      return;
    System.out.println("PDMPSimulator-summary: " + 
      "wallClockTimeMillis=" + (System.currentTimeMillis() - startTimeMilliSeconds) + ", " +
      "trajectoryLength=" + totalProcessTime + ", " +
      "nJumps=" + getNumberOfJumps() + ", " + 
      "nQueuePolls=" + getNumberOfQueuePolls()
      );
  }
}
