package ca.ubc.rejfree;

import ca.ubc.pdmp.Coordinate;

public class PiecewiseConstant<T> implements Coordinate
{
  public final MutableObject<T> contents;
  public PiecewiseConstant(MutableObject<T> contents)
  {
    this.contents = contents;
  }
  @Override
  public void extrapolateInPlace(double deltaTime)
  {
    // nothing to do
  }
}