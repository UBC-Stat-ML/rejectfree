package ca.ubc.pdmp;

import java.util.ArrayList;
import java.util.List;

/**
 * A Piecewise deterministic Markov Process.
 * 
 * @author bouchard
 *
 */
public class PDMP
{
  public final List<JumpProcess> jumpProcesses = new ArrayList<>();
  public final List<Coordinate> coordinates = new ArrayList<>();
  public final List<Processor> processors = new ArrayList<>();
}