package ca.ubc.bps.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import blang.inits.Implementations;
import ca.ubc.bps.BPSFactory.Model;
import ca.ubc.bps.BPSFactory.ModelBuildingContext;
import ca.ubc.bps.energies.NormalEnergy;
import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.bps.timers.NormalClock;
import xlinear.DenseMatrix;
import xlinear.Matrix;
import xlinear.MatrixExtensions;
import xlinear.MatrixOperations;
import xlinear.SparseMatrix;
import xlinear.StaticUtils;

public class FixedPrecisionNormalModel implements Model
{
  @Arg @DefaultValue("DiagonalPrecision")
  public PrecisionBuilder precision = new DiagonalPrecision();
  
  @Arg @DefaultValue("true")
  public boolean useLocal = true;
  
  @Implementations({DiagonalPrecision.class})
  public static interface PrecisionBuilder
  {
    public Matrix build();
  }
  
  public static class DiagonalPrecision implements PrecisionBuilder
  {
    @Arg @DefaultValue("2")
    public int size = 2;

    @Override
    public Matrix build()
    {
      return MatrixOperations.identity(size);
    }
  }

  @Override
  public void setup(ModelBuildingContext context, boolean initializeToStationary)
  {
    Matrix precisionMatrix = precision.build();
    List<ContinuouslyEvolving> vars = context.buildAndRegisterContinuouslyEvolvingStates(precisionMatrix.nCols());
    if (initializeToStationary)
      initializeToStationary(vars, precisionMatrix, context.initializationRandom);
    if (useLocal && precisionMatrix.nCols() > 2)
      setupLocal(context, precisionMatrix, vars);
    else
      context.registerBPSPotential(bpsPotential(precisionMatrix, vars));
  }
  
  private static void initializeToStationary(List<ContinuouslyEvolving> vars, Matrix precisionMatrix, Random random)
  {
    DenseMatrix vector = MatrixOperations.sampleNormalByPrecision(random, precisionMatrix);
    for (int i = 0; i < vector.nEntries(); i++)
      vars.get(i).position.set(vector.get(i));  
  }
  public void setupLocal(ModelBuildingContext context, Matrix precision, List<ContinuouslyEvolving> variables)
  {
    final SparseMatrix counts = MatrixOperations.sparse(precision.nRows());
    StaticUtils.visitSkippingSomeZeros(precision, (int row, int col, double currentValue) -> 
    {
      if (currentValue != 0.0 && row < col)
      {
        MatrixExtensions.increment(counts, row, 1.0);
        MatrixExtensions.increment(counts, col, 1.0);
      }
    });
    StaticUtils.visitSkippingSomeZeros(precision, (int row, int col, double currentValue) -> 
    {
      if (currentValue != 0.0 && row < col)
      {
        Matrix subMatrix = MatrixOperations.dense(2, 2);
        subMatrix.set(0, 1, currentValue);
        subMatrix.set(1, 0, currentValue);
        subMatrix.set(0, 0, precision.get(row, row) / counts.get(row));
        subMatrix.set(1, 1, precision.get(col, col) / counts.get(col));
        List<ContinuouslyEvolving> varSubset = new ArrayList<>();
        varSubset.add(variables.get(row));
        varSubset.add(variables.get(col));
        context.registerBPSPotential(bpsPotential(subMatrix, varSubset));
      }
    });
    // remaining diagonals
    for (int i = 0; i < precision.nRows(); i++)
      if (counts.get(i) == 0.0)
      {
        Matrix subMatrix = MatrixOperations.dense(1, 1);
        subMatrix.set(0, 0, precision.get(i, i));
        List<ContinuouslyEvolving> varSubset = new ArrayList<>();
        varSubset.add(variables.get(i));
        context.registerBPSPotential(bpsPotential(subMatrix, varSubset));
      }
  }
  
  public static BPSPotential bpsPotential(Matrix precision, List<ContinuouslyEvolving> variables)
  {
    NormalEnergy energy = new NormalEnergy(precision);
    NormalClock timer = new NormalClock(variables, precision);
    return new BPSPotential(energy, timer);
  }

}
