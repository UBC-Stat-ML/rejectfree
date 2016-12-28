package ca.ubc.pdmp;

import java.util.List;

public abstract class StateDependentBase implements StateDependent
{
  protected final List<? extends Coordinate> requiredVariables;
  
  public StateDependentBase(List<? extends Coordinate> requiredVariables)
  {
    this.requiredVariables = requiredVariables;
  }

  @Override
  public final List<? extends Coordinate> requiredVariables()
  {
    return requiredVariables;
  }
}