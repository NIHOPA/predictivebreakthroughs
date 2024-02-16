package gov.nih.opa.mcl;

import com.koloboke.collect.map.hash.HashIntDoubleMap;

/**
 * Node structure used as input into the clustering algorithm
 * All node names have been mapped to an integer index
 * Edges are stored as a map of node neighbors as an integer index with a weight
 */
public record MCLNode(int nodeAId, HashIntDoubleMap edges) {
}
