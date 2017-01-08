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


### Interfaces ``JumpKernel`` and ``Clock``

JumpKernel specifies the following method to implement:

```java
void simulate(Random random);
```

which samples the jump. The change is again performed 
in place into the coordinates held by the instance.

Clock on the other hand specifies:

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

The main data structures maintained during simulation are:

```java
double               time;
  
// queue over the jump processes and their next schedule time
EventQueue<Integer>  queue;
  
  // variable -> last updated time
  private double  []           lastUpdateTime; 
  
  // event source -> isBound?
  private boolean []           isBoundIndicators;
```