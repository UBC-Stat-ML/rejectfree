package ca.ubc.pdmp;

/**
 * PDMP simulation will stop when any one of the following occurs:
 * 
 * - the stochastic process' time is equal to stochasticProcessTime (not to be confused with wall clock time)
 * - the wall clock time spent performing the simulation exceeds wallClockTimeMilliseconds
 * - the number of times a candiate has been polled from the queue equals to numberOfQueuePolls
 * 
 * @author bouchard
 *
 */
public class StoppingCriterion
{
  public double stochasticProcessTime = Double.POSITIVE_INFINITY;
  public long wallClockTimeMilliseconds = Long.MAX_VALUE;
  public long numberOfQueuePolls = Long.MAX_VALUE;

  public static StoppingCriterion byStochasticProcessTime(double stochasticProcessTime)
  {
    StoppingCriterion result = new StoppingCriterion();
    result.stochasticProcessTime = stochasticProcessTime;
    return result;
  }
  
  public static StoppingCriterion byWallClockTimeMilliseconds(long wallClockTime)
  {
    StoppingCriterion result = new StoppingCriterion();
    result.wallClockTimeMilliseconds = wallClockTime;
    return result;
  }
  
  public static StoppingCriterion byNumberOfQueuePolls(long nQueuePolls)
  {
    StoppingCriterion result = new StoppingCriterion();
    result.numberOfQueuePolls = nQueuePolls;
    return result;
  }
}