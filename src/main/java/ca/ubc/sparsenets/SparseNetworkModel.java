package ca.ubc.sparsenets;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import briefj.BriefIO;
import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.factory.ModelBuildingContext;
import ca.ubc.bps.models.Model;
import ca.ubc.bps.state.PiecewiseLinear;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.bps.timers.ConstantIntensityAdaptiveThinning;
import ca.ubc.bps.timers.StandardIntensity;
import ca.ubc.bps.timers.Superposition;

public class SparseNetworkModel implements Model
{
  @Arg
  public File degreeFile;
  
  @Arg   @DefaultValue("6.5427") 
  public double wStar = 6.5427;
  
  @Arg @DefaultValue("1.0")
  public double tau = 1.0;

  @Arg   @DefaultValue("0.5")
  public double sigma = 0.5;

  @Override
  public void setup(ModelBuildingContext context, boolean initializeStatesFromStationary)
  {
    if (!(context.dynamics() instanceof PiecewiseLinear) || 
        initializeStatesFromStationary) 
      throw new RuntimeException();
    
    // TODO: read matrix to compute m_i's 
    List<Integer> degrees = readDegrees();
    int nNodes = degrees.size();
    
    List<PositionVelocity> variables = context.buildAndRegisterPositionVelocityCoordinates(nNodes);
    
    // quadratic term
    QuadraticEnergy quadEnergy = new QuadraticEnergy(wStar, tau);
    StandardIntensity quadIntensity = new StandardIntensity(variables, quadEnergy);
    ConstantIntensityAdaptiveThinning quadTimer = new ConstantIntensityAdaptiveThinning(variables, quadIntensity);
    BPSPotential quadPotential = new BPSPotential(quadEnergy, quadTimer);
    
    // linear term
    LinearEnergy linEnergy = new LinearEnergy(degrees, sigma);
    LinearTimer linTimer = new LinearTimer(variables, degrees, sigma);
    BPSPotential linPotential = new BPSPotential(linEnergy, linTimer);
    
    List<BPSPotential> potentials = Arrays.asList(quadPotential, linPotential);
    context.registerBPSPotential(Superposition.createSuperpositionBPSPotential(potentials));
   
//    // TODO: add sampling of discrete variables [LATER; this will make it harder to compare to Stan]
//    // TODO: add sampling of global parameters  [LATER; this will make it harder to compare to Stan]
  }

  protected List<Integer> readDegrees()
  {
    List<Integer> result = new ArrayList<Integer>();
    for (String line : BriefIO.readLines(degreeFile))
      if (!line.isEmpty())
        result.add(Integer.parseInt(line));
    return result;
  }

}
