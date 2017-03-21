package ca.ubc.bps;


import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.inject.TypeLiteral;

import ca.ubc.bps.bounces.BounceFactory;
import ca.ubc.bps.processors.SaveTrajectory;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.state.Dynamics;
import ca.ubc.bps.state.PiecewiseConstant;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.JumpKernel;
import ca.ubc.pdmp.JumpProcess;
import ca.ubc.pdmp.PDMP;
import ca.ubc.pdmp.PDMPSimulator;
import ca.ubc.pdmp.StoppingCriterion;
import blang.inits.Arg;
import blang.inits.Creator;
import blang.inits.DefaultValue;
import blang.inits.DesignatedConstructor;
import blang.inits.InitService;
import blang.inits.Input;
import blang.inits.providers.CollectionsProviders;

import static ca.ubc.bps.BPSFactoryHelpers.*;

public class BPSFactory
{
  @Arg 
  public Model model = isotropicGlobal(2);
  
  @Arg 
  public Dynamics dynamics = linear;
  
  @Arg 
  public BounceFactory bounce = standard;
  
  @Arg
  public RefreshmentFactory refreshment = local(1);
  
  @Arg
  public MonitoredIndices save = all;
  
  @Arg
  public boolean stationaryInitialization = false;
  
  @Arg @DefaultValue("1")
  public Random simulationRandom = new Random(1);
  
  @Arg @DefaultValue("1")
  public Random initializationRandom = new Random(1);
  
  @Arg @DefaultValue("--stochasticProcessTime 100.0")
  public StoppingCriterion stoppingRule = StoppingCriterion.byStochasticProcessTime(100.0);
  
  @Arg @DefaultValue("ZERO")
  public InitializationStrategy initialization = InitializationStrategy.ZERO;
  
  public static enum InitializationStrategy
  {
    ZERO, STATIONARY;
  }
  
  public static class MonitoredIndices
  {
    private final List<Integer> list;
    @DesignatedConstructor
    public MonitoredIndices(
        @Input(formatDescription = "all|none|space-separated indices") List<String> strings,
        @InitService final Creator creator)
    {
      if (strings.size() == 1 && strings.get(0).trim().equals("all"))
        this.list = null;
      else if (strings.size() == 1 && strings.get(0).trim().equals("none"))
        this.list = new ArrayList<>();
      else
      {
        TypeLiteral<List<Integer>> listOfInts = new TypeLiteral<List<Integer>>() {};
        this.list = CollectionsProviders.parseList(strings, listOfInts, creator);
      }
    }
    public MonitoredIndices(List<Integer> list) 
    {
      this.list = list;
    }
    public List<Integer> get(int numberItems)
    {
      if (list == null)
      {
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < numberItems; i++)
          result.add(i);
        return result;
      }
      else
        return list;
    }
  }

  @FunctionalInterface
  public static interface RefreshmentFactory
  {
     public void addRefreshment(PDMP pdmp);
  }
  
  public static interface Model
  {
    public void setup(ModelBuildingContext context, boolean initializeStatesFromStationary);
  }
  
  public class ModelBuildingContext
  {
    public final Random initializationRandom;
    public ModelBuildingContext(Random initializationRandom)
    {
      this.initializationRandom = initializationRandom;
    }
    private List<JumpProcess> jumpProcesses = new ArrayList<>();
    private LinkedHashSet<ContinuouslyEvolving> continuouslyEvolvingStates = null;
    private LinkedHashSet<PiecewiseConstant<?>> piecewiseConstantStates = new LinkedHashSet<>();
    public List<ContinuouslyEvolving> continuouslyEvolvingStates(int dim) 
    {
      if (continuouslyEvolvingStates != null)
        throw new RuntimeException();
      List<ContinuouslyEvolving> result = ContinuouslyEvolving.buildArray(dim, dynamics);
      continuouslyEvolvingStates = 
          new LinkedHashSet<>(result);
      return result;
    }
    public void registerBPSPotential(BPSPotential potential)
    {
      // find which variables bounce: those that are continuously evolving
      List<? extends Coordinate> allReqVars = new ArrayList<>(potential.clock.requiredVariables());
      List<ContinuouslyEvolving> continuousCoordinates = StaticUtils.continuousCoordinates(allReqVars);
      // check they form the prefix (to make correspondence with energy indices straightforward)
      if (!continuousCoordinates.equals(allReqVars.subList(0, continuousCoordinates.size())))
        throw new RuntimeException();
      JumpKernel kernel = bounce.build(continuousCoordinates, potential.energy);
      JumpProcess process = new JumpProcess(potential.clock, kernel);
      registerJumpProcess(process);
    }
    public List<Coordinate> coordinates() 
    {
      List<Coordinate> result = new ArrayList<>();
      result.addAll(continuouslyEvolvingStates);
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
        if (c instanceof ContinuouslyEvolving)
        {
          if (!continuouslyEvolvingStates.contains(c))
            throw new RuntimeException();
        }
        else
          piecewiseConstantStates.add((PiecewiseConstant<?>) c);
    }
  }

  public BPS buildAndRun()
  {
    ModelBuildingContext modelContext = new ModelBuildingContext(initializationRandom);
    
    // setup bounces and variables
    boolean modelShouldInitToStationarity = initialization == InitializationStrategy.STATIONARY;
    model.setup(modelContext, modelShouldInitToStationarity);
    PDMP pdmp = new PDMP(modelContext.coordinates());
    pdmp.jumpProcesses.addAll(modelContext.jumpProcesses);
    
    // refreshments
    refreshment.addRefreshment(pdmp);
    BPS result = new BPS(pdmp, new ArrayList<>(modelContext.continuouslyEvolvingStates));
    
    // monitors
    Set<Integer> savedIndices = new LinkedHashSet<>(save.get(result.continuouslyEvolvingStates.size()));
    int index = 0;
    for (ContinuouslyEvolving variable : result.continuouslyEvolvingStates)
      if (savedIndices.contains(index++))
      {
        SaveTrajectory processor = new SaveTrajectory(variable);
        result.savedTrajectories.put(variable, processor);
        pdmp.processors.add(processor);
      }
    
    // initializations 
    initializeVelocities(result.continuouslyEvolvingStates);
    initializePositions(result.continuouslyEvolvingStates);
    
    result.simulator = new PDMPSimulator(result.pdmp);
    result.simulator.simulate(simulationRandom, stoppingRule);
    
    return result;
  }
  
  private void initializePositions(List<ContinuouslyEvolving> continuouslyEvolvingStates)
  {
    if (initialization == InitializationStrategy.ZERO)
      ; // nothing to do, this is the default
    else if (initialization == InitializationStrategy.STATIONARY)
      ; // nothing to do, already initialized when creating model
    else
      throw new RuntimeException();
  }

  private void initializeVelocities(List<ContinuouslyEvolving> continuouslyEvolvingStates)
  {
    for (ContinuouslyEvolving coordinate : continuouslyEvolvingStates)
      coordinate.velocity.set(initializationRandom.nextGaussian());
  }

  public static class BPS
  {
    public final PDMP pdmp;
    public PDMPSimulator simulator;
    public final List<ContinuouslyEvolving> continuouslyEvolvingStates;
    
    public final Map<ContinuouslyEvolving, SaveTrajectory> savedTrajectories = new LinkedHashMap<ContinuouslyEvolving, SaveTrajectory>();

    public BPS(PDMP pdmp, List<ContinuouslyEvolving> continuouslyEvolvingStates)
    {
      this.continuouslyEvolvingStates = continuouslyEvolvingStates;
      this.pdmp = pdmp;
    }
    
    // convenience methods
  }
  
  public static void main(String [] args)
  {
    BPSFactory f = new BPSFactory();
    f.bounce = dependentRandomized;
    BPS bps = f.buildAndRun();
  }
  
}
