package gov.nih.opa.mcl;

import com.koloboke.collect.map.hash.HashIntIntMap;
import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import gov.nih.opa.mcl.matrix.SparseMatrix;
import gov.nih.opa.mcl.matrix.SparseVector;
import gov.nih.opa.mcl.matrix.SparseWorkVector;
import gov.nih.opa.mcl.matrix.WorkNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.logging.Logger;

/***
 * Entry point in the MCL/R-MCL algorithm
 * usage:
 *    MCLParameters param = ...
 *    MCLDataSource dataSource = ...
 *    MCLRunner mcl = new MCLRunner(param);
 *    MCLResults results = mcl.run(dataSource);
 * @author mdavis
 */
public class MCLRunner {

	private static final Logger LOG = Logger.getLogger(MCLRunner.class.getSimpleName());

	private final MCLParameters mclParameters;

	public MCLRunner(MCLParameters mclParameters) {

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
			SparseWorkVector v = new SparseWorkVector(node.nodeAId());
			for (Map.Entry<Integer, Double> entry : node.edges().entrySet()) {
				double val = entry.getValue();
				sum += val;
				v.addNode(new WorkNode(entry.getKey(), val));
			}

			sum += mclParameters.getLoop();
			v.addNode(new WorkNode(node.nodeAId(), mclParameters.getLoop()));

			v.divideNodesBy(sum);
			v.sort();
			v.removeIfBelow(mclParameters.getPruningThreshold());

			columns[node.nodeAId()] = v.getSparseVector();
		});

		Arrays.sort(columns, Comparator.comparing(SparseVector::getLabel));

		SparseMatrix beginMatrix = null;
		if (mclParameters.isRegularized()) { //R-MCL replaces currentMatrix*currentMatrix with currentMatrix*matrixAtStart
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

		HashIntObjMap<MCLCluster> clusterToIdMap = HashIntObjMaps.newMutableMap();
		HashIntIntMap idToAttractorMap = current.getIdToAttractorMap();
		for (int id : idToAttractorMap.keySet()) {
			int attractor = idToAttractorMap.get(id);
			clusterToIdMap.computeIfAbsent(attractor, integer -> new MCLCluster(attractor)).addNode(nodeInfo.getNameFromId(id));
		}

		ArrayList<MCLCluster> clusters = new ArrayList<>(clusterToIdMap.values());
		clusters.sort(Comparator.comparing(MCLCluster::getSize).reversed().thenComparing(MCLCluster::getAttractor));

		for (int i = 0; i < clusters.size(); i++) {
			clusters.get(i).setId(i);
		}
		return new MCLResults(clusters);

	}

	private void logIter(int iter, SparseMatrix current) {
		LOG.info("iteration " + String.format("%4d", iter) + ": shift " + String.format("%.8f", current.getShift()) + " entries: "
				+ current.getNumberOfEntries());
	}
}
