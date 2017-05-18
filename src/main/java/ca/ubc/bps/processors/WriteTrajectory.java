package ca.ubc.bps.processors;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;

import ca.ubc.bps.state.PositionVelocityDependent;
import ca.ubc.bps.state.PositionVelocity;
import ca.ubc.pdmp.Processor;

public class WriteTrajectory extends PositionVelocityDependent implements Processor
{
  final PositionVelocity variable;
  final Writer writer;
  
  public WriteTrajectory(PositionVelocity variable, Writer writer)
  {
    super(Collections.singletonList(variable));
    this.variable = variable;
    this.writer = writer;
    println("deltaTime", "initialPosition", "initialVelocity", "jumpProcessIndex");
  } 
  
  @Override
  public void process(double deltaTime, int jumpProcessIndex)
  {
    println(Double.toString(deltaTime), Double.toString(variable.position.get()), Double.toString(variable.velocity.get()), Integer.toString(jumpProcessIndex));
  }
  
  private void println(Object deltaTime, Object pos, String vel, Object jumpProcessIndex)
  {
    try
    {
      writer.append(deltaTime + "," + pos + "," + vel + "," + jumpProcessIndex + "\n");
    } 
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

}
