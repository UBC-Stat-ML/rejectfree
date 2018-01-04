package ca.ubc.bps.energies;

/**
 * When implementing the local version, gradients will 
 * be low-dimensional, so using a numerical finite 
 * difference solver is not too bad in this context.
 * 
 * In the future, a method based on Math Commons' autodiff
 * functionalities could be used instead but that requires 
 * a bit of work to setup the operator overloadings. 
 */
public interface NumericalEnergy extends Energy 
{
  public static double REL_ACCURACY = 0.01;
  public static double STARTING_DELTA = 1.0;
  public static int MAX_ITERS = 100;
  
  @Override
  public default double[] gradient(double[] point) 
  {
    double [] result = new double[point.length];
    for (int i = 0; i < point.length; i++)
      result[i] = partialDerivative(i, point, this);
    return result;
  }

  public static double partialDerivative(int i, double[] point, Energy energy) 
  {
    double lastEstimate = Double.NaN;
    double delta = STARTING_DELTA;
    double curRelAcc = Double.NaN;
    for (int iteration = 0; iteration < MAX_ITERS; iteration++)
    {
      final double eval0 = energy.valueAt(point);
      final double direction = iteration % 2 == 0 ? +1 : -1;
      final double x0 = point[i];
      final double x1 = x0 + direction * delta;
      point[i] = x1;
      final double eval1 = energy.valueAt(point);
      point[i] = x0;
      final double estimate = (eval1 - eval0) / (x1 - x0);
      curRelAcc = Math.abs((lastEstimate - estimate)/estimate);
      if (curRelAcc < REL_ACCURACY)
        return estimate;
      delta /= 2.0;
      lastEstimate = estimate;
    }
    throw new RuntimeException("Req accuracy not obtained. Cur accuracy is " + curRelAcc);
  }
}
