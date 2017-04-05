package ca.ubc.bps.models;

import blang.inits.Implementations;
import ca.ubc.bps.factory.BPSFactory.ModelBuildingContext;

@Implementations({FixedPrecisionNormalModel.class, GeneralizedNormalModel.class})
public interface Model
{
  public void setup(ModelBuildingContext context, boolean initializeStatesFromStationary);
}