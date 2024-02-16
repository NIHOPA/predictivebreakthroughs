package gov.nih.opa.mcl.matrix;

import java.util.List;

/**
 * SparseColumn enables optimized storage of sparse columns with ability to look at column shift with the difference method
 * Index of max value is tracked to identify the attractor to determine cluster assignment at the end of the MCL algorithm
 */
public class SparseColumn {

	protected final int label;

	protected final int[] indexes;
	protected final float[] values;

	protected final int indexOfMaxValue;

	public SparseColumn(int label, List<WorkNode> workNodes) {
		this.label = label;

		int size = workNodes.size();
		this.indexes = new int[size];
		this.values = new float[size];

		float maxValue = 0;
		int maxIndex = 0;

		for (int i = 0; i < size; i++) {
			this.indexes[i] = workNodes.get(i).index;
			this.values[i] = (float) workNodes.get(i).value;
			if (this.values[i] > maxValue) {
				maxIndex = this.indexes[i];
				maxValue = this.values[i];
			}
		}

		this.indexOfMaxValue = maxIndex;
	}

	public int getLabel() {
		return label;
	}


	public double diff(SparseColumn other) {

		double diff = 0;

		int otherPointer = 0;
		int thisPointer = 0;

		int otherLength = other.indexes.length;
		int thisLength = this.indexes.length;

		while (otherPointer < otherLength && thisPointer < thisLength) {

			int otherIndex = other.indexes[otherPointer];
			int thisIndex = this.indexes[thisPointer];

			if (otherIndex == thisIndex) {

				diff += Math.abs(other.values[otherPointer] - this.values[thisPointer]);
				otherPointer++;
				thisPointer++;
			}
			else if (otherIndex < thisIndex) {
				diff += Math.abs(other.values[otherPointer]);
				otherPointer++;
			}
			else {
				diff += Math.abs(this.values[thisPointer]);
				thisPointer++;
			}

		}

		return diff;
	}
}
