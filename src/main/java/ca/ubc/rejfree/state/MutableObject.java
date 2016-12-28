package ca.ubc.rejfree.state;

public interface MutableObject<T>
{
  public void set(T value);
  public T get();
}