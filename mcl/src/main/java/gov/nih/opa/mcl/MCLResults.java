package gov.nih.opa.mcl;

import com.koloboke.collect.map.hash.HashIntIntMap;
import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class MCLResults {

	private final HashIntIntMap idToAttractorMap;
	private final MCLNodeInfo nodeInfo;

	private List<MCLCluster> clusters;

	public MCLResults(HashIntIntMap idToAttractorMap, MCLNodeInfo nodeInfo) {
		this.idToAttractorMap = idToAttractorMap;
		this.nodeInfo = nodeInfo;

		HashIntObjMap<MCLCluster> clusterToIdMap = HashIntObjMaps.newMutableMap();
		for (int id : idToAttractorMap.keySet()) {
			int attractor = idToAttractorMap.get(id);
			clusterToIdMap.computeIfAbsent(attractor, integer -> new MCLCluster(attractor)).addNode(nodeInfo.getNameFromId(id));
		}

		clusters = new ArrayList<>(clusterToIdMap.values());
		clusters.sort(Comparator.comparing(MCLCluster::getSize).reversed().thenComparing(MCLCluster::getAttractor));

		for (int i = 0; i < clusters.size(); i++) {
			clusters.get(i).setId(i);
		}

	}

	public void iterateResuts(Consumer<MCLCluster> clusterConsumer) {
		for (MCLCluster mclCluster : clusters) {
			clusterConsumer.accept(mclCluster);
		}
	}

	public int getNumberOfClusters() {
		return clusters.size();
	}
}
