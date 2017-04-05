package ca.ubc.bps.factory;


import static ca.ubc.bps.factory.BPSFactoryHelpers.all;
import static ca.ubc.bps.factory.BPSFactoryHelpers.isotropicGlobal;
import static ca.ubc.bps.factory.BPSFactoryHelpers.linear;
import static ca.ubc.bps.factory.BPSFactoryHelpers.local;
import static ca.ubc.bps.factory.BPSFactoryHelpers.standard;
import static ca.ubc.bps.factory.BPSFactoryHelpers.zero;

import java.io.File;
import java.io.Writer;
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

import blang.inits.Arg;
import blang.inits.Creator;
import blang.inits.Creators;
import blang.inits.DefaultValue;
import blang.inits.experiments.Experiment;
import blang.inits.experiments.ExperimentResults;
import blang.inits.parsing.Arguments;
import blang.inits.parsing.CSVFile;
import briefj.BriefIO;
import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.bounces.BounceFactory;
import ca.ubc.bps.models.Model;
import ca.ubc.bps.processors.IntegrateTrajectory;
import ca.ubc.bps.refresh.RefreshmentFactory;
import ca.ubc.bps.processors.MemorizeTrajectory;
import ca.ubc.bps.processors.MomentIntegrator;
import ca.ubc.bps.processors.SegmentIntegrator;
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
  
  @Arg @DefaultValue({"1", "2", "3", "4"})
  public List<Integer> summarizedMomentDegrees = Arrays.asList(1, 2, 3, 4);
  
  @Arg @DefaultValue("EXPONENTIALLY_SPACED")
  public PartialSumOutputMode partialSumOutputMode = PartialSumOutputMode.EXPONENTIALLY_SPACED; 
  
  @Arg @DefaultValue("1")
  public long simulationRandom = 1L;
  
  @Arg @DefaultValue("1")
  public long initializationRandom = 1L;
  
  @Arg @DefaultValue({"--stochasticProcessTime", "10_000"})
  public StoppingCriterion stoppingRule = StoppingCriterion.byStochasticProcessTime(10_000);
  
  @Arg @DefaultValue("Zero") 
  public InitializationStrategy initialization = zero;
  
  @Arg @DefaultValue("false")
  public boolean forbidOutputFiles = true; // Note: programmatic initialization intentionally different
  
  public static BPSFactory loadBPSFactory(File bpsExecFolder, ExperimentResults results)
  {
    Arguments arguments = CSVFile.parseTSV(new File(bpsExecFolder, Experiment.CSV_ARGUMENT_FILE));
    Creator c = Creators.conventional();
    c.addGlobal(ExperimentResults.class, results);
    try { return c.init(BPSFactory.class, arguments); }
    catch (Exception e) { throw new RuntimeException("Init failure. Details: \n" + c.errorReport()); }
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
      List<ContinuouslyEvolving> continuousCoordinates = BPSStaticUtils.continuousCoordinates(allReqVars);
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

  private void initializeVelocities(Collection<ContinuouslyEvolving> continuouslyEvolvingStates)
  {
    Random initializationRandom = new Random(30284L * this.initializationRandom + 394);
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
      modelContext = new ModelBuildingContext(new Random(8493L * initializationRandom + 948));
      
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
      initialization.initializePositions(modelContext.continuouslyEvolvingStates);
    }
    
    public PDMP getPDMP()
    {
      if (isRun())
        throw new RuntimeException();
      return pdmp;
    }
    
    public void addProcessor(Processor processor)
    {
      
      getPDMP().processors.add(processor);
    }
    
    private PDMPSimulator simulator = null;
    public void run()
    {
      simulator = new PDMPSimulator(pdmp);
      simulator.simulate(new Random(74737L * simulationRandom + 33304L), stoppingRule);
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
        BriefIO.write(results.getFileInResultFolder(SUMMARY_STATS_FILE_NAME), result);
      }
      if (!memorizedTrajectories.isEmpty())
      {
        StringBuilder result = new StringBuilder();
        result.append(VARIABLE_KEY + ",moment,value\n");
        for (int degree : summarizedMomentDegrees)
          for (ContinuouslyEvolving variable : memorizedTrajectories.keySet())
            result.append("" + variable.key + "," + degree + "," + memorizedTrajectories.get(variable).getTrajectory().momentEss(degree) + "\n");
        BriefIO.write(results.getFileInResultFolder(ESS_FILE_NAME), result);
      }
      writeFinalSamples();
    }
    
    private void writeFinalSamples()
    {
      Writer out = results.getAutoClosedBufferedWriter(FINAL_SAMPLES);
      BriefIO.println(out, VARIABLE_KEY + ",value");
      for (ContinuouslyEvolving state : continuouslyEvolvingStates())
        BriefIO.println(out, state.key + "," + state.position.get());
    }

    public static final String 
      FINAL_SAMPLES                               = "finalSamples.csv",
      CONTINUOUSLY_EVOLVING_SAMPLES_DIR_NAME      = "continuouslyEvolvingSamples",
      CONTINUOUSLY_EVOLVING_PARTIAL_SUMS_DIR_NAME = "continuouslyEvolvingPartialSums",
      SUMMARY_STATS_FILE_NAME                     = "summaryStatistics.csv",
      ESS_FILE_NAME                               = "ess.csv",
      DATA_FILE_NAME                              = "data.csv";
    
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
            SegmentIntegrator integrator = new MomentIntegrator(degree);
            IntegrateTrajectory processor = new IntegrateTrajectory(variable, integrator); 
            summarizedTrajectories.put(variable, momentKey(degree), processor);
            pdmp.processors.add(processor);
            if (!forbidOutputFiles && partialSumOutputMode != PartialSumOutputMode.OFF)
            {
              if (results == null)
                results = BPSFactory.this.results.child(CONTINUOUSLY_EVOLVING_PARTIAL_SUMS_DIR_NAME);
              ExperimentResults variableResults = results.child(MOMENT_KEY, degree).child(VARIABLE_KEY, index);
              processor.setOutput(variableResults.getAutoClosedBufferedWriter(DATA_FILE_NAME), partialSumOutputMode == PartialSumOutputMode.EXPONENTIALLY_SPACED);
            }
          }
        else if (type == MonitorType.WRITE)
        {
          if (results == null)
            results = BPSFactory.this.results.child(CONTINUOUSLY_EVOLVING_SAMPLES_DIR_NAME);
          ExperimentResults variableResults = results.child(VARIABLE_KEY, index);
          WriteTrajectory processor = new WriteTrajectory(variable, variableResults.getAutoClosedBufferedWriter(DATA_FILE_NAME));
          pdmp.processors.add(processor);
        }
        else
          throw new RuntimeException();
      }
    }

    private PDMP setupVariablesAndBounces() 
    {
      boolean modelShouldInitToStationarity = initialization.requestStationarySampling();
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
  
  public static void main(String [] args)
  {
    Experiment.startAutoExit(args);
  }
}
