package ca.ubc.bps.state;

import java.util.function.DoubleFunction;

public class TransformedMutableDouble implements MutableDouble
{
  /**
   * Keep track of both bounded and unbounded.
   * Do the extrapolation computations using bounded coordinates 
   * to be able to revert moves taking you out of the plane;
   */
  private double bounded;
  private final MutableDouble unbounded;
  private final DoubleFunction<Double> bd2un, un2bd;
  
  public TransformedMutableDouble(MutableDouble unbounded, DoubleFunction<Double> bd2un, DoubleFunction<Double> un2bd)
  {
    this.unbounded = unbounded;
    this.bd2un = bd2un;
    this.un2bd = un2bd;
    this.bounded = un2bd.apply(unbounded.get());
  }
  
  public static TransformedMutableDouble hyperbolicPosition(MutableDouble unbounded)
  {
    return new TransformedMutableDouble(unbounded, Hyperbolic::toUnbCoord, Hyperbolic::toBdCoord);
  }

  @Override
  public void set(double unb)
  {
    if (Double.isNaN(unb))
      throw new RuntimeException("Use setBoundedCoordinate(..), otherwise, not able to roll back.");
    this.unbounded.set(unb);
    this.bounded = un2bd.apply(unb);
  }
  
  public void setBoundedCoordinate(double bd)
  {
    this.bounded = bd;
    this.unbounded.set(bd2un.apply(bd));  
  }

  /**
   * This could be NaN if out of bound.
   */
  @Override
  public double get()
  {
    return unbounded.get();
  }
  
  public double getBounded()
  {
    return bounded;
  }
  
  public boolean inBound()
  {
    return !Double.isNaN(unbounded.get());
  }
}
