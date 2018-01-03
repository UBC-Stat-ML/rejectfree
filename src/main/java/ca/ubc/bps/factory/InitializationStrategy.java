package ca.ubc.bps.factory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import blang.inits.Implementations;
import briefj.BriefIO;
import ca.ubc.bps.state.PositionVelocity;

import ca.ubc.bps.factory.InitializationStrategy.Zero;
import ca.ubc.bps.factory.InitializationStrategy.Stationary;
import ca.ubc.bps.factory.InitializationStrategy.Far;
import ca.ubc.bps.factory.InitializationStrategy.FromCSVFile;
import ca.ubc.bps.factory.InitializationStrategy.NoInit;

@Implementations({Zero.class, Stationary.class, Far.class, FromCSVFile.class, NoInit.class})
public interface InitializationStrategy
{
  public boolean requestStationarySampling();
  public void initializePositions(Collection<PositionVelocity> positionVelocityCoordinates);
  
  public static class Far implements InitializationStrategy
  {
    @Arg @DefaultValue("1.0")
    public double valueForEachPositionCoordinate = 1.0;

    @Override
    public boolean requestStationarySampling()
    {
      return false;
    }

    @Override
    public void initializePositions(Collection<PositionVelocity> positionVelocityCoordinates)
    {
      for (PositionVelocity coordinate : positionVelocityCoordinates)
        coordinate.position.set(valueForEachPositionCoordinate);
    }
    
  }
  
  public static class NoInit implements InitializationStrategy
  {
    @Override
    public boolean requestStationarySampling() 
    {
      return false;
    }

    @Override
    public void initializePositions(Collection<PositionVelocity> positionVelocityCoordinates) 
    {
    }
  }
  
  public static class FromCSVFile implements InitializationStrategy
  {
    @Arg(description = "CSV File with a header and two columns: first is variable index and second is position value.")
    public File file;

    @Override
    public boolean requestStationarySampling()
    {
      return false;
    }

    @Override
    public void initializePositions(Collection<PositionVelocity> positionVelocityCoordinates)
    {
      List<PositionVelocity> list = new ArrayList<>(positionVelocityCoordinates);
      for (List<String> line : BriefIO.readLines(file).splitCSV())
      {
        int index = Integer.parseInt(line.get(0));
        double value = Double.parseDouble(line.get(1));
        list.get(index).position.set(value);
      }
    }
    
  }
  
  public static class Zero implements InitializationStrategy
  {
    @Override 
    public boolean requestStationarySampling()
    {
      return false;
    }

    @Override
    public void initializePositions(Collection<PositionVelocity> positionVelocityCoordinates)
    {
      for (PositionVelocity state : positionVelocityCoordinates)
        state.position.set(0.0);
    }
  }
  
  public static class Stationary implements InitializationStrategy
  {
    @Override
    public boolean requestStationarySampling()
    {
      return true;
    }
    @Override
    public void initializePositions(Collection<PositionVelocity> positionVelocityCoordinates)
    {
      // nothing to do
    }
  }
}