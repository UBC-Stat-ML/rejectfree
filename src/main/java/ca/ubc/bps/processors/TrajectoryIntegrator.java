package ca.ubc.bps.processors;

class TrajectoryIntegrator
{
  final SegmentIntegrator integral;
  double totalLength = 0.0, sum = 0.0;
  TrajectoryIntegrator(SegmentIntegrator integral)
  {
    this.integral = integral;
  }
  void process(double deltaTime, double x, double v)
  {
    totalLength += deltaTime;
    sum += integral.evaluate(x, v, deltaTime);
  }
  double integrate()
  {
    if (totalLength == 0.0)
      throw new RuntimeException();
    return sum / totalLength;
  }
}