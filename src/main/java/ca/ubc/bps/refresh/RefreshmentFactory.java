package ca.ubc.bps.refresh;

import java.util.Collection;
import java.util.List;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import blang.inits.Implementations;
import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.refresh.RefreshmentFactory.NoRefreshment;
import ca.ubc.bps.refresh.RefreshmentFactory.NormDependent;
import ca.ubc.bps.refresh.RefreshmentFactory.Standard;
import ca.ubc.bps.refresh.RefreshmentFactory.Local;
import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.timers.HomogeneousPP;
import ca.ubc.bps.timers.Intensity;
import ca.ubc.bps.timers.ConstantIntensityAdaptiveThinning;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.JumpProcess;
import ca.ubc.pdmp.PDMP;

import static ca.ubc.bps.BPSStaticUtils.continuousCoordinates;
import static java.util.Collections.singleton;
import static xlinear.MatrixExtensions.*;
import static xlinear.MatrixOperations.*;


@Implementations({NormDependent.class, Standard.class, NoRefreshment.class, Local.class})
public interface RefreshmentFactory
{
  public abstract void addRefreshment(PDMP pdmp);
  
  public static class Standard implements RefreshmentFactory
  {
    @Arg(description = "Global rate of refreshment")
    @DefaultValue("1.0")
    public double rate = 1.0;
    
    @Arg(description = "Normalize to unit norm")
    @DefaultValue("false")
    public boolean normalized = false;

    @Override
    public void addRefreshment(final PDMP pdmp) 
    {
      addGlobal(pdmp, this.rate, normalized);
    }
  }
  
  public static class Local implements RefreshmentFactory
  {
    @Arg(description = "Global rate of refreshment")
    @DefaultValue("1.0")
    public double rate = 1.0;
    
    @Override
    public void addRefreshment(final PDMP pdmp) 
    {
      List<ContinuouslyEvolving> _continuousCoordinates = BPSStaticUtils.continuousCoordinates(pdmp.coordinates);
      int _size = _continuousCoordinates.size();
      double _divide = (this.rate / ((double) _size));
      addLocal(pdmp, _divide);
    }
  }
  
  public static class NoRefreshment implements RefreshmentFactory 
  {
    @Override
    public void addRefreshment(PDMP pdmp) {}
  }
  
  public static class NormDependent implements RefreshmentFactory 
  {
    @Arg
    @DefaultValue("0.5")
    public double power = 0.5;
    
    @Arg(description = "Normalize to unit norm")
    @DefaultValue("false")
    public boolean normalized = false;
    
    @Override
    public void addRefreshment(final PDMP pdmp) 
    {
      final List<ContinuouslyEvolving> continuousCoordinates = BPSStaticUtils.continuousCoordinates(pdmp.coordinates);
          
      pdmp.jumpProcesses.add(
          new JumpProcess(
              new ConstantIntensityAdaptiveThinning( 
                  continuousCoordinates, 
                  new NormPotential(continuousCoordinates)),               
              new IndependentRefreshment(continuousCoordinates, normalized)));
    }
    
    private class NormPotential extends ContinuousStateDependent implements Intensity
    {
      private NormPotential(Collection<? extends Coordinate> requiredVariables)
      {
        super(requiredVariables);
      }
      @Override
      public double evaluate(double delta)
      {
        double [] velocity = extrapolateVelocity(delta);
        return 1.0 + Math.pow(norm(denseCopy(velocity)), power);
      }
    }
  }
  
  public static void addGlobal(PDMP pdmp, double rate, boolean normalized)
  {
    add(pdmp, rate, continuousCoordinates(pdmp.coordinates), normalized);
  }
  
  public static void addLocal(PDMP pdmp, double rate)
  {
    for (ContinuouslyEvolving coordinate : continuousCoordinates(pdmp.coordinates))
      add(pdmp, rate, singleton(coordinate), false);
  }
  
  public static void add(
      PDMP pdmp, 
      double rate, 
      Collection<ContinuouslyEvolving> continuousCoordinates, 
      boolean normalized)
  {
    pdmp.jumpProcesses.add(
        new JumpProcess(
            new HomogeneousPP(rate), 
            new IndependentRefreshment(continuousCoordinates, normalized)));
  }
}