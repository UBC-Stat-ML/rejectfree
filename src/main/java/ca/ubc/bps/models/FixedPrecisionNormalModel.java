package ca.ubc.bps.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import ca.ubc.bps.energies.NormalEnergy;
import ca.ubc.bps.factory.ModelBuildingContext;
import ca.ubc.bps.BPSPotential;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.bps.state.IsotropicHamiltonian;
import ca.ubc.bps.state.PiecewiseLinear;
import ca.ubc.bps.timers.NormalClock;
import xlinear.DenseMatrix;
import xlinear.Matrix;
import xlinear.MatrixExtensions;
import xlinear.MatrixOperations;
import xlinear.SparseMatrix;
import xlinear.StaticUtils;

public class FixedPrecisionNormalModel implements Model
{
  @Arg                     @DefaultValue("DiagonalPrecision")
  public PrecisionBuilder precision = new DiagonalPrecision();
  
  @Arg       @DefaultValue("true")
  public boolean useLocal = true;
  
  @Arg                       @DefaultValue("None")
  public Likelihood likelihood = Likelihood.none;
  
  @Override
  public void setup(ModelBuildingContext context, boolean initializeToStationary)
  {
    if (!(context.dynamics() instanceof PiecewiseLinear) && 
        !(context.dynamics() instanceof IsotropicHamiltonian))
      throw new RuntimeException();
    Matrix precisionMatrix = precision.build();
    List<PositionVelocity> vars = context.buildAndRegisterPositionVelocityCoordinates(precisionMatrix.nCols());
    if (initializeToStationary)
      initializeToStationary(vars, precisionMatrix, context.initializationRandom);
    
    if (context.dynamics() instanceof PiecewiseLinear)
    {
      if (useLocal && precisionMatrix.nCols() > 2)
        setupLocal(context, precisionMatrix, vars);
      else
        context.registerBPSPotential(potential(precisionMatrix, vars));
    }
    else if (context.dynamics() instanceof IsotropicHamiltonian)
      ((IsotropicHamiltonian) (context.dynamics())).setPrecision(((DiagonalPrecision) precision).diagonalPrecision);
    else
      throw new RuntimeException();
    
    likelihood.setup(context, vars); 
  }
  
  private static void initializeToStationary(List<PositionVelocity> vars, Matrix precisionMatrix, Random random)
  {
    DenseMatrix vector = MatrixOperations.sampleNormalByPrecision(random, precisionMatrix);
    for (int i = 0; i < vector.nEntries(); i++)
      vars.get(i).position.set(vector.get(i));  
  }
  
  public static class SparseDecomposition
  {
    public List<Integer> 
      edgeEndPoints1 = new ArrayList<>(),
      edgeEndPoints2 = new ArrayList<>();
    
    public List<Integer>
      singletonPoints = new ArrayList<>();
    
    public List<Matrix> 
      subMatrices_2by2 = new ArrayList<>(),
      subMatrices_1by1 = new ArrayList<>();
    
    public void add2by2(int row, int col, double diagonal0, double diagonal1, double offDiagonal)
    {
      Matrix subMatrix = MatrixOperations.dense(2, 2);
      subMatrix.set(0, 1, offDiagonal);
      subMatrix.set(1, 0, offDiagonal);
      subMatrix.set(0, 0, diagonal0);
      subMatrix.set(1, 1, diagonal1);
      subMatrices_2by2.add(subMatrix);
      edgeEndPoints1.add(row);
      edgeEndPoints2.add(col);
    }
    
    public void add1by1(int entry, double value)
    {
      Matrix subMatrix = MatrixOperations.dense(1, 1);
      subMatrix.set(0, 0, value);
      subMatrices_1by1.add(subMatrix);
      singletonPoints.add(entry);
    }
  }
  
  public static SparseDecomposition sparseDecomposition(Matrix precision)
  {
    final SparseDecomposition result = new SparseDecomposition();
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
        result.add2by2(row, col, precision.get(row, row) / counts.get(row), precision.get(col, col) / counts.get(col), currentValue);
    });
    // remaining diagonals
    for (int i = 0; i < precision.nRows(); i++)
      if (counts.get(i) == 0.0)
        result.add1by1(i, precision.get(i, i));
    return result;
  }
  
  public static void setupLocal(ModelBuildingContext context, Matrix precision, List<PositionVelocity> variables)
  {
    SparseDecomposition decomp = sparseDecomposition(precision);
    for (int i = 0; i < decomp.subMatrices_2by2.size(); i++)
    {
      List<PositionVelocity> varSubset = new ArrayList<>();
      varSubset.add(variables.get(decomp.edgeEndPoints1.get(i)));
      varSubset.add(variables.get(decomp.edgeEndPoints2.get(i)));
      context.registerBPSPotential(potential(decomp.subMatrices_2by2.get(i), varSubset));
    }
    for (int i = 0; i < decomp.subMatrices_1by1.size(); i++)
    {
      List<PositionVelocity> varSubset = new ArrayList<>();
      varSubset.add(variables.get(decomp.singletonPoints.get(i)));
      context.registerBPSPotential(potential(decomp.subMatrices_1by1.get(i), varSubset));
    }
  }
  
  public static BPSPotential potential(Matrix precision, List<PositionVelocity> variables)
  {
    NormalEnergy energy = new NormalEnergy(precision);
    NormalClock timer = new NormalClock(variables, precision);
    return new BPSPotential(energy, timer);
  }

}
