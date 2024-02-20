package gov.nih.opa.mcl;

import com.koloboke.collect.ObjCursor;
import com.koloboke.collect.map.hash.HashIntDoubleMap;
import com.koloboke.collect.map.hash.HashIntDoubleMaps;
import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Data source wrapper that handle the mapping of node names to integer indexes
 * Child classes just need to implement open() and call handleInput(String nodeA, String nodeB, String value) for each edge
 */
public abstract class FileDataSource implements MCLDataSource {

	private final String fileName;
	private final MCLNodeInfo mclNodeInfo;
	private final HashIntObjMap<HashIntDoubleMap> nodeIdToOtherNodes;

	public FileDataSource(String fileName) {

		this.fileName = fileName;
		this.mclNodeInfo = new MCLNodeInfo();
		this.nodeIdToOtherNodes = HashIntObjMaps.newMutableMap();

	}

	protected void handleInput(String nodeA, String nodeB, String value) {
		if (!nodeA.equals(nodeB)) {

			try {
				double weight = Double.parseDouble(value);

				mclNodeInfo.addNode(nodeA);
				mclNodeInfo.addNode(nodeB);

				int nodeAId = mclNodeInfo.getIdFromName(nodeA);
				int nodeBId = mclNodeInfo.getIdFromName(nodeB);

				nodeIdToOtherNodes.computeIfAbsent(nodeAId, integer -> HashIntDoubleMaps.newMutableMap()).put(nodeBId, weight);
				nodeIdToOtherNodes.computeIfAbsent(nodeBId, integer -> HashIntDoubleMaps.newMutableMap()).put(nodeAId, weight);

			}
			catch (Exception e) {
				throw new RuntimeException("Expected numeric last column: Found: " + value);
			}
		}
	}

	@Override
	public MCLNodeInfo getNodeInfo() {
		return mclNodeInfo;
	}

	@Override
	public void iterateEdges(Consumer<MCLNode> nodeConsumer) {
		ObjCursor<Map.Entry<Integer, HashIntDoubleMap>> cursor = nodeIdToOtherNodes.entrySet().cursor();
		while (cursor.moveNext()) {
			Map.Entry<Integer, HashIntDoubleMap> entry = cursor.elem();
			MCLNode mclNode = new MCLNode(entry.getKey(), entry.getValue());
			nodeConsumer.accept(mclNode);
			cursor.remove();
		}

	}

	public String getFileName() {
		return fileName;
	}

	@Override
	public String toString() {
		return "File Data Source: " + fileName;
	}
}