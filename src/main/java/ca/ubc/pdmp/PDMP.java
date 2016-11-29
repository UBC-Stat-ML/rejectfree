package ca.ubc.pdmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * A Piecewise deterministic Markov Process.
 * 
 * 
 * 
 * @author bouchard
 *
 */
public class PDMP
{
  /**
   * Check there are no identical objects in each list, then copy them 
   * to lists for fast indexing.
   * 
   * @param jumpProcesses
   * @param dynamics
   * @param processors
   */
  public PDMP(
      Collection<JumpProcess> jumpProcesses,
      Collection<Coordinate> dynamics,
      Collection<Processor> processors)
  {
    this.jumpProcesses = toListEnsureElementsUniques(jumpProcesses);
    this.dynamics = toListEnsureElementsUniques(dynamics);
    this.processors = toListEnsureElementsUniques(processors);
  }
  
  final List<JumpProcess> jumpProcesses;
  final List<Coordinate> dynamics;
  final List<Processor> processors;
  
  static <T> List<T> toListEnsureElementsUniques(Collection<T> collection)
  {
    LinkedHashSet<T> copy = new LinkedHashSet<>(collection);
    if (copy.size() != collection.size())
      throw new RuntimeException("Make sure duplicate elements are not present.");
    return new ArrayList<>(collection);
  }
}