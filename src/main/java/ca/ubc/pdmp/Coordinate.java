package ca.ubc.pdmp;

/**
 * A coordinate is a set of variables such that the deterministic dynamics of
 * this set of variables can be simulated without knowledge of any other
 * coordinates.
 * 
 * We assume each coordinate object holds a mutable representation to its
 * corresponding variables and that no two coordinates share variables.
 * 
 * Examples:
 * 
 * - For the local BPS: A single position index with its corresponding velocity.
 * - A discrete variable undergoing hold-jump behaviour. - For the non-sparse
 * Hamiltonian BPS: all variables and their velocities.
 * 
 * @author bouchard
 *
 */
public interface Coordinate
{
  void extrapolateInPlace(double deltaTime);
}