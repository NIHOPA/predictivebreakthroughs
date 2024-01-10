package gov.nih.opa.ccn.common;

import java.util.Collection;
import java.util.function.BiFunction;

/**
 * Created by mdavis on 6/27/17.
 */
public class SimConfig {

	public static final String SOURCE = "source";
	public static final String TARGET = "target";
	public static final String DIRECT = "direct";
	private final Collection<SimType> simTypes;

	private boolean min;
	private boolean jaccard;
	private boolean cosine;
	private BiFunction<Integer, Integer, DirectCitationType> directLookup;

	private final char sep;
	private final double threshold;

	public SimConfig(Collection<SimType> simTypes, double threshold, char sep) {
		min = false;
		cosine = false;
		jaccard = false;
		this.sep = sep;
		this.threshold = threshold;

		for (SimType simType : simTypes) {
			if (SimType.MIN.equals(simType)) {
				min = true;
			}
			else if (SimType.JACCARD.equals(simType)) {
				jaccard = true;
			}
			else if (SimType.COSINE.equals(simType)) {
				cosine = true;
			}
		}

		this.simTypes = simTypes;
	}

	public void setDirect(BiFunction<Integer, Integer, DirectCitationType> directLookup) {
		this.directLookup = directLookup;
	}

	public String getCSVHeader() {
		StringBuilder sb = new StringBuilder();
		sb.append(SOURCE);
		sb.append(sep);
		sb.append(TARGET);

		for (SimType simType : simTypes) {
			sb.append(sep);
			sb.append(simType.name().toLowerCase());
		}

		if (directLookup != null) {
			sb.append(sep);
			sb.append(DIRECT);
		}

		sb.append('\n');
		return sb.toString();
	}

	public String getCSVLine(SparseVector v1, SparseVector v2) {
		boolean write = false;

		double minSim = 0;
		if (min) {
			minSim = v1.minSim(v2, 1);
			if (minSim >= threshold) {
				write = true;
			}
		}

		double jaccardSim = 0;
		if (jaccard) {
			jaccardSim = v1.jaccardSim(v2, 1);
			if (jaccardSim >= threshold) {
				write = true;
			}
		}

		double cosineSim = 0;
		if (cosine) {
			cosineSim = v1.cosineSim(v2);
			if (cosineSim >= threshold) {
				write = true;
			}
		}

		if (write) {
			StringBuilder sb = new StringBuilder();

			Boolean direct = null;

			if (directLookup != null) {
				DirectCitationType directCitationType = directLookup.apply(v1.getId(), v2.getId());
				if (DirectCitationType.NONE.equals(directCitationType)) {
					sb.append(v1.getId());
					sb.append(sep);
					sb.append(v2.getId());
					direct = false;
				}
				else if (DirectCitationType.CITED_BY.equals(directCitationType)) {
					sb.append(v1.getId());
					sb.append(sep);
					sb.append(v2.getId());
					direct = true;
				}
				else if (DirectCitationType.CITES.equals(directCitationType)) {
					sb.append(v2.getId());
					sb.append(sep);
					sb.append(v1.getId());
					direct = true;
				}
				else {
					throw new IllegalArgumentException("Invalid direct citation type <" + directCitationType + ">");
				}

			}
			else {
				sb.append(v1.getId());
				sb.append(sep);
				sb.append(v2.getId());
			}

			for (SimType simType : simTypes) {
				if (SimType.MIN.equals(simType)) {
					sb.append(sep);
					sb.append(String.format("%.04f", minSim));
				}
				else if (SimType.JACCARD.equals(simType)) {
					sb.append(sep);
					sb.append(String.format("%.04f", jaccardSim));
				}
				else if (SimType.COSINE.equals(simType)) {
					sb.append(sep);
					sb.append(String.format("%.04f", cosineSim));
				}
			}

			if (direct != null) {
				sb.append(sep);
				sb.append(direct);
			}

			sb.append('\n');
			return sb.toString();
		}
		return null;
	}
}
