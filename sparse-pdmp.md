# Package ``ca.ubc.pdmp``

## Purpose

Simulation of Piecewise Deterministic Markov Processes (PDMP). The focus is on efficient simulation of sparse PDMPs although non sparse ones can be handled without marked performance hit. 

This package is concerned with the low-level algorithmic aspects of PDMPs and 
as such PDMPs are modelled as conceptual interfaces only. For convenient creation of  PDMPs corresponding to BPS algorithms, see package ``ca.ubc.bps``.


## Overview 

We start by going over the interfaces we use to model PDMPs, the main ones being:

- class ``PDMP``, which is composed of:
    - a list of ``Coordinate``'s, which model the deterministic part of PDMPs; 
    - a list of ``JumpProcess``'s, which model the stochastic part, and where the class JumpProcess is composed of:
        - an instance of interface ``Clock`` to determine timing;
        - an instance of interface ``JumpKernel`` to perform the jump;
    - a list of ``Processor``'s, which take care of integration of test functions and related path processing tasks;
- interface ``StateDependent``, used to encode the sparsity assumptions;

Once these are implemented, the main algorithmic contents of the package 
``ca.ubc.pdmp`` is in the class ``PDMPSimulator``, which takes a PDMP object and simulates paths. 
    

### Determinism: interface ``Coordinate``

This models the deterministic part of PDMPs.

More precisely, a coordinate is a set of variables such that 
the deterministic dynamics of this set of 
variables can be simulated without knowledge of 
any other coordinates.

We assume each coordinate object holds a mutable representation to its corresponding variables and that no two coordinates share 
variables. 

Examples:

- For the local BPS: A single position index with its corresponding velocity.
- A discrete variable undergoing hold-jump behaviour.
- For the non-sparse Hamiltonian BPS: all variables and their velocities.

The only method to implement for this interface is:

```java
void extrapolateInPlace(double deltaTime);
```


### Stochasticity: class ``JumpProcess`` 

A JumpProcess models the stochastic part of PDMPs. 
A JumpProcess is just a pairing of a ``Clock``, which 
determines the times of the jump, and of a ``JumpKernel``, 
which determines the effect.

Examples:

- Bounce process for one factor in the local BPS;
- Bounce process for all factors in the global BPS;
- Refreshment;
- Collisions to hard boundaries.

Before going into the details of Clock and JumpKernel, 
we note that both need to access the value of 
a potentially sparse subset of variables. As a consequence, 
both interfaces inherit from the interface ``StateDependent``
described next.


### Sparsity: interface ``StateDependent``

When computation is performed by a Clock or a JumpKernel, 
the values of a subset of the variables need to be accessed. 
To do this, instances of Clock and JumpKernel hold references 
to a subset of the coordinates.

Since the variables change in continuous manner, PDMPSimulator 
takes care of updating the coordinates via appropriate calls to 
``extrapolateInPlace`` before any method calls to ``JumpKernel``,
``Clock`` or ``Processor``. However, in the sparse case 
it is important for efficiency reasons to update as few variables 
as possible. 

To make the sparse update scheme possible, PDMPSimulator needs to 
be able to find out which coordinates are accessible by instances of 
``JumpKernel``, ``Clock`` and ``Processor``. The StateDependent 
interface specifies how to do this, namely via implementation of:

```java
Collection<? extends Coordinate> requiredVariables();
```

which is queried by PDMPSimulator in a pre-processing stage.


### Interfaces ``JumpKernel`` and ``Clock``

Interface JumpKernel specifies the following method to implement:

```java
void simulate(Random random);
```

which samples the jump. The change is again performed 
in place into the coordinates held by the instance.

Interface Clock specifies:

```java
DeltaTime next(Random random);
```

Here, DeltaTime can be either the time to the next event (by returning 
``DeltaTime.isEqualTo(deltaTime)``, or a 
strict lower bound on the time to the next event. 

Using a strict 
lower bound becomes useful in the case of a thinning implementation. 
In such case, instead of implementing thinning as an inner loop, 
we can take a lazy approach where only the first thinning step is executed, 
and in the case of a rejection, continuation of the loop is performed 
via the main priority queue by returning a time bound via 
``DeltaTime.isGreaterThan(deltaTime)``. This approach is advantageous
compared to the inner loop approach. This is because by 
the time the next thinning iteration is performed, more information 
may be known about the neighbour variables.


### Integration: interface ``Processor``

A processor implements the following method, called once for 
deterministic segment simulated by PDMPSimulator:

```java
void process(double deltaTime);
``` 

Instances of processors should additionally declare the coordinates 
they hold, but in contrast to JumpKernel's and Clock's, we assume only 
one coordinate is monitored by each processor instance. This is without loss 
of generality as the joint path of several coordinates can always be 
reconstructed from the univariate paths as a post-processing step.


## PDMP simulation

### Usage

Once a pdmp is available (either by direct implementation of the above 
interfaces, or, more commonly, via the building blocks provided in 
``ca.ubc.bps``, simulation can be done via:

```java
new PDMPSimulator(pdmp)
    .simulate(
        random, 
        StoppingCriterion.byStochasticProcessTime(100.0)
    );
```

Alternative stopping criteria include ``byWallClockTimeMilliseconds(millis)`` and 
``byNumberOfQueuePolls(number)``.


### Implementation

The main data structures updated during simulation are:

- ``time``: a global variable for the current process time;
- ``queue``: a priority queue where priorities are (absolute) times and items are integers indexing the list of jump processes specified by the PDMP;
- ``lastUpdateTimes``: an array of positive numbers, indexed by jump process indices; for each, we keep track of the last time a jump occurred;
- ``isBoundIndicators``: an array of booleans, indexed by jump process indices; for each, we keep track of whether the even in the queue is an actual jump, or a bound on the time (see discussion on DeltaTime above).

In addition to this, some quantities related to the factor graph structure are 
cached as a pre-processing step. We start by introducing some notation related to 
a certain factor graph structure.

Here we generalize the notion of factor graph used for local BPS into factor 
graphs corresponding to generic sparse PDMPs. The factor graph consists in an undirected bipartite multigraph where the 
nodes in one component are Coordinate's, and the nodes in the other component are 
the JumpProcess objects associated to the sparse PDMP. To define edges, we visit 
both the Clock and JumpKernel of each JumpProcess. We create one edge for each 
elements returned by the collection of each call to  
the method ``requiredVariables()`` specified by the StateDependent super interface   
of Clock and JumpKernel. Each edge has a label to denote if it is associated to a 
Clock or a JumpKernel.

We define in the following a functions that take as input a set of nodes A 
in one component of the graph and return the set of all nodes connected to A. 
As a convention, we use the notation ``{n,N}{d,k}`` for these functions, where:

- the prefixes mean:
    - ``n``: the function take as input JumpProcess nodes;
    - ``N``: the function take as input Coordinate nodes;
- the suffixes mean:
    - ``k``: the function is based on edges with label JumpKernel;
    - ``d``: the function is based on edges with label Clock.

For example, ``Nd_nk[j]`` returns, for given JumpProcess index j, Nd(nk({j})).

With this notation, we can write the four caches needed by the algorithm as:

- ``nd`` and ``nk``, two arrays indexed by JumpProcesses returning 
  sets of Coordinate's;
- ``Nd_nk``, an array indexed by JumpProcesses and returning sets of 
  JumpProcesses, namely Nd(nk({j})) as above;
- ``nd_Nd_nk_plus_nd_minus_nk``, an array indexed by JumpProcesses and 
  and returning sets of Coordinates, namely nd(Nd(nk({j}))) U nd({j}) \ nk({j}).
  
For efficiency, these caches are implemented as arrays of arrays of integers, 
where the inner arrays encode the sets, and with the convention that a null means 
the empty set, and the array containing only -1 is the set of all variables.
  
With these definitions, we can now introduce the two core methods implementing 
the sparse PDMP simulator.

First, ``updateVariable(int variableIndex, boolean commit)`` 
performs extrapolation of the provided variable index to the time stored 
in the global variable ``time``. The parameter ``commit`` controls whether this update 
will be used to perform a jump on that variable at that time (true), or because 
the variable is a neighbour needed by a Clock time to event re-computation (false).
The processor is called only in the case of a commit as shown in the pseudo-code below:

```java
  updateVariable(int variableIndex, boolean commit) {
    Coordinate coordinate = pdmp.coordinates.get(variableIndex)
    deltaTime = time - lastUpdateTimes[variableIndex]
    if (commit) {
      for (int processorIdx : processors[variableIndex])
        pdmp.processors.get(processorIdx).process(deltaTime)
      lastUpdateTimes[variableIndex] = time
    }
    coordinate.extrapolateInPlace(deltaTime)
  }
```
 
In the latter case, the variable is rolled back after it is needed via a call to 
``rollBack`` to ensure processor is called once per deterministic trajectory chunk:

```java
  rollBack(int variableIndex) {
    Coordinate coordinate = pdmp.coordinates.get(variableIndex)
    deltaTime = time - lastUpdateTimes[variableIndex]
    coordinate.extrapolateInPlace(-deltaTime)
  }
```

We also need a utility function to simulate event times:

```java
  simulateNextEventDeltaTime(int jumpProcessIndex) {
    queue.remove(jumpProcessIndex)
    DeltaTime nextEvent = pdmp.jumpProcesses.get(jumpProcessIndex).clock.next(random)
    absoluteTime = time + nextEvent.deltaTime
    if (absoluteTime <= stoppingRule.stochasticProcessTime) {
      isBoundIndicators[jumpProcessIndex] = nextEvent.isBound
      absoluteTime = fixNumericalIssue(absoluteTime)
      queue.add(jumpProcessIndex, absoluteTime)
    }
  }
```

With these definitions, the core algorithm in the package is as follows:

```java
  simulateChunk()
  {
    for (jumpProcessIndex : jumpProcessIndices)
      simulateNextEventDeltaTime(jumpProcessIndex)
    
    while (computeBudgetPositive() && !queue.isEmpty()) {
      event = queue.pollEvent()
      time = event.time()
      eventJumpProcessIndex = event.jumpProcessIndex()
      if (isBoundIndicators[eventJumpProcessIndex]) {
        updateVariables(nd[eventJumpProcessIndex], false)
        simulateNextEventDeltaTime(eventJumpProcessIndex)
        rollBack(nd[eventJumpProcessIndex])
      } else {
        updateVariables(nk[eventJumpProcessIndex], true)
        updateVariables(nd_Nd_nk_plus_nd_minus_nk[eventJumpProcessIndex], false)
        pdmp.jumpProcesses.get(eventJumpProcessIndex).kernel.simulate(random)
        simulateNextEventDeltaTimes(Nd_nk[eventJumpProcessIndex])
        rollBack(nd_Nd_nk_plus_nd_minus_nk[eventJumpProcessIndex]);
      }
    }
    updateAllVariables(true) 
  }
```

Finally, since times are expressed as absolute times, to avoid loss of numerical 
accuracy in long simulation, we break long trajectories into chunks (hence the name 
simulateChunk() above), 

```java
  simulate(Random random, StoppingCriterion inputStoppingRule) {
    while (inputStoppingRule.stochasticProcessTime - totalProcessTime > 0) {
      processIncrementTime = 
         min(
             maxTrajectoryLengthPerChunk, 
             inputStoppingRule.stochasticProcessTime - totalProcessTime
         )
      this.stoppingRule = new StoppingCriterion(
          processIncrementTime,
          inputStoppingRule.wallClockTimeMilliseconds,
          inputStoppingRule.numberOfQueuePolls
      )
      simulateChunk()
      totalProcessTime += processIncrementTime
    }
  }
```