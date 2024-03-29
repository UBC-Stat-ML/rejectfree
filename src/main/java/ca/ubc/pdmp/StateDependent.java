package ca.ubc.pdmp;

import java.util.Collection;

/**
 * A function, distribution or kernel that depends on the current state of the
 * PDMP. 
 * 
 * @author bouchard
 */
public interface StateDependent
{
  /**
   * 
   * @return The variables that need to be up to date to compute the present 
   * distribution, function or kernel.
   */
  Collection<? extends Coordinate> requiredVariables();
}