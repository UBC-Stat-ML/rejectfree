Processors
==========

Three processors are offered by BPSFactory:

1. ``WriteTrajectory`` which saves trajectories to disk 
2. ``IntegrateTrajectory`` which computes integrals in O(1) memory (concretely, moments of arbitrary degrees are current supported)
3. ``MemorizeTrajectory`` which keeps the trajectory in RAM

See the corresponding options ``write``, ``memorize``, ``summarize`` via ``bps --help``.

To go beyond this, there are two routes:

- For quick tests, extend ``BPSFactory`` and use either ``MemorizeTrajectory`` or add custom processors via ``BPS.addProcessor(..)``.
- Write trajectories to disk and load them via ``TrajectoryLoader``

Note also that all of the above are tailored at looking at the marginal of a single variable. To look at several variables at the same time, see ``ConvertToGlobalProcessor`` (the rationale is that global processing breaks locality, so it's better to leave it as a post-processing step which can later be parallelized over the sequence length). 