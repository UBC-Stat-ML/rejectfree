package rejfree;

import java.util.Random;

import org.jblas.DoubleMatrix;

import bayonet.distributions.Uniform;

public class TestProof
{
  static double d = 3;
  
  
  public static DoubleMatrix randomOfNorm(double norm) 
  {
    DoubleMatrix result = DoubleMatrix.randn(3);
    return result.mul(norm/result.norm2());
  }
  
  public static void main(String [] args)
  {
    
    test();
    

  }
  
  public static void test() 
  {
    Random rand = new Random(1);
    org.jblas.util.Random.seed(rand.nextLong());
    double normLowerBound = 1.0/2.0;
    double epsilon = 1.0; //2.0 * rand.nextDouble();
    for (int i = 0; i < 1_000_000_000; i++)
    {
      DoubleMatrix v0 = randomOfNorm(rand.nextDouble());
      DoubleMatrix v2 = randomOfNorm(Uniform.generate(rand, normLowerBound, 1.0));
      
      DoubleMatrix x0 = randomOfNorm(Uniform.generate(rand, 0, epsilon));
      DoubleMatrix x2 = randomOfNorm(Uniform.generate(rand, 0, epsilon));
      double t0 = 24.0; //Math.max(4.0, 32.0 * epsilon / 8.0);
      double tau1 = Uniform.generate(rand, 0, t0);
      double tau2 = Uniform.generate(rand, 0, t0);
      
      double norm2 = x2.sub(  v2.mul(t0 - tau1 - tau2)  ).sub(x0).sub( v0.mul(tau1)  ).mul(1.0 / tau2).norm2();
      
      if (norm2 * norm2 > 1.0)
        throw new RuntimeException("Found counter example of norm " + norm2 * norm2);
    }
  }
  
  public static void bad() 
  {
    Random rand = new Random(1);
    org.jblas.util.Random.seed(rand.nextLong());
    double normLowerBound = 1.0/2.0;
    for (int i = 0; i < 1_000_000_000; i++)
    {
      DoubleMatrix v0 = randomOfNorm(rand.nextDouble());
      DoubleMatrix v2 = randomOfNorm(Uniform.generate(rand, normLowerBound, 1.0));
      double epsilon = 2.0 * rand.nextDouble();
      DoubleMatrix x0 = randomOfNorm(Uniform.generate(rand, 0, epsilon));
      DoubleMatrix x2 = randomOfNorm(Uniform.generate(rand, 0, epsilon));
      double t0 = 24.0 * epsilon;
      double tau1 = Uniform.generate(rand, 0, t0 / 24.0);
      double tau2 = Uniform.generate(rand, 0, t0 * (1.0 - 1.0/24.0));
      
      double norm2 = x2.sub(  v2.mul(t0 - tau1 - tau2)  ).sub(x0).sub( v0.mul(tau1)  ).mul(1.0 / tau2).norm2();
      
      if (norm2 * norm2 > 1.0)
        throw new RuntimeException("Found counter example of norm " + norm2 * norm2);
    }
  }
}


