package ca.ubc.sparsenets;


import blang.inits.experiments.Experiment;
import ca.ubc.bps.factory.BPSFactory;
import ca.ubc.bps.factory.InitializationStrategy.Far;
import ca.ubc.bps.refresh.RefreshmentFactory;

public class SparseNetMain extends BPSFactory
{
  @Override
  public BPS buildAndRun() 
  {
    if (!(initialization instanceof Far))
      throw new RuntimeException();
    
    if (((Far) initialization).valueForEachPositionCoordinate <= 0.0)
      throw new RuntimeException();
    
    // hack: we don't want to build the refreshment in the standard way
    ((SparseNetworkModel) model).refreshment = this.refreshment;
    this.refreshment = new RefreshmentFactory.NoRefreshment();
    
    BPS result = buildBPS();
    ((SparseNetworkModel) model).initSum();
    result.run();
    result.writeRequestedResults();
    return result;
  }
  
  public static void main(String [] args)
  {
    Experiment.startAutoExit(args);
  }
}
