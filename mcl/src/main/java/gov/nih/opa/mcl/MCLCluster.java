package gov.nih.opa.mcl;

import java.util.TreeSet;

/** Stores the results of MCL clustering
 *
 */
public class MCLCluster {
	private int id;
	protected final int attractor;
	private final TreeSet<String> nodes;

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
