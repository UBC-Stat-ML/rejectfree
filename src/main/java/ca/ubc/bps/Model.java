package ca.ubc.bps;

import blang.inits.Implementations;
import ca.ubc.bps.BPSFactory.ModelBuildingContext;
import ca.ubc.bps.models.FixedPrecisionNormalModel;

@Implementations({FixedPrecisionNormalModel.class})
public interface Model
{
  public void setup(ModelBuildingContext context, boolean initializeStatesFromStationary);
}