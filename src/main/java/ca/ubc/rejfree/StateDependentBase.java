package ca.ubc.rejfree;

import java.util.List;

import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.StateDependent;

// add 
public abstract class StateDependentBase implements StateDependent
{
  protected final List<Coordinate> requiredVariables;
  
  public StateDependentBase(List<Coordinate> requiredVariables)
  {
    this.requiredVariables = requiredVariables;
  }
}