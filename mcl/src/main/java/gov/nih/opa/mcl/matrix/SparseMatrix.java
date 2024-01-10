package gov.nih.opa.mcl.matrix;

import com.koloboke.collect.map.hash.HashIntIntMap;
import com.koloboke.collect.map.hash.HashIntIntMaps;

import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class SparseMatrix {
	private static final Logger LOG = Logger.getLogger(SparseMatrix.class.getSimpleName());
	public static final double ZERO_THRESHOLD = 10 * Double.MIN_VALUE;

	private final SparseVector[] columns;
	private double shift = 1;
	private long numberOfEntries = 0;

	public SparseMatrix(SparseVector[] columns) {
		this.columns = columns;

		for (SparseVector column : this.columns) {
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

		SparseVector[] newColumns = new SparseVector[columns.length];

		DoubleAdder difference = new DoubleAdder();

		IntStream.range(0, columns.length).parallel().forEach(col -> {
			SparseVector bCol = other.columns[col];

			double sum = 0;

			SparseWorkVector sv = new SparseWorkVector(col);

			int k = bCol.indexes.length;

			int[] aColRowIdx = new int[k];

			SparseVector[] aCols = new SparseVector[k];

			for (int i = 0; i < k; i++) {
				aCols[i] = columns[bCol.indexes[i]];
			}

			boolean done = false;
			while (!done) {
				int minRow = columns.length + 1;
				for (int i = 0; i < k; i++) {
					SparseVector aCol = aCols[i];
					if (aColRowIdx[i] < aCol.indexes.length) {
						minRow = Math.min(aCol.indexes[aColRowIdx[i]], minRow);
					}
				}

				double result = 0;
				for (int i = 0; i < k; i++) {
					SparseVector aCol = aCols[i];
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
					SparseVector aCol = aCols[i];
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

			newColumns[col] = sv.getSparseVector();

			difference.add(this.columns[col].diff(newColumns[col]));

		});

		SparseMatrix newMatrix = new SparseMatrix(newColumns);
		newMatrix.shift = (difference.sum() / this.columns.length) / 2.0;
		return newMatrix;
	}

	public HashIntIntMap getIdToAttractorMap() {
		HashIntIntMap idToAttractorMap = HashIntIntMaps.newMutableMap();

		for (SparseVector col : this.columns) {
			int attractor = col.indexOfMaxValue;
			idToAttractorMap.put(col.label, attractor);
		}

		return idToAttractorMap;

	}

}
