package ca.ubc.pdmp;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import static ca.ubc.rejfree.StaticUtils.*;

/**
 * A Piecewise deterministic Markov Process.
 * 
 * @author bouchard
 *
 */
public class PDMP
{
  public final List<JumpProcess> jumpProcesses = new ArrayList<>();
  public final List<Coordinate> coordinates;
  public final List<Processor> processors = new ArrayList<>();
  
  public PDMP(List<? extends Coordinate> coordinates)
  {
    if (!isSet(coordinates))
      error();
    this.coordinates = ImmutableList.copyOf(coordinates);
  }
}