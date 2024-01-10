package gov.nih.opa.mcl;

import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import com.koloboke.collect.map.hash.HashObjIntMap;
import com.koloboke.collect.map.hash.HashObjIntMaps;

import java.util.concurrent.atomic.AtomicInteger;

public class MCLNodeInfo {

	private HashIntObjMap<String> idToName;
	private HashObjIntMap<String> nameToId;

	private AtomicInteger counter;

	public MCLNodeInfo() {
		idToName = HashIntObjMaps.newMutableMap();
		nameToId = HashObjIntMaps.newMutableMap();

		counter = new AtomicInteger();
	}

	public void addNode(String nodeName) {

		if (!nameToId.containsKey(nodeName)) {
			int id = counter.getAndIncrement();
			nameToId.put(nodeName, id);
			idToName.put(id, nodeName);
		}

	}

	public int getIdFromName(String nodeName) {
		return nameToId.getInt(nodeName);
	}

	public String getNameFromId(int nodeId) {
		return idToName.get(nodeId);
	}

	public int getNodeCount() {
		return nameToId.size();
	}

}
