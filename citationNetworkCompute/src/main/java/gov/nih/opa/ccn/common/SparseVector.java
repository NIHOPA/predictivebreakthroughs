package gov.nih.opa.ccn.common;

import com.koloboke.collect.map.IntFloatMap;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class SparseVector {

	private final int id;
	private final int[] indexes;
	private final float[] weights;
	private final double magnitude;

	public SparseVector(int id, IntFloatMap intFloatMap) {

		int[] indexes = intFloatMap.keySet().toIntArray();
		Arrays.sort(indexes);

		double magnitude = 0;
		for (int citedKey : indexes) {
			float value = intFloatMap.get(citedKey);
			magnitude += (value * value);
		}

		this.id = id;
		this.indexes = indexes;
		this.magnitude = Math.sqrt(magnitude);
		this.weights = new float[this.indexes.length];

		int i = 0;
		for (int citedKey : indexes) {
			weights[i] = intFloatMap.get(citedKey);
			i++;
		}

	}

	public int[] getIndexes() {
		return indexes;
	}

	public Map<Integer, Float> getVectorAsMap() {
		Map<Integer, Float> vector = new TreeMap<>();
		for (int i = 0; i < indexes.length; i++) {
			vector.put(indexes[i], weights[i]);
		}
		return vector;
	}

	public int getId() {
		return id;
	}

	public double cosineSim(SparseVector other) {
		if (this.indexes.length == 0 || other.indexes.length == 0) {
			return 0;
		}

		double productOfMag = this.magnitude * other.magnitude;
		return dotProduct(other) / productOfMag;
	}

	public double dotProduct(SparseVector other) {

		int[] c1;
		float[] w1;
		int[] c2;
		float[] w2;

		if (other.indexes.length < this.indexes.length) {
			c1 = other.indexes;
			w1 = other.weights;
			c2 = this.indexes;
			w2 = this.weights;
		}
		else {
			c2 = other.indexes;
			w2 = other.weights;
			c1 = this.indexes;
			w1 = this.weights;
		}

		int i1 = 0;
		int i2 = 0;

		double dotProduct = 0;
		while (i1 < c1.length && i2 < c2.length) {

			int val1 = c1[i1];
			int val2 = c2[i2];
			if (val1 == val2) {
				dotProduct += (w1[i1] * w2[i2]);
				i1++;
				i2++;
			}
			else if (val1 < val2) {
				i1++;
			}
			else {
				i2++;
			}

		}

		return dotProduct;

	}

	public double jaccardSim(SparseVector other, int threshold) {
		int intersection = setIntersection(other, threshold);
		return (double) intersection / (this.indexes.length + other.indexes.length - intersection);
	}

	public double minSim(SparseVector other, int threshold) {
		int intersection = setIntersection(other, threshold);
		int minSize = Math.min(this.indexes.length, other.indexes.length);
		return (double) intersection / minSize;
	}

	public int setIntersection(SparseVector other, int threshold) {

		int[] c1;
		float[] w1;
		int[] c2;
		float[] w2;

		if (other.indexes.length < this.indexes.length) {
			c1 = other.indexes;
			w1 = other.weights;
			c2 = this.indexes;
			w2 = this.weights;
		}
		else {
			c2 = other.indexes;
			w2 = other.weights;
			c1 = this.indexes;
			w1 = this.weights;
		}

		int i1 = 0;
		int i2 = 0;

		int intersection = 0;
		while (i1 < c1.length && i2 < c2.length) {

			int val1 = c1[i1];
			int val2 = c2[i2];
			if (val1 == val2) {
				if (w1[i1] >= threshold && w2[i2] >= threshold) {
					intersection++;
				}
				i1++;
				i2++;
			}
			else if (val1 < val2) {
				i1++;
			}
			else {
				i2++;
			}

		}

		return intersection;

	}

	public double diff(SparseVector other) {

		double diff = 0;

		int otherPointer = 0;
		int thisPointer = 0;

		int otherLength = other.indexes.length;
		int thisLength = this.indexes.length;

		while (otherPointer < otherLength && thisPointer < thisLength) {

			int otherIndex = other.indexes[otherPointer];
			int thisIndex = this.indexes[thisPointer];

			if (otherIndex == thisIndex) {

				diff += Math.abs(other.weights[otherPointer] - this.weights[thisPointer]);
				otherPointer++;
				thisPointer++;
			}
			else if (otherIndex < thisIndex) {
				diff += Math.abs(other.weights[otherPointer]);
				otherPointer++;
			}
			else {
				diff += Math.abs(this.weights[thisPointer]);
				thisPointer++;
			}

		}

		return diff;
	}

	public float[] getWeights() {
		return weights;
	}
}

