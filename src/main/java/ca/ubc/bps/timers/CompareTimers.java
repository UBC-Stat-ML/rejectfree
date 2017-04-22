package ca.ubc.bps.timers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import bayonet.math.NumericalUtils;
import ca.ubc.pdmp.Clock;
import ca.ubc.pdmp.Coordinate;
import ca.ubc.pdmp.DeltaTime;

public class CompareTimers implements Clock
{
  private final List<Clock> clocks;
  private final Collection<Coordinate> coordinates;
  private final double threshold;
  
  public CompareTimers(List<Clock> clocks, double threshold)
  {
    super();
    this.clocks = clocks;
    this.threshold = threshold;
    this.coordinates = new LinkedHashSet<>();
    for (Clock c : clocks)
      this.coordinates.addAll(c.requiredVariables());
  }

  @Override
  public Collection<? extends Coordinate> requiredVariables()
  {
    return coordinates;
  }

  @Override
  public DeltaTime next(Random random)
  {
    final long seed = random.nextLong();
    List<DeltaTime> answers = new ArrayList<>();
    for (Clock c : clocks)
      answers.add(c.next(new Random(seed)));
    check(answers);
    return answers.get(0);
  }

  private void check(List<DeltaTime> answers)
  {
    StringBuilder report = new StringBuilder();
    boolean error = false;
    DeltaTime ref = answers.get(0);
    for (int i = 0; i < answers.size(); i++)
    {
      DeltaTime answer = answers.get(i);
      report.append(clocks.get(i).getClass().getSimpleName() + " " + answer + "\n");
      if (answer.isBound != ref.isBound)
        error = true;
      if (!answer.isBound &&
          !NumericalUtils.isClose(answer.deltaTime, ref.deltaTime, threshold))
        error = true;
    }
    if (error)
      throw new RuntimeException(report.toString());
  }
}
