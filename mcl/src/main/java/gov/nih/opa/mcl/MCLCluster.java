package gov.nih.opa.mcl;

import java.util.TreeSet;

public class MCLCluster {
	private int id;
	protected int attractor;
	private TreeSet<String> nodes;

	public MCLCluster(int attractor) {
		this.attractor = attractor;
		this.nodes = new TreeSet<>();
	}

	protected int getAttractor() {
		return attractor;
	}

	public TreeSet<String> getNodes() {
		return nodes;
	}

	public int getSize() {
		return nodes.size();
	}

	protected void addNode(String id) {
		this.nodes.add(id);
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	@Override
	public String toString() {
		return "MCLCluster{" + "id=" + id + ", nodes=" + nodes + '}';
	}
}
