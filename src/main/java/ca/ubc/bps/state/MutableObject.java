package ca.ubc.bps.state;

public interface MutableObject<T>
{
  public void set(T value);
  public T get();
}