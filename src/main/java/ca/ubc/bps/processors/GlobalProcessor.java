package ca.ubc.bps.processors;

import ca.ubc.bps.processors.ConvertToGlobalProcessor.GlobalProcessorContext;

/**
 * Works as local processors, except that many (typical use case: all) variables are updated.
 * 
 * @author bouchard
 *
 */
public interface GlobalProcessor
{
  /**
   * The variables in the context are set at the beginning of a segment. Managed interpolation is available 
   * through the context. 
   * @param context
   */
  public void process(GlobalProcessorContext context);
}