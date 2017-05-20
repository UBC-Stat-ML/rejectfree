package ca.ubc.pdmp;

import java.util.Collection;

public abstract class StateDependentBase implements StateDependent
{
  protected final Collection<? extends Coordinate> requiredVariables;
  
  public StateDependentBase(Collection<? extends Coordinate> requiredVariables)
  {
    this.requiredVariables = requiredVariables;
  }

  @Override
  public Collection<? extends Coordinate> requiredVariables()
  {
    return requiredVariables;
  }
}