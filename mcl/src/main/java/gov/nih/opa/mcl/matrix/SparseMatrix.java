package gov.nih.opa.mcl.matrix;

import com.koloboke.collect.map.hash.HashIntIntMap;
import com.koloboke.collect.map.hash.HashIntIntMaps;

import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.IntStream;

/**
 * SparseMatrix handles the multiply, inflate, prune steps of the MCL algorithm using a SparseColumn[] as a basis for the matrix.  It also allows finding the attractor to determine cluster assignment.
 */
public class SparseMatrix {
	public static final double ZERO_THRESHOLD = 10 * Double.MIN_VALUE;

	private final SparseColumn[] columns;
	private double shift = 1;
	private long numberOfEntries = 0;

	public SparseMatrix(SparseColumn[] columns) {
		this.columns = columns;

		for (SparseColumn column : this.columns) {
			numberOfEntries += column.indexes.length;
		}
	}

	public long getNumberOfEntries() {
		return numberOfEntries;
	}

	public double getShift() {
		return shift;
	}

	public SparseMatrix multiplyInflatePrune(SparseMatrix other, Float inflation, Float pruneThreshold, boolean normalize) {

		SparseColumn[] newColumns = new SparseColumn[columns.length];

		DoubleAdder difference = new DoubleAdder();

		IntStream.range(0, columns.length).parallel().forEach(col -> {
			SparseColumn bCol = other.columns[col];

			double sum = 0;

			SparseWorkColumn sv = new SparseWorkColumn(col);

			int k = bCol.indexes.length;

			int[] aColRowIdx = new int[k];

			SparseColumn[] aCols = new SparseColumn[k];

			for (int i = 0; i < k; i++) {
				aCols[i] = columns[bCol.indexes[i]];
			}

			boolean done = false;
			while (!done) {
				int minRow = columns.length + 1;
				for (int i = 0; i < k; i++) {
					SparseColumn aCol = aCols[i];
					if (aColRowIdx[i] < aCol.indexes.length) {
						minRow = Math.min(aCol.indexes[aColRowIdx[i]], minRow);
					}
				}

				double result = 0;
				for (int i = 0; i < k; i++) {
					SparseColumn aCol = aCols[i];
					if (aColRowIdx[i] < aCol.indexes.length) {
						int aRowIdxForColumn = aColRowIdx[i];
						if (aCol.indexes[aRowIdxForColumn] == minRow) {
							result += aCol.values[aRowIdxForColumn] * bCol.values[i];
							aColRowIdx[i]++;
						}
					}
				}

				if (Math.abs(result) > ZERO_THRESHOLD) {
					if (inflation != null) {
						result = Math.pow(result, inflation);
					}
					sv.addNode(new WorkNode(minRow, result));
					sum += result;
				}

				done = true;
				for (int i = 0; i < k; i++) {
					SparseColumn aCol = aCols[i];
					if (aColRowIdx[i] < aCol.indexes.length) {
						done = false;
						break;
					}
				}
			}

			if (normalize) {
				sv.divideNodesBy(sum);
			}

			if (pruneThreshold != null) {
				sv.removeIfBelow(pruneThreshold);
			}

			newColumns[col] = sv.getSparseColumn();

			difference.add(this.columns[col].diff(newColumns[col]));

		});

		SparseMatrix newMatrix = new SparseMatrix(newColumns);
		newMatrix.shift = (difference.sum() / this.columns.length) / 2.0;
		return newMatrix;
	}

	public HashIntIntMap getIdToAttractorMap() {
		HashIntIntMap idToAttractorMap = HashIntIntMaps.newMutableMap();

		for (SparseColumn col : this.columns) {
			int attractor = col.indexOfMaxValue;
			idToAttractorMap.put(col.label, attractor);
		}

		return idToAttractorMap;

	}

}
