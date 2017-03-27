package ca.ubc.bps;

import blang.inits.Implementations;
import ca.ubc.bps.BPSFactory.ModelBuildingContext;
import ca.ubc.bps.models.FixedPrecisionNormalModel;
import ca.ubc.bps.models.GeneralizedNormalModel;

@Implementations({FixedPrecisionNormalModel.class, GeneralizedNormalModel.class})
public interface Model
{
  public void setup(ModelBuildingContext context, boolean initializeStatesFromStationary);
}