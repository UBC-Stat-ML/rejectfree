package ca.ubc.bps.timers;

import java.util.Collection;
import java.util.Random;

import ca.ubc.bps.BPSStaticUtils;
import ca.ubc.bps.state.PositionVelocityDependent;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

/**
 * An adaptive thinning timer **assuming that for a bounded time interval, the max of the 
 * two end-points bounds the function**. This holds for example when the intensity function is 
 * has a single global connected region of minimum intensity. 
 * 
 * These cases could often (always?) be solved using the more general NumericalUnimodalInversionTimer but 
 * the present was found to be faster in the cases observed (see correctnessGeneralizedGaussian). 
 * However there are cases where the present does not apply and the NumericalUnimodalInversionTimer solver 
 * is needed (for example, generalized gaussians with alpha < 0).
 * 
 * @author bouchard
 *
 */
public class ConstantIntensityAdaptiveThinning extends PositionVelocityDependent implements Clock
{
  private Random jitter = jitterProvider.get();
  private double currentInitialStepSize = INITIAL_STEP + jitter.nextDouble();
  private Intensity intensity;
  
  public ConstantIntensityAdaptiveThinning(Collection<? extends Coordinate> requiredVariables, Intensity intensity)
  {
    super(requiredVariables);
    this.intensity = intensity;
  }

  @Override
  public DeltaTime next(Random random)
  {
    // compute the adaptive bound
    Bound bound = new Bound(currentInitialStepSize, intensity.evaluate(0.0));
    if      (bound.expectedNPoints < LOW_THRESHOLD)
      bound = tryIterativeGrow(bound);
    else if (bound.expectedNPoints > HIGH_THRESHOLD)
      bound = iterativeShrink(bound);
    
    // sample using this bound
    final double boundRate = bound.rate;
    final double stepSize = bound.stepSize;
    if (boundRate == 0.0)
      return DeltaTime.isGreaterThan(stepSize);
    double sample = BPSStaticUtils.sampleExponential(random, boundRate);
    if (sample > stepSize)
      return DeltaTime.isGreaterThan(stepSize);
    double ratio = intensity.evaluate(sample) / boundRate;
    if (BPSStaticUtils.sampleBernoulli(random, ratio))
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
      this.rate = Math.max(currentPotential, intensity.evaluate(stepSize));
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
  
  private static ThreadLocal<Random> jitterProvider = new ThreadLocal<Random>() 
  {
    @Override protected Random initialValue() { return new Random(33844); }
  };
}
