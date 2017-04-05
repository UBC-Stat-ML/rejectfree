package ca.ubc.pdmp;

import static ca.ubc.bps.BPSStaticUtils.*;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * A Piecewise deterministic Markov Process.
 * 
 * @author bouchard
 *
 */
public class PDMP
{
  public final List<JumpProcess> jumpProcesses = new ArrayList<>();
  public final ImmutableList<Coordinate> coordinates;
  public final List<Processor> processors = new ArrayList<>();
  
  public PDMP(List<? extends Coordinate> coordinates)
  {
    if (!isSet(coordinates))
      error();
    this.coordinates = ImmutableList.copyOf(coordinates);
  }
}