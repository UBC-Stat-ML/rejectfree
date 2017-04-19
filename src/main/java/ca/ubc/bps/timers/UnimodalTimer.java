package ca.ubc.bps.timers;

import java.util.Collection;
import java.util.Random;

import bayonet.distributions.Bernoulli;
import bayonet.distributions.Exponential;
import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

/**
 * An adaptive thinning timer **assuming that for a bounded time interval, the max of the 
 * two end-points bounds the function**. This holds for example when the intensity function is 
 * has a single global connected region of minimum intensity. 
 * @author bouchard
 *
 */
public class UnimodalTimer extends ContinuousStateDependent implements Clock
{
  // Jitter is used to avoid having several event with exact same time in the queue
  private static final Random jitterRandom = new Random(1);
  
  private double currentInitialStepSize = INITIAL_STEP + jitterRandom.nextDouble();
  private Intensity intensity;
  
  public UnimodalTimer(Collection<? extends Coordinate> requiredVariables, Intensity intensity)
  {
    super(requiredVariables);
    this.intensity = intensity;
  }

  @Override
  public DeltaTime next(Random random)
  {
    // compute the adaptive bound
    Bound bound = new Bound(currentInitialStepSize, intensity.evaluate(this, 0.0));
    if      (bound.expectedNPoints < LOW_THRESHOLD)
      bound = tryIterativeGrow(bound);
    else if (bound.expectedNPoints > HIGH_THRESHOLD)
      bound = iterativeShrink(bound);
    
    // sample using this bound
    final double boundRate = bound.rate;
    final double stepSize = bound.stepSize;
    if (boundRate == 0.0)
      return DeltaTime.isGreaterThan(stepSize);
    double sample = Exponential.generate(random, boundRate);
    if (sample > stepSize)
      return DeltaTime.isGreaterThan(stepSize);
    double ratio = intensity.evaluate(this, sample) / boundRate;
    if (Bernoulli.generate(random, ratio))
      return DeltaTime.isEqualTo(sample);
    else
      return DeltaTime.isGreaterThan(sample);
  }
  
  private Bound tryIterativeGrow(Bound bound)
  {
    currentInitialStepSize *= SHRINK_GROW_FACTOR;
    Bound prev = null;
    for (int iter = 0; iter < MAX_INTER; iter++) 
    {
      prev = bound;
      bound = bound.grow();
      if (bound.expectedNPoints >= LOW_THRESHOLD && 
          bound.expectedNPoints <= HIGH_THRESHOLD)
        return bound;
      if (bound.expectedNPoints > HIGH_THRESHOLD)
        return prev; // overshot
    }
    return bound;
  }

  private Bound iterativeShrink(Bound bound)
  {
    currentInitialStepSize /= SHRINK_GROW_FACTOR;
    for (int iter = 0; iter < MAX_INTER; iter++) 
    {
      bound = bound.shrink();
      if (bound.expectedNPoints <= HIGH_THRESHOLD)
        return bound;
    }
    return bound;
  }

  private class Bound
  {
    private final double stepSize;
    private final double rate;
    private final double expectedNPoints;
    private final double currentPotential;
    private Bound(double stepSize, double currentPotential)
    {
      this.currentPotential = currentPotential;
      this.stepSize = stepSize;
      this.rate = Math.max(currentPotential, intensity.evaluate(UnimodalTimer.this, stepSize));
      this.expectedNPoints = rate * stepSize;
    }
    private Bound shrink()
    {
      return new Bound(stepSize / SHRINK_GROW_FACTOR, currentPotential);
    }
    private Bound grow()
    {
      return new Bound(stepSize * SHRINK_GROW_FACTOR, currentPotential);
    }
  }
  
  private static final double INITIAL_STEP = 1.0;
  
  private static final double LOW_THRESHOLD  = 0.25;
  private static final double HIGH_THRESHOLD = 4.0;
  
  private static final int MAX_INTER = 5;
  
  private static final int SHRINK_GROW_FACTOR = 2;
}
