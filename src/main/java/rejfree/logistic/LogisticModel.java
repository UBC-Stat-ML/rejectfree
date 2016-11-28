package rejfree.logistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.jblas.DoubleMatrix;

import bayonet.distributions.Bernoulli;
import bayonet.distributions.Exponential;
import bayonet.distributions.Multinomial;
import bayonet.math.SpecialFunctions;
import blang.annotations.DefineFactor;
import blang.annotations.FactorComponent;
import blang.factors.FactorList;
import blang.variables.RealVariable;
import briefj.opt.Option;
import rejfree.local.CollisionContext;
import rejfree.local.CollisionFactor;
import rejfree.local.LocalRFRunner;

public class LogisticModel
{
  @Option
  public int nDatapoints = 100;
  
  @Option
  public int nDimensions = 5;
  
  @Option
  public Random dataGenRandom = new Random(1);
  
  public List<RealVariable> variables;
  
  public Object getModelSpec()
  {
    generateData();
    variables = new ArrayList<>();
    for (int d = 0; d < nDimensions; d++)
      variables.add(new RealVariable(0.0));
    return new LocalFactorModelSpec();
  }
  
  private void generateData()
  {
    if (trueParams == null)
      org.jblas.util.Random.seed(dataGenRandom.nextLong());
    
    labels = new ArrayList<>();
    covariates = new ArrayList<>();
    
    // generate params
    trueParams = DoubleMatrix.randn(nDimensions);
    
    // generate covariates and labels
    for (int r = 0; r < nDatapoints; r++)
    {
      DoubleMatrix covariate = nextRandomCovariate();
      boolean isOne = Bernoulli.generate(dataGenRandom, SpecialFunctions.logistic(covariate.dot(trueParams)));
      covariates.add(covariate);
      labels.add(isOne ? 1 : 0);
    }
  }
  
  // generated data
  public DoubleMatrix trueParams = null;
  public List<Integer> labels;
  public List<DoubleMatrix> covariates;
  
  private DoubleMatrix nextRandomCovariate()
  {
    // TODO: other distributions over covars?
    
    DoubleMatrix result = new DoubleMatrix(nDimensions);
    
    for (int d = 0; d < nDimensions; d++)
      result.put(d, Bernoulli.generate(dataGenRandom, 0.5) ? 1.0 : 0.0);
    
    return result;
  }

  private class LocalFactorModelSpec
  {
    // NB: ignore prior on parameters for now
    
    // likelihood:
    @DefineFactor
    public final List<CollisionFactor> factors;
    
    public LocalFactorModelSpec()
    {
      factors = new ArrayList<>();
      for (int r = 0; r < nDatapoints; r++)
        factors.add(new LogisticFactor(covariates.get(r), labels.get(r), variables));
    }
  }
  
  public class FastLogisticFactor implements CollisionFactor
  {
    @FactorComponent
    public final FactorList<RealVariable> variables;
    
    public FastLogisticFactor(List<RealVariable> variables)
    {
      this.variables = FactorList.ofArguments(variables, true);
      mus = null;
      iotaSums = null;
//      setupCache(); 
    }

    @Override
    public double logDensity()
    {
      throw new RuntimeException();
    }
    
    private final double [][] iotaSums;  // {0,1} -> dim
    private final DiscreteProbabilityDistribution [][] mus; // {0, 1} -> dim
    
    private double [] ratesByDim(DoubleMatrix velocity)
    {
      double [] ratesByDim = new double[velocity.length];
      
      for (int dim = 0; dim < nDimensions; dim++)
      {
        final double v = velocity.get(dim);
        ratesByDim[dim] = Math.abs(v) * iotaSums[v < 0 ? 1 : 0][dim];
      }
      
      return ratesByDim;
    }
    
    private DoubleMatrix currentParam()
    {
      DoubleMatrix result = new DoubleMatrix(nVariables());
      for (int d = 0; d < nVariables(); d++)
        result.put(d, variables.list.get(d).getValue());
      return result;
    }
    
    DoubleMatrix nextGradient = null;

    @Override
    public Pair<Double, Boolean> getLowerBoundForCollisionDeltaTime(CollisionContext context)
    {
      DoubleMatrix v = context.velocity;
      
      final double [] ratesByDim = ratesByDim(v);
      final double boundingRate = Multinomial.normalize(ratesByDim);
      final double proposedColDelta = Exponential.generate(context.random, boundingRate);

      // sample data point
      final int proposedDim = Multinomial.sampleMultinomial(context.random, ratesByDim);
      final int proposedDatum = mus[v.get(proposedDim) < 0 ? 1 : 0][proposedDim].sample(context.random);
      
      // accept-reject for that point
      final double bound = proposalRateForOneDatum(proposedDatum, v);
      DoubleMatrix xPrime = currentParam().add(v.mul(proposedColDelta));
      nextGradient = gradient(xPrime, proposedDatum);
      double targetRate = Math.max(0.0, -nextGradient.dot(v));
      if (bound < targetRate)
        throw new RuntimeException();
      boolean accept = Bernoulli.generate(context.random, targetRate / bound);
      
      return Pair.of(proposedColDelta, accept);
    }

    private double proposalRateForOneDatum(int proposedDatum, DoubleMatrix v)
    {
      double sum = 0.0;
      
      for (int dim = 0; dim < nDimensions; dim++)
        sum += (v.get(dim) * Math.pow(-1, labels.get(proposedDatum)) >= 0 ? 1 : 0) 
          * covariates.get(proposedDatum).get(dim)
          * Math.abs(v.get(dim));
      
      return sum;
    }

    @Override
    public DoubleMatrix gradient()
    {
      return nextGradient;
    }
    
    public DoubleMatrix gradient(DoubleMatrix x, int dataIndex)
    {
      DoubleMatrix covariateVector = covariates.get(dataIndex);
      double dotProd = covariateVector.dot(x);
      return covariateVector.mul(((double) labels.get(dataIndex)) - SpecialFunctions.logistic(dotProd));
    }

    @Override
    public RealVariable getVariable(int gradientCoordinate)
    {
      return variables.list.get(gradientCoordinate);
    }

    @Override
    public int nVariables()
    {
      return variables.list.size();
    }
    
  }
  
  public static class LogisticFactor implements CollisionFactor
  {
    // assume for now to be {0, 1}
    private final DoubleMatrix covariates;
    
    // assume for now to {0, 1}
    private final int label;
    
    @FactorComponent
    public final FactorList<RealVariable> variables;
    
    public LogisticFactor(DoubleMatrix covariates, int label, List<RealVariable> variables)
    {
      this.covariates = covariates;
      this.label = label;
      this.variables = FactorList.ofArguments(variables, true);
//      this.variables = variables;
    }

    private DoubleMatrix currentParam()
    {
      DoubleMatrix result = new DoubleMatrix(nVariables());
      for (int d = 0; d < nVariables(); d++)
        result.put(d, variables.list.get(d).getValue());
      return result;
    }

    @Override
    public double logDensity()
    {
      double dotProd = covariates.dot(currentParam());
      return dotProd * label - Math.log1p(dotProd);
    }

    @Override
    public Pair<Double, Boolean> getLowerBoundForCollisionDeltaTime(CollisionContext context)
    {
      DoubleMatrix v = context.velocity;
      
      // compute bound
      double bound = 0.0;
      for (int d = 0; d < nVariables(); d++)
        if (v.get(d) * Math.pow(-1.0, label) >= 0.0)
          bound += covariates.get(d) * Math.abs(v.get(d));
      
      // simulate exp
      double deltaT = Exponential.generate(context.random, bound);
      
      // compute accept-reject
      DoubleMatrix xPrime = currentParam().add(v.mul(deltaT));
      double target = Math.max(0.0, -gradient(xPrime).dot(v));
      if (bound < target)
        throw new RuntimeException();
      boolean accept = Bernoulli.generate(context.random, target / bound);
      
      return Pair.of(deltaT, accept);
    }

    @Override
    public DoubleMatrix gradient()
    {
      return gradient(currentParam());
    }
    
    public DoubleMatrix gradient(DoubleMatrix x)
    {
      double dotProd = covariates.dot(x);
      return covariates.mul(((double) label) - SpecialFunctions.logistic(dotProd));
    }

    @Override
    public RealVariable getVariable(int gradientCoordinate)
    {
      return variables.list.get(gradientCoordinate);
    }

    @Override
    public int nVariables()
    {
      return variables.list.size();
    }
  }
  
  public static void main(String [] args)
  {
    LogisticModel lm = new LogisticModel();
    LocalRFRunner runner = new LocalRFRunner();
    runner.init(lm.getModelSpec());
    runner.run();
  }
}
