package ca.ubc.bps;

import java.util.List;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import blang.inits.Implementations;
import ca.ubc.bps.RefreshmentFactory.NormDependent;
import ca.ubc.bps.RefreshmentFactory.Standard;
import ca.ubc.bps.RefreshmentFactory.NoRefreshment;
import ca.ubc.bps.kernels.IndependentRefreshment;
import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.timers.Intensity;
import ca.ubc.bps.timers.UnimodalTimer;
import ca.ubc.pdmp.JumpProcess;
import ca.ubc.pdmp.PDMP;
import static xlinear.MatrixExtensions.*;
import static xlinear.MatrixOperations.*;


@Implementations({NormDependent.class, Standard.class, NoRefreshment.class})
@FunctionalInterface
public interface RefreshmentFactory
{
   public void addRefreshment(PDMP pdmp);
   
   public static class Standard implements RefreshmentFactory 
   {
     @Arg(description = "Global rate of refreshment")
     @DefaultValue("1.0")
     public double rate = 1.0;
     
     @Arg(description = "Use local refreshment?")
     @DefaultValue("true")
     public boolean local = true;
     
     @Override
     public void addRefreshment(final PDMP pdmp) {
       if (this.local) {
         List<ContinuouslyEvolving> _continuousCoordinates = StaticUtils.continuousCoordinates(pdmp.coordinates);
         int _size = _continuousCoordinates.size();
         double _divide = (this.rate / ((double) _size));
         Refreshments.addLocal(pdmp, _divide);
       } else {
         Refreshments.addGlobal(pdmp, this.rate);
       }
     }
   }
   
   public static class NoRefreshment implements RefreshmentFactory {
    @Override
    public void addRefreshment(PDMP pdmp)
    {
      
    }
   }
   
   public static class NormDependent implements RefreshmentFactory {
     @Arg
     @DefaultValue("0.5")
     public double power = 0.5;
     
     private Intensity normPotential() {
       return (ContinuousStateDependent state, double deltaTime) -> {
         double [] velocity = state.extrapolateVelocity(deltaTime);
         return 1.0 + Math.pow(norm(denseCopy(velocity)), power);
       };
     }
     
     @Override
     public void addRefreshment(final PDMP pdmp) {
       final List<ContinuouslyEvolving> continuousCoordinates = StaticUtils.continuousCoordinates(pdmp.coordinates);
           
       pdmp.jumpProcesses.add(
           new JumpProcess(
               new UnimodalTimer( 
                   continuousCoordinates, 
                   normPotential()),               
               new IndependentRefreshment(continuousCoordinates)));
     }
   }
}