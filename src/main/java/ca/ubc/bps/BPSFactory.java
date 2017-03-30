package ca.ubc.bps;


import static ca.ubc.bps.BPSFactoryHelpers.all;
import static ca.ubc.bps.BPSFactoryHelpers.isotropicGlobal;
import static ca.ubc.bps.BPSFactoryHelpers.linear;
import static ca.ubc.bps.BPSFactoryHelpers.local;
import static ca.ubc.bps.BPSFactoryHelpers.standard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.Tables;
import com.google.inject.TypeLiteral;

import blang.inits.Arg;
import blang.inits.Creator;
import blang.inits.DefaultValue;
import blang.inits.DesignatedConstructor;
import blang.inits.InitService;
import blang.inits.Input;
import blang.inits.experiments.Experiment;
import blang.inits.experiments.ExperimentResults;
import blang.inits.providers.CollectionsProviders;
import briefj.BriefIO;
import ca.ubc.bps.bounces.BounceFactory;
import ca.ubc.bps.processors.IntegrateTrajectory;
import ca.ubc.bps.processors.IntegrateTrajectory.SegmentIntegrator;
import ca.ubc.bps.processors.MemorizeTrajectory;
import ca.ubc.bps.processors.WriteTrajectory;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.state.Dynamics;
import ca.ubc.bps.state.PiecewiseConstant;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.JumpKernel;
import ca.ubc.pdmp.JumpProcess;
import ca.ubc.pdmp.PDMP;
import ca.ubc.pdmp.PDMPSimulator;
import ca.ubc.pdmp.Processor;
import ca.ubc.pdmp.StoppingCriterion;

public class BPSFactory extends Experiment
{
  @Arg @DefaultValue("FixedPrecisionNormalModel")
  public Model model = isotropicGlobal(2);
  
  @Arg @DefaultValue("PiecewiseLinear")
  public Dynamics dynamics = linear();
  
  @Arg @DefaultValue("Standard")
  public BounceFactory bounce = standard;
  
  @Arg @DefaultValue("Standard")
  public RefreshmentFactory refreshment = local(1);
  
  @Arg @DefaultValue("all")
  public MonitoredIndices write = all;
  
  @Arg @DefaultValue("all")
  public MonitoredIndices memorize = all;
  
  @Arg @DefaultValue("all")
  public MonitoredIndices summarize = all;
  
  @Arg @DefaultValue({"1", "2"})
  public List<Integer> summarizedMomentDegrees = Arrays.asList(1, 2);
  
  @Arg @DefaultValue("EXPONENTIALLY_SPACED")
  public PartialSumOutputMode partialSumOutputMode = PartialSumOutputMode.EXPONENTIALLY_SPACED;
  
  @Arg @DefaultValue("false")
  public boolean stationaryInitialization = false;
  
  @Arg @DefaultValue("1")
  public Random simulationRandom = new Random(1);
  
  @Arg @DefaultValue("1")
  public Random initializationRandom = new Random(1);
  
  @Arg @DefaultValue({"--stochasticProcessTime", "10_000"})
  public StoppingCriterion stoppingRule = StoppingCriterion.byStochasticProcessTime(10_000);
  
  @Arg @DefaultValue("ZERO")
  public InitializationStrategy initialization = InitializationStrategy.ZERO;
  
  @Arg @DefaultValue("false")
  public boolean forbidOutputFiles = true; // Note: programmatic initialization intentionally different
  
  public static enum InitializationStrategy
  {
    ZERO, STATIONARY;
  }
  
  public static enum PartialSumOutputMode
  {
    OFF, EXPONENTIALLY_SPACED, ALL;
  }
  
  public class ModelBuildingContext
  {
    public final Random initializationRandom;
    private List<JumpProcess> jumpProcesses = new ArrayList<>();
    private LinkedHashSet<ContinuouslyEvolving> continuouslyEvolvingStates = null;
    private LinkedHashSet<PiecewiseConstant<?>> piecewiseConstantStates = new LinkedHashSet<>();
    
    public ModelBuildingContext(Random initializationRandom)
    {
      this.initializationRandom = initializationRandom;
    }
    public Dynamics dynamics()
    {
      return dynamics;
    }
    public List<ContinuouslyEvolving> buildAndRegisterContinuouslyEvolvingStates(int dim) 
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
    private List<Coordinate> coordinates() 
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
  
  public BPS buildBPS()
  {
    return new BPS();
  }

  public BPS buildAndRun() 
  {
    BPS result = buildBPS();
    result.run();
    result.writeRequestedResults();
    return result;
  }
  
  @Override
  public void run()
  {
    buildAndRun();
  }
  
  private void initializePositions(Collection<ContinuouslyEvolving> continuouslyEvolvingStates)
  {
    if (initialization == InitializationStrategy.ZERO)
      ; // nothing to do, this is the default
    else if (initialization == InitializationStrategy.STATIONARY)
      ; // nothing to do, already initialized when creating model
    else
      throw new RuntimeException();
  }

  private void initializeVelocities(Collection<ContinuouslyEvolving> continuouslyEvolvingStates)
  {
    for (ContinuouslyEvolving coordinate : continuouslyEvolvingStates)
      coordinate.velocity.set(initializationRandom.nextGaussian());
  }
  
  public class BPS
  {
    private final PDMP pdmp;
    public final Map<ContinuouslyEvolving, MemorizeTrajectory> memorizedTrajectories = new LinkedHashMap<>();
    public final Table<ContinuouslyEvolving, String, IntegrateTrajectory> summarizedTrajectories 
      = Tables.newCustomTable(new LinkedHashMap<>(), LinkedHashMap::new);
    private final ModelBuildingContext modelContext;
    private final int nBounceProcesses;
    
    public Collection<ContinuouslyEvolving> continuouslyEvolvingStates()
    {
      return modelContext.continuouslyEvolvingStates;
    }
    
    public boolean isBounce(int jumpCoordinate)
    {
      return jumpCoordinate < nBounceProcesses;
    }
    
    public BPS()
    {
      modelContext = new ModelBuildingContext(initializationRandom);
      
      // setup bounces and variables
      pdmp = setupVariablesAndBounces();
      nBounceProcesses = pdmp.jumpProcesses.size();
      
      // refreshments
      refreshment.addRefreshment(pdmp);
      
      // monitors
      for (MonitorType type : MonitorType.values())
        setupMonitors(pdmp, type);
      
      // initializations 
      initializeVelocities(modelContext.continuouslyEvolvingStates);
      initializePositions(modelContext.continuouslyEvolvingStates);
    }
    
    public void addProcessor(Processor processor)
    {
      if (isRun())
        throw new RuntimeException();
      pdmp.processors.add(processor);
    }
    
    private PDMPSimulator simulator = null;
    public void run()
    {
      simulator = new PDMPSimulator(pdmp);
      simulator.simulate(simulationRandom, stoppingRule);
    }
    
    public boolean isRun()
    {
      return simulator != null;
    }
    
    public void writeRequestedResults()
    {
      if (forbidOutputFiles)
        return;
      if (!summarizedTrajectories.isEmpty())
      {
        StringBuilder result = new StringBuilder();
        result.append(VARIABLE_KEY + ",moment,value\n");
        for (Cell<ContinuouslyEvolving, String, IntegrateTrajectory> cell : summarizedTrajectories.cellSet())
          result.append("" + cell.getRowKey().key + "," + cell.getColumnKey() + "," + cell.getValue().integrate() + "\n");
        BriefIO.write(results.getFileInResultFolder("summaryStatistics.csv"), result);
      }
      if (!memorizedTrajectories.isEmpty())
      {
        StringBuilder result = new StringBuilder();
        result.append(VARIABLE_KEY + ",moment,value\n");
        for (int degree : summarizedMomentDegrees)
          for (ContinuouslyEvolving variable : memorizedTrajectories.keySet())
            result.append("" + variable.key + "," + degree + "," + memorizedTrajectories.get(variable).getTrajectory().momentEss(degree) + "\n");
        BriefIO.write(results.getFileInResultFolder("ess.csv"), result);
      }
    }
    
    public static final String 
      CONTINUOUSLY_EVOLVING_SAMPLES_DIR_NAME =  "continuouslyEvolvingSamples",
      CONTINUOUSLY_EVOLVING_PARTIAL_SUMS_DIR_NAME = "continuouslyEvolvingPartialSums";
    
    private void setupMonitors(
        PDMP pdmp, 
        MonitorType type
        )
    {
      if (type == MonitorType.WRITE && forbidOutputFiles)
        return;
      
      ExperimentResults results = null;
      MonitoredIndices requested = null;
      
      requested = type == MonitorType.MEMORIZE  ? memorize  : requested;
      requested = type == MonitorType.SUMMARIZE ? summarize : requested;
      requested = type == MonitorType.WRITE     ? write     : requested;

      Set<Integer> savedIndices = new LinkedHashSet<>(requested.get(modelContext.continuouslyEvolvingStates.size()));
      
      loop : for (ContinuouslyEvolving variable : modelContext.continuouslyEvolvingStates)
      {
        final int index = (int) variable.key;
        if (!savedIndices.contains(index))
           continue loop;
        if (type == MonitorType.MEMORIZE)
        {
          MemorizeTrajectory processor = new MemorizeTrajectory(variable);
          memorizedTrajectories.put(variable, processor);
          pdmp.processors.add(processor);
        }
        else if (type == MonitorType.SUMMARIZE)
          for (int degree : summarizedMomentDegrees)
          {
            SegmentIntegrator integrator = new IntegrateTrajectory.MomentIntegrator(degree);
            IntegrateTrajectory processor = new IntegrateTrajectory(variable, integrator); 
            summarizedTrajectories.put(variable, momentKey(degree), processor);
            pdmp.processors.add(processor);
            if (!forbidOutputFiles && partialSumOutputMode != PartialSumOutputMode.OFF)
            {
              if (results == null)
                results = BPSFactory.this.results.child(CONTINUOUSLY_EVOLVING_PARTIAL_SUMS_DIR_NAME);
              ExperimentResults variableResults = results.child(MOMENT_KEY, degree).child(VARIABLE_KEY, index);
              processor.setOutput(variableResults.getAutoClosedBufferedWriter("data.csv"), partialSumOutputMode == PartialSumOutputMode.EXPONENTIALLY_SPACED);
            }
          }
        else if (type == MonitorType.WRITE)
        {
          if (results == null)
            results = BPSFactory.this.results.child(CONTINUOUSLY_EVOLVING_SAMPLES_DIR_NAME);
          ExperimentResults variableResults = results.child(VARIABLE_KEY, index);
          WriteTrajectory processor = new WriteTrajectory(variable, variableResults.getAutoClosedBufferedWriter("data.csv"));
          pdmp.processors.add(processor);
        }
        else
          throw new RuntimeException();
      }
    }

    private PDMP setupVariablesAndBounces() 
    {
      boolean modelShouldInitToStationarity = initialization == InitializationStrategy.STATIONARY;
      model.setup(modelContext, modelShouldInitToStationarity);
      checkModelCreated();
      PDMP pdmp = new PDMP(modelContext.coordinates());
      pdmp.jumpProcesses.addAll(modelContext.jumpProcesses);
      return pdmp;
    }

    private void checkModelCreated()
    {
      if (modelContext.jumpProcesses.isEmpty())
        throw new RuntimeException("No bounce added by the model.");
      if (modelContext.continuouslyEvolvingStates.isEmpty() && modelContext.piecewiseConstantStates.isEmpty())
        throw new RuntimeException("No variables added by the model.");
    }
  }
  
  public static String momentKey(int degree) 
  {
    return "" + degree;
  }
  
  public static final String VARIABLE_KEY = "variable";
  public static final String MOMENT_KEY = "momentDegree";
  
  private static enum MonitorType { MEMORIZE, WRITE, SUMMARIZE }
  
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
    private List<Integer> get(int numberItems)
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
  
  public static void main(String [] args)
  {
    Experiment.start(args);
  }
}
