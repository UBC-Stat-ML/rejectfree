package ca.ubc.pdmp;

import java.util.Random;

/**
 * 
 * @author bouchard
 */
public interface JumpKernel extends StateDependent
{
  void simulate(Random random);
}