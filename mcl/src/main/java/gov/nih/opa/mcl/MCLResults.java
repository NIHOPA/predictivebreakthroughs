package gov.nih.opa.mcl;

import java.util.List;

/** Simple container for a list of MCLCluster cluster results
 *
 * @param clusters
 */
public record MCLResults(List<MCLCluster> clusters) {

}
