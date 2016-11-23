package rejfree.logistic;

import java.util.Random;

public interface DiscreteProbabilityDistribution
{
  public int size();
  public int sample(Random rand);
  public double normalization();
  public void update(int entry, double newValue);
}
