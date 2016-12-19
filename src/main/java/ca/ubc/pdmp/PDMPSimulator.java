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
   
  private PDMPSimulator(PDMP pdmp) 
  {
    this.pdmp = pdmp; 
    this.numberOfJumpProcesses = pdmp.jumpProcesses.size();
    this.numberOfVariables = pdmp.dynamics.size();
    this.nd = new int[numberOfJumpProcesses][];
    this.nk = new int[numberOfJumpProcesses][];
    this.Nd_nk = new int[numberOfJumpProcesses][];
    this.nd_Nd_nk_plus_nd_minus_n_k = new int[numberOfJumpProcesses][];
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
   *            takes as input factor indices
   * prefix N : inverse neighbors (i.e. set of timers/kernels that depend on given variable index)
   *            takes as input variable indices
   * 
   * suffix k : related to jump *k*ernels
   * suffix d : related to timers (which provide *d*elta times)
   * 
   * For example, Nd_nk[j]: 
   *   for given factor index j, this returns Nd(nk(j)), 
   *   a list of variables.
   */

  // factor -> var
  private final int [][] nd, nk;
  
  // factor -> factor
  private final int [][] Nd_nk;
  
  // factor -> var
  private final int [][] nd_Nd_nk_plus_nd_minus_n_k; 
  
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
  
  private Random               random;
  private StoppingCriterion    stoppingRule;
  private long                 startTimeMilliSeconds;
  private int                  numberOfQueuePolls; 
  
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
  
  // TODO: the public version of that will truncate in process time to ensure better numerical properties
  // TODO: test by having a long chain followed by a bunch of short ones; watch moments
  void simulate(Random random, StoppingCriterion stoppingCriterion)
  {
    init(random, stoppingCriterion);
    
    for (int eventSourceIndex = 0; eventSourceIndex < pdmp.jumpProcesses.size(); eventSourceIndex++)
      simulateNextEventDeltaTime(eventSourceIndex);
    
    while (moreSamplesNeeded())
    {
      // retrieve info about event
      final Entry<Double, Integer> event = queue.pollEvent();
      numberOfQueuePolls++;
      time = event.getKey();
      final int eventSourceIndex = event.getValue();
        
      if (isBoundIndicators[eventSourceIndex])  
      {
        updateVariables(nd[eventSourceIndex], false);
        
        // recompute new time
        simulateNextEventDeltaTime(eventSourceIndex);
        
        // undo
        rollBack(nd[eventSourceIndex]);
      }
      else
      {
        updateVariables(nk[eventSourceIndex], true);
        updateVariables(nd_Nd_nk_plus_nd_minus_n_k[eventSourceIndex], false);
        
        // do the jump
        pdmp.jumpProcesses.get(eventSourceIndex).kernel.simulate(random);
        
        // recompute factor 'hood new times (including self) 
        simulateNextEventDeltaTimes(Nd_nk[eventSourceIndex]);
        
        // extended 'hood: undo
        rollBack(nd_Nd_nk_plus_nd_minus_n_k[eventSourceIndex]);
      }
    }
    
    // final update on all variables
    updateAllVariables(true); 
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
    
    if (numberOfQueuePolls > stoppingRule.numberOfQueuePolls)
      return false;
    
    return true;
  }

  private void updateAllVariables(boolean commit)
  {
    for (int varIdx = 0; varIdx < numberOfVariables; varIdx++)
      _updateVariable(varIdx, commit);
  }
  
  private void updateVariables(int [] variables, boolean commit)
  {
    if (variables == null)
      return;
    
    if (variables[0] == -1)
      updateAllVariables(commit);
    else
      for (int variableIdx : variables)
        _updateVariable(variableIdx, commit);
  }

  private void _updateVariable(int variableIndex, boolean commit)
  {
    if (lastUpdateTime[variableIndex] == time)
      return;
    
    final Coordinate coordinate = pdmp.dynamics.get(variableIndex);
    final double deltaTime = time - lastUpdateTime[variableIndex];
    if (commit)
    {
      final int [] processorsForThisVar = processors[variableIndex];
      if (processorsForThisVar != null)
        for (int processorIdx : processorsForThisVar)
          pdmp.processors.get(processorIdx).process(deltaTime);
      lastUpdateTime[variableIndex] = time;
    }
    coordinate.extrapolateInPlace(deltaTime);
  }
  
  private void _rollBack(int variableIndex)
  {
    final Coordinate coordinate = pdmp.dynamics.get(variableIndex);
    final double deltaTime = time - lastUpdateTime[variableIndex];
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
  
  private void simulateNextEventDeltaTimes(int [] eventSourceIndices)
  {
    if (eventSourceIndices == null)
      return;
    if (eventSourceIndices[0] == -1)
      for (int i = 0; i < numberOfJumpProcesses; i++)
        simulateNextEventDeltaTime(i);
    else
      for (int index : eventSourceIndices)
        simulateNextEventDeltaTime(index);
  }
  
  private void simulateNextEventDeltaTime(int eventSourceIndex)
  {
    final DeltaTime nextEvent = pdmp.jumpProcesses.get(eventSourceIndex).timer.next(random);
    double absoluteTime = time + nextEvent.deltaTime;
    if (absoluteTime <= stoppingRule.stochasticProcessTime)
    {
      isBoundIndicators[eventSourceIndex] = nextEvent.isBound;
      absoluteTime = fixNumericalIssue(absoluteTime);
      queue.add(eventSourceIndex, absoluteTime);
    }
  }
  
  //// Some low-level details
  
  private static final double epsilon = 1;
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
      for (int variableIndex = 0; variableIndex < pdmp.dynamics.size(); variableIndex++)
        variable2Index.put(pdmp.dynamics.get(variableIndex), variableIndex);
      for (int factorIndex = 0; factorIndex < pdmp.jumpProcesses.size(); factorIndex++)
        for (Object var : pdmp.jumpProcesses.get(factorIndex).timer.requiredVariables())
          BriefMaps.getOrPutSet(_Nds, variable2Index.get(var)).add(factorIndex);
    }
    
    private Set<Integer> nd(int j)
    {
      return n_(j, pdmp.jumpProcesses.get(j).timer);
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
      return convert(collection, (isVar ? pdmp.dynamics.size() : pdmp.jumpProcesses.size()));
    }
    
    private int [] convert(Collection<Integer> collection, int size)
    {
      if (collection.isEmpty())
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
      int variableIdx = deps.variable2Index.get(currentProc.requiredVariables().get(0));
      BriefMaps.getOrPutSet(processorMappings, variableIdx).add(procIdx);
    }
    for (int variableIdx = 0; variableIdx < numberOfVariables; variableIdx++)
      processors[variableIdx] = deps.convert(processorMappings.get(variableIdx), Integer.MAX_VALUE);
    
    // variable and factor neighborhoods
    for (int eventIdx = 0; eventIdx < numberOfJumpProcesses; eventIdx++)
    {
      final Set<Integer> _nd = deps.nd(eventIdx);
      final Set<Integer> _nk = deps.nk(eventIdx);
      final Set<Integer> _Nd_nk = deps.Nd(_nk);
      final Set<Integer> _nd_Nd_nk = deps.nd(_Nd_nk);
      _nd_Nd_nk.addAll(_nd);
      _nd_Nd_nk.removeAll(_nk);
      final Set<Integer> _nd_Nd_nk_plus_nd_minus_n_k = _nd_Nd_nk;
      
      nd[eventIdx] = deps.convert(_nd, true);
      nk[eventIdx] = deps.convert(_nk, true);
      Nd_nk[eventIdx] = deps.convert(_Nd_nk, false);
      nd_Nd_nk_plus_nd_minus_n_k[eventIdx] = deps.convert(_nd_Nd_nk_plus_nd_minus_n_k, true);
    }
  }
}
