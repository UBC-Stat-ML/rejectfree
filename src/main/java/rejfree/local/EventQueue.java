package rejfree.local;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;



public class EventQueue<S>
{
  private final TreeMap<Double,S> sortedEvents = new TreeMap<>();
  private final Map<S, Double> eventTimes = new HashMap<>();
  
  public Entry<Double,S> pollEvent()
  {
    Entry<Double,S> result = sortedEvents.pollFirstEntry();
    eventTimes.remove(result.getValue());
    return result;
  }
  
  public Entry<Double,S> peekEvent()
  {
    return sortedEvents.firstEntry();
  }
  
  public int size()
  {
    return sortedEvents.size();
  }
  
  public boolean isEmpty()
  {
    return size() == 0;
  }
  
  public void remove(S event)
  {
    Double time = eventTimes.get(event);
    if (time != null)
    {
      sortedEvents.remove(time);
      eventTimes.remove(event);
    }
  }
  
  public void add(S event, double time)
  {
    if (Double.isInfinite(time))
      return;
    if (containsTime(time))
      throw new RuntimeException("EventQueue does not support two events at the same time (t=" + time + ",event=" + event + ")");
    sortedEvents.put(time, event);
    if (eventTimes.put(event, time) != null)
      throw new RuntimeException("An event cannot be associated to two times.");
  }
  
  public final boolean containsTime(double t)
  {
    return sortedEvents.containsKey(t);
  }

  public double peekTime()
  {
    return sortedEvents.firstKey();
  }
  
  @Override
  public String toString()
  {
    return "EventQueue[nextTime=" + peekTime() + ",nEvents=" + size() + "]";
  }
}