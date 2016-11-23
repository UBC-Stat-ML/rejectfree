package rejfree.logistic;

import java.util.Random;


import bayonet.distributions.Bernoulli;
import bayonet.distributions.Multinomial;
import bayonet.math.NumericalUtils;
import briefj.collections.Counter;
import cern.colt.Arrays;

public class TreeDPD implements DiscreteProbabilityDistribution
{
  private final double [][] probabilities;
  
  public TreeDPD(int size)
  {
    probabilities = new double[1 + (int) Math.ceil(Math.log(size)/Math.log(2))][];
    for (int level = nLevels() - 1; level >= 0; level--)
    {
      probabilities[level] = new double[size];
      size = (int) Math.ceil(((double) size) / 2.0);
    }
  }

  @Override
  public int size()
  {
    return probabilities[nLevels() - 1].length;
  }
  
  private int nLevels()
  {
    return probabilities.length;
  }

  @Override
  public int sample(Random rand)
  {
    return sample(rand, 0, 0);
  }
  
  private int sample(Random rand, int level, int position)
  {
    if (level == nLevels() - 1)
      return position;
    
    double conditionalPr = probabilities[level + 1][position * 2] / probabilities[level][position];
    int nextPosition = position * 2 + (Bernoulli.generate(rand, conditionalPr) ? 0 : 1);
    return sample(rand, level + 1, nextPosition);
  }

  @Override
  public double normalization()
  {
    return probabilities[0][0];
  }

  @Override
  public void update(int entry, double newValue)
  {
    if (newValue < 0 && !NumericalUtils.isClose(newValue, 0.0, NumericalUtils.THRESHOLD))
      throw new RuntimeException();
    
    final double delta = newValue - probabilities[nLevels() - 1][entry];
    
    for (int level = nLevels() - 1; level >= 0; level--)
    {
      probabilities[level][entry] += delta;
      entry = entry / 2;
    }
  }
  
  public static void main(String [] args)
  {
    Random rand = new Random(1);
    int size = 5;
    TreeDPD t = new TreeDPD(size);
    t.size();
    double [] direct = new double[size];
    for (int i = 0; i < size; i++)
    {
      double v = rand.nextDouble();
      direct[i] = v;
      t.update(i, v);
    }
    
    Multinomial.normalize(direct);
    System.out.println(Arrays.toString(direct));
    Counter<Integer> c = new Counter<>();
    for (int i = 0; i < 1_000_000; i++)
      c.incrementCount(t.sample(rand), 1.0);
    c.normalize();
    System.out.println(c);
    
    System.out.println("---");
    
    for (int i = 0; i < size; i++)
    {
      double v = rand.nextDouble();
      direct[i] = v;
      t.update(i, v);
    }
    
    Multinomial.normalize(direct);
    System.out.println(Arrays.toString(direct));
    c = new Counter<>();
    for (int i = 0; i < 1_000_000; i++)
      c.incrementCount(t.sample(rand), 1.0);
    c.normalize();
    System.out.println(c);
  }

}
