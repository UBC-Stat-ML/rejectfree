package ca.ubc.pdmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * A Piecewise deterministic Markov Process.
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
   * @param coordinates
   * @param processors
   */
  @SuppressWarnings("unchecked")
  public PDMP(
      Collection<? extends JumpProcess> jumpProcesses,
      Collection<? extends Coordinate> coordinates,
      Collection<? extends Processor> processors)
  {
    this.jumpProcesses = (List<JumpProcess>) toListEnsureElementsUniques(jumpProcesses);
    this.coordinates = (List<Coordinate>) toListEnsureElementsUniques(coordinates);
    this.processors = (List<Processor>) toListEnsureElementsUniques(processors);
  }
  
  final List<JumpProcess> jumpProcesses;
  final List<Coordinate> coordinates;
  final List<Processor> processors;
  
  private static <T> List<T> toListEnsureElementsUniques(Collection<T> collection)
  {
    LinkedHashSet<T> copy = new LinkedHashSet<>(collection);
    if (copy.size() != collection.size())
      throw new RuntimeException("Make sure duplicate elements are not present.");
    return new ArrayList<>(collection);
  }
}