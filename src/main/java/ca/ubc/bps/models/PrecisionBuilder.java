package ca.ubc.bps.models;

import blang.inits.Implementations;
import xlinear.Matrix;

@Implementations({DiagonalPrecision.class, BrownianMotion.class, RadfordNealExample.class})
public interface PrecisionBuilder
{
  public Matrix build();
}