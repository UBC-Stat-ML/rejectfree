package ca.ubc.rejfree;

import ca.ubc.pdmp.Coordinate;

public class PiecewiseConstant<T> implements Coordinate
{
  public final MutableObject<T> contents;
  public PiecewiseConstant(MutableObject<T> contents)
  {
    super();
    this.contents = contents;
  }
  @Override
  public void extrapolate(double deltaTime)
  {
    // nothing to do
  }
}