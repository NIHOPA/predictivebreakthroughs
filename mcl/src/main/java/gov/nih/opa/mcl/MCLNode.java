package gov.nih.opa.mcl;

import com.koloboke.collect.map.hash.HashIntDoubleMap;

public class MCLNode {

	private int nodeAId;
	private HashIntDoubleMap edges;

	public MCLNode(int nodeAId, HashIntDoubleMap edges) {
		this.nodeAId = nodeAId;
		this.edges = edges;
	}

	public int getNodeAId() {
		return nodeAId;
	}

	public HashIntDoubleMap getEdges() {
		return edges;
	}

	@Override
	public String toString() {
		return "MCLNode{" + "nodeAId=" + nodeAId + ", edges=" + edges + '}';
	}
}
