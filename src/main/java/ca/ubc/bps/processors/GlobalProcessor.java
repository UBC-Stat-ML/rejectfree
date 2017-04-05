package ca.ubc.bps.processors;

import ca.ubc.bps.processors.ConvertToGlobalProcessor.GlobalProcessorContext;

public interface GlobalProcessor
{
  public void process(GlobalProcessorContext context);
}