package ca.ubc.pdmp;

public interface Processor extends StateDependent
{
  /**
   * Indicate that a segment of deterministic behaviour is 
   * ready to be processed. 
   * 
   * The PDMPSimulator ensures that the state is set to the 
   * value at the beginning of the the deterministic segment.
   * 
   * These are called in an order sorted by the time of the end of the 
   * deterministic intervals.
   * 
   * @param deltaTime
   * @param jumpProcessIndex The index of the process that triggered the end of 
   *  this deterministic segment, or -1 if the segment ended because of the end
   *  of the required trajectory length (or some internal numerical consideration 
   *  that cause some segments to be artificially broken).
   */
  void process(double deltaTime, int jumpProcessIndex);
}