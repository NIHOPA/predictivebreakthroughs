package gov.nih.opa.mcl.matrix;

/**
 * Working node class to hold the current value of an index within a column
 */
public class WorkNode {

	protected final int index;
	protected double value;

	public WorkNode(int index, double value) {
		this.index = index;
		this.value = value;
	}

}
