package ca.ubc.pdmp;

public interface Processor extends StateDependent
{
  void process(double deltaTime);
}