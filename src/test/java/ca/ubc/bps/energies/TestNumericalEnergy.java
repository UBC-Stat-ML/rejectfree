package ca.ubc.bps.energies;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import cern.colt.Arrays;
import xlinear.DenseMatrix;
import xlinear.MatrixOperations;

public class TestNumericalEnergy 
{
  @Test
  public void testIt() 
  {
    DenseMatrix precision = MatrixOperations.denseCopy(new double[][] {
      {2.3, 5.1},
      {5.1, 1.3}
    });
    NormalEnergy ne = new NormalEnergy(precision);
    NumericalEnergy test = new NumericalEnergy() {
      
      @Override
      public double valueAt(double[] point) {
        return ne.valueAt(point);
      }
    };
    
    Random rand = new Random(1);
    
    for (int i = 0; i < 1000; i++) 
    {
      double [] point = new double[] { rand.nextDouble(), rand.nextDouble() };
      double [] expected = ne.gradient(point);
      double [] actual = test.gradient(point);
      for (int j = 0; j < 2; j++)
        Assert.assertEquals(expected[j], actual[j], 1e-1);
    }
    
  }
}
