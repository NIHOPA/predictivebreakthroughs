package gov.nih.opa.mcl;

import java.util.function.Consumer;

public interface MCLDataSource {

	MCLNodeInfo getNodeInfo();

	void iterateEdges(Consumer<MCLNode> edgeConsumer);

	void open() throws Exception;
}
