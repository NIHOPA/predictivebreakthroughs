package gov.nih.opa.mcl;

import gov.nih.opa.mcl.matrix.SparseMatrix;
import gov.nih.opa.mcl.matrix.SparseVector;
import gov.nih.opa.mcl.matrix.SparseWorkVector;
import gov.nih.opa.mcl.matrix.WorkNode;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.logging.Logger;

public class MCL {

	private static final Logger LOG = Logger.getLogger(MCL.class.getSimpleName());

	private final MCLParameters mclParameters;

	public MCL(MCLParameters mclParameters) {

		this.mclParameters = mclParameters;
	}

	public MCLResults run(MCLDataSource mclDataSource) throws Exception {

		LOG.info("--Running MCL with parameters <" + mclParameters + ">");
		LOG.info("--Reading from " + mclDataSource);
		mclDataSource.open();
		MCLNodeInfo nodeInfo = mclDataSource.getNodeInfo();
		LOG.info("--Data source has " + nodeInfo.getNodeCount() + " nodes");

		SparseVector[] columns = new SparseVector[nodeInfo.getNodeCount()];

		LOG.info("--Loading edges into sparse matrix");

		mclDataSource.iterateEdges(node -> {

			double sum = 0;
			SparseWorkVector v = new SparseWorkVector(node.getNodeAId());
			for (Map.Entry<Integer, Double> entry : node.getEdges().entrySet()) {
				double val = entry.getValue();
				sum += val;
				v.addNode(new WorkNode(entry.getKey(), val));
			}

			sum += mclParameters.getLoop();
			v.addNode(new WorkNode(node.getNodeAId(), mclParameters.getLoop()));

			v.divideNodesBy(sum);
			v.sort();
			v.removeIfBelow(mclParameters.getPruningThreshold());

			columns[node.getNodeAId()] = v.getSparseVector();
		});

		Arrays.sort(columns, Comparator.comparing(SparseVector::getLabel));

		SparseMatrix beginMatrix = null;
		if (mclParameters.isRegularized()) {
			beginMatrix = new SparseMatrix(columns);
		}

		SparseMatrix current = new SparseMatrix(columns);
		logIter(0, current);

		for (int i = 0; i < mclParameters.getMaxIterations(); i++) {
			SparseMatrix rhs = beginMatrix == null ? current : beginMatrix;

			current = current.multiplyInflatePrune(rhs, mclParameters.getInflation(), mclParameters.getPruningThreshold(), true);
			logIter(i + 1, current);
			if (current.getShift() < mclParameters.getShiftThreshold()) {
				break;
			}
		}

		LOG.info("--Finished iterations");
		return new MCLResults(current.getIdToAttractorMap(), nodeInfo);

	}

	private void logIter(int iter, SparseMatrix current) {
		LOG.info("iteration " + String.format("%4d", iter) + ": shift " + String.format("%.8f", current.getShift()) + " entries: " + current
				.getNumberOfEntries());
	}
}
