package gov.nih.opa.mcl.matrix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SparseWorkVector {

	private final List<WorkNode> nodes;
	private final int label;

	public SparseWorkVector(int label) {
		this.label = label;
		nodes = new ArrayList<>();
	}

	public int getLabel() {
		return label;
	}

	public void addNode(WorkNode node) {
		nodes.add(node);
	}

	public void divideNodesBy(double sum) {
		nodes.forEach(node -> node.value = node.value / sum);
	}

	public void removeIfBelow(float pruneThreshold) {
		nodes.removeIf(node -> node.value < pruneThreshold);
	}

	public List<WorkNode> getNodes() {
		return nodes;
	}

	public void sort() {
		nodes.sort(Comparator.comparing(workNode -> workNode.index));
	}

	public SparseVector getSparseVector() {
		return new SparseVector(label, nodes);
	}
}