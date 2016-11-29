package ca.ubc.rejfree;

public interface MutableObject<T>
{
  public void set(T value);
  public T get();
}