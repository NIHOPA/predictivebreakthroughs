package gov.nih.opa.mcl;

import java.util.function.Consumer;

/**
 * Interface used by the MCL/R-MCL algorithm to load the data
 */
public interface MCLDataSource {

	MCLNodeInfo getNodeInfo();

	void iterateEdges(Consumer<MCLNode> edgeConsumer);

	void open() throws Exception;
}
