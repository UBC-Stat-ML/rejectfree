package ca.ubc.pdmp;

import java.util.Random;

/**
 * A simulator for the time of the next event. The two main uses are:
 * (1) A Poisson process.
 * (2) A deterministic time to hit a boundary.
 * 
 * @author bouchard
 */
public interface Clock extends StateDependent
{
  /**
   * 
   * @param random
   * @return
   */
  DeltaTime next(Random random);
}