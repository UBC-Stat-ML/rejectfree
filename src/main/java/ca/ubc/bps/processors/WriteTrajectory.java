package ca.ubc.bps.processors;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;

import ca.ubc.bps.state.ContinuousStateDependent;
import ca.ubc.bps.state.ContinuouslyEvolving;
import ca.ubc.pdmp.Processor;

public class WriteTrajectory extends ContinuousStateDependent implements Processor
{
  final ContinuouslyEvolving variable;
  final Writer writer;
  
  public WriteTrajectory(ContinuouslyEvolving variable, Writer writer)
  {
    super(Collections.singletonList(variable));
    this.variable = variable;
    this.writer = writer;
    println("deltaTime", "initialPosition", "initialVelocity");
  } 
  
  @Override
  public void process(double deltaTime, int jumpProcessIndex)
  {
    println(Double.toString(deltaTime), Double.toString(variable.position.get()), Double.toString(variable.velocity.get()));
  }
  
  private void println(Object deltaTime, Object pos, String vel)
  {
    try
    {
      writer.append(deltaTime + "," + pos + "," + vel + "\n");
    } 
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

}
