package rejfree;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import org.jblas.DoubleMatrix;

import bayonet.opt.DifferentiableFunction;
import rejfree.RFSamplerOptions.RefreshmentMethod;
import rejfree.global.GlobalRFSampler;
import rejfree.models.normal.NormalEnergy;

public class NewEstimator2 
{
  private static DoubleMatrix covar = new DoubleMatrix(new double[][]{{1.0,0},{0,1.0}});
  private static DifferentiableFunction energy = NormalEnergy.withCovariance(covar);
  private static Random rand = new Random(1);
  private static RFSamplerOptions options = new RFSamplerOptions();

  public static void main(String [] args)
  {
    options.collectRate = 0.0;
    options.refreshmentMethod = RefreshmentMethod.GLOBAL;
    GlobalRFSampler sampler = new GlobalRFSampler(energy, new DoubleMatrix(2));
    sampler.iterate(rand, 1_000_000);
    
    for (String testFctName : testFunctions.keySet()) 
    {
      System.out.println("Test function: " + testFctName);
      Function<Double,Double> testFct = testFunctions.get(testFctName);
      
      for (String weightFctName : weigthFunctions.keySet())
      {
        WeightFunction weightFct = weigthFunctions.get(weightFctName);
        System.out.println("\t" + weightFctName + "\t" + importanceSamplingEstimator(weightFct, testFct, sampler));
      }
    }
  }
  
  public static double importanceSamplingEstimator(WeightFunction weight, Function<Double,Double> f, GlobalRFSampler sampler)
  {
    return weightedSum(weight, f, sampler) 
            / 
           weightedSum(weight, (Double d) -> 1.0, sampler);
  }
  
  public static double weightedSum(WeightFunction weight, Function<Double,Double> f, GlobalRFSampler sampler)
  {
    double sum = 0.0;
    for (int i = 1; i < sampler.eventPositions.size() - 1; i++) 
    {
      sum += weight.weight(sampler, i) * f.apply(sampler.eventPositions.get(i).get(0));
    }
    return sum;
  }
  
  public static interface WeightFunction
  {
    public double weight(GlobalRFSampler sampler, int i);
  }
  
  public static Map<String,WeightFunction> weigthFunctions = buildWeightFunctions();
  
  private static Map<String, WeightFunction> buildWeightFunctions()
  {
    Map<String, WeightFunction> result = new LinkedHashMap<>();
    
    result.put("FromTheorem1", (GlobalRFSampler sampler, int i) -> {
      DoubleMatrix position = sampler.eventPositions.get(i);
      DoubleMatrix velocity = sampler.eventVelocities.get(i);
      DoubleMatrix gradient = new DoubleMatrix(energy.derivativeAt(position.data));
      return 1.0 / lambda(position, StaticUtils.bounce(velocity, gradient));
    });
    
    result.put("FromTheorem2", (GlobalRFSampler sampler, int i) -> {
      DoubleMatrix position = sampler.eventPositions.get(i + 1);
      DoubleMatrix velocity = sampler.eventVelocities.get(i);
      return 1.0 / lambda(position, velocity);
    });
    
    result.put("Guess", (GlobalRFSampler sampler, int i) -> {
      DoubleMatrix position = sampler.eventPositions.get(i);
      DoubleMatrix velocity = sampler.eventVelocities.get(i);
      return 1.0 / lambda(position, velocity);
    });
    
    return result;
  }
  
  public static Map<String,Function<Double,Double>> testFunctions = buildTestFunctions();
  
  private static Map<String, Function<Double, Double>> buildTestFunctions()
  {
    Map<String, Function<Double, Double>> result = new LinkedHashMap<>();
    for (int i = 1; i < 5; i++) 
    {
      final int power = i;
      result.put("x^" + i + "", (Double x) -> Math.pow(x, power));
    }
    return result;
  }
  
  public static double lambda(DoubleMatrix position, DoubleMatrix velocity)
  {
    DoubleMatrix gradient = new DoubleMatrix(energy.derivativeAt(position.data));
    return options.refreshRate + Math.max(0, velocity.dot(gradient));
  }
}
