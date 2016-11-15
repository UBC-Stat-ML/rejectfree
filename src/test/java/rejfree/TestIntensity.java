package rejfree;

import org.jblas.DoubleMatrix;
import org.jblas.util.Random;

import briefj.OutputManager;
import briefj.run.Mains;
import briefj.run.Results;
import rejfree.local.LocalRFRunner;
import rejfree.scalings.EstimateESSByDimensionality;
import rejfree.scalings.EstimateESSByDimensionality.ModelSpec;

public class TestIntensity implements Runnable
{
  public void run() 
  {
    OutputManager manager = new OutputManager();
    manager.setOutputFolder(Results.getResultFolder());
    Random.seed(1001);
    for (int rep = 0; rep < 100_000; rep++)
    {
      for (int d = 1000; d <= 10_000; d *= 2) 
      {
        DoubleMatrix v = DoubleMatrix.randn(d);
        DoubleMatrix x = DoubleMatrix.randn(d);
        double intensity = Math.max(0, x.dot(v));
        
        manager.write("results",
            "rep", rep,
            "d", d,
            "intensity", intensity);
      }
    }
    manager.close();
  }
  
  public static void main(String [] args) 
  {
    Mains.instrumentedRun(args, new TestIntensity());
  }

}
