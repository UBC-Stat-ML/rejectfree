package ca.ubc.bps.factory;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.inject.TypeLiteral;

import blang.inits.Creator;
import blang.inits.DesignatedConstructor;
import blang.inits.InitService;
import blang.inits.Input;
import blang.inits.providers.CollectionsProviders;

public class MonitoredIndices
{
  private final List<Integer> list;
  @DesignatedConstructor
  public MonitoredIndices(
      @Input(formatDescription = "all|none|space-separated indices") List<String> strings,
      @InitService final Creator creator)
  {
    if (Joiner.on("").join(strings).trim().equals("all"))
      this.list = null;
    else if (Joiner.on("").join(strings).trim().equals("none"))
      this.list = new ArrayList<>();
    else
    {
      TypeLiteral<List<Integer>> listOfInts = new TypeLiteral<List<Integer>>() {};
      this.list = CollectionsProviders.parseList(strings, listOfInts, creator);
    }
  }
  public MonitoredIndices(List<Integer> list) 
  {
    this.list = list;
  }
  public List<Integer> get(int numberItems)
  {
    if (list == null)
    {
      List<Integer> result = new ArrayList<Integer>();
      for (int i = 0; i < numberItems; i++)
        result.add(i);
      return result;
    }
    else
      return list;
  }
}