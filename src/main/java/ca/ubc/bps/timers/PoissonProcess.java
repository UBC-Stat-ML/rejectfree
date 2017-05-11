package ca.ubc.bps.timers;

import java.util.Random;

import ca.ubc.pdmp.DeltaTime;

/**
 * Similar to Timer, but different since also need to 
 * specify intensity pointwise. Note we do extend Timer to avoid extending StateDependent 
 * but we use the same name to be compatible.
 */
public interface PoissonProcess extends Intensity
{
  DeltaTime next(Random random);
}
