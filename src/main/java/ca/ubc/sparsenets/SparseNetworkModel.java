package ca.ubc.sparsenets;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import blang.inits.Arg;
import briefj.BriefIO;
import ca.ubc.bps.bounces.FlipBounce;
import ca.ubc.bps.bounces.StandardBounce;
import ca.ubc.bps.energies.Energy;
import ca.ubc.bps.factory.ModelBuildingContext;
import ca.ubc.bps.models.Model;
import ca.ubc.bps.refresh.RefreshmentFactory;
import ca.ubc.bps.state.PiecewiseLinear;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.JumpKernel;
import ca.ubc.pdmp.JumpProcess;
import ca.ubc.pdmp.PDMP;

public class SparseNetworkModel implements Model
{
  @Arg
  public File degreeFile;
  
  @Arg
  public double wStar;
  
  @Arg
  public double tau;

  @Arg
  public double sigma;
  
  RefreshmentFactory refreshment = null; // Will be set by SparseNetMain
  protected boolean allowNullRef = false;
  
  List<PositionVelocity> variables;
  PositionVelocity sum;

  @Override
  public void setup(ModelBuildingContext context, boolean initializeStatesFromStationary)
  {
    if (!(context.dynamics() instanceof PiecewiseLinear) || 
        initializeStatesFromStationary) 
      throw new RuntimeException();
    
    // TODO: read matrix to compute m_i's 
    List<Integer> degrees = readDegrees();
    int nNodes = degrees.size();
    
    List<PositionVelocity> augmentedVars = context.buildAndRegisterPositionVelocityCoordinates(nNodes + 1); // use # node + 1 for the sum
    variables = augmentedVars.subList(0, nNodes);
    sum = augmentedVars.get(nNodes);
    
    if (refreshment != null && allowNullRef) // hack: needed by the stan superclass
      setupRefresh(context);
    
    //register(context, new QuadraticTimer(sum, wStar), new StandardBounce(variables, ones));
    register(context, new NaiveQuadraticTimer(variables, sum, wStar), new StandardBounce(variables, ones));
    
    for (int i = 0; i < variables.size(); i++)
    {
      PositionVelocity w = variables.get(i);
      register(context, new LinearTimer(w, tau), new FlipBounce(Collections.singletonList(w)));
      register(context, new LogTimer(w, degrees.get(i), sigma), new FlipBounce(Collections.singletonList(w)));
    }
    
    // TODO: add sampling of discrete variables [LATER; this will make it harder to compare to Stan]
    // TODO: add sampling of global parameters  [LATER; this will make it harder to compare to Stan]
  }

  private void register(ModelBuildingContext context, Clock timer, JumpKernel kernel)
  {
    context.registerJumpProcess(
        new JumpProcess(
            timer, 
            new UpdateSumJumpKernel(
                sum, 
                kernel))); 
  }

  private Energy ones = new Energy() 
  {
    double [] ones = null;
    @Override
    public double[] gradient(double[] point)
    {
      if (ones == null)
      {
        ones = new double[point.length];
        for (int i = 0; i < point.length; i++)
          ones[i] = 1.0;
      }
      return ones;
    }
    @Override
    public double valueAt(double[] point)
    {
      throw new RuntimeException();
    }
  };

  private void setupRefresh(ModelBuildingContext context)
  {
    PDMP dummy = new PDMP(variables);
    refreshment.addRefreshment(dummy);
    for (JumpProcess p : dummy.jumpProcesses)
      register(context, p.clock, p.kernel);
  }

  protected List<Integer> readDegrees()
  {
    List<Integer> result = new ArrayList<Integer>();
    for (String line : BriefIO.readLines(degreeFile))
      if (!line.isEmpty())
        result.add(Integer.parseInt(line));
    return result;
  }

  public void initSum()
  {
    UpdateSumJumpKernel.recompute(true,  sum.position, variables);
    UpdateSumJumpKernel.recompute(false, sum.velocity, variables);
  }
}
