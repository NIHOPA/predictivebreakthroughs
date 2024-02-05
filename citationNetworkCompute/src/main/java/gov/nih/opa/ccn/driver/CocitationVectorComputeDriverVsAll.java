package gov.nih.opa.ccn.driver;

import com.google.common.collect.Lists;
import gov.nih.opa.ccn.common.MongoCited;
import gov.nih.opa.ccn.common.SimConfig;
import gov.nih.opa.ccn.common.SimType;
import gov.nih.opa.ccn.common.SparseVector;
import gov.nih.opa.ccn.common.WorkPool;
import gov.nih.opa.spreadsheet.SpreadsheetFactory;
import gov.nih.opa.spreadsheet.SpreadsheetReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Matt Davis on 6/27/17.
 * Computes co citation network of a set of PMIDs vs all PMIDs in the database
 * This is the full CCN and not the first order computation
 * Max Year can be set to get a point in time view of the CCN
 * @author mdavis
 */
public class CocitationVectorComputeDriverVsAll {

	private static final Logger LOG = LoggerFactory.getLogger(CocitationVectorComputeDriverVsAll.class);

	public static void computeCCNVsAll(String inputCsv, String outputCsv, int threads, double thresholdToWrite, int maxYear) throws Exception {
		MongoCited mongoCited = MongoCited.getMongoCited();

		LOG.info("Threshold To write: " + thresholdToWrite);
		LOG.info("Max Year: " + maxYear);
		LOG.info("Reading pmids from: " + inputCsv);
		LOG.info("Writing output to: " + outputCsv);

		List<Integer> pmids = new ArrayList<>(readCSVToList(inputCsv));
		System.out.println("Read Input Pmids: " + pmids.size());

		SimConfig simConfig = new SimConfig(Collections.singletonList(SimType.COSINE), thresholdToWrite, ',');

		Set<Integer> allPmids = MongoCited.getMongoCited().getAllCitedPmids(maxYear);
		System.out.println("Total Pmids to Compare Against: " + allPmids.size());

		List<Integer> fullPmidList = new ArrayList<>(allPmids);

		List<List<Integer>> pmidSubLists = Lists.partition(fullPmidList, 100000);

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputCsv))) {

			writer.write(simConfig.getCSVHeader());

			SparseVector[] inputVectors = new SparseVector[pmids.size()];
			for (int i = 0; i < pmids.size(); i++) {
				inputVectors[i] = mongoCited.getCocitationVector(pmids.get(i), true, maxYear);
			}

			LOG.info("Generated <" + pmidSubLists.size() + "> sublists");
			int subList = 0;
			for (List<Integer> pmidSubList : pmidSubLists) {

				LOG.info("Processing sublist <" + subList++ + ">");

				SparseVector[] compareVectors = new SparseVector[pmidSubList.size()];
				for (int i = 0; i < pmidSubList.size(); i++) {
					compareVectors[i] = mongoCited.getCocitationVector(pmidSubList.get(i), true, maxYear);
				}

				try (WorkPool workPool = new WorkPool(threads)) {

					for (SparseVector cv1 : inputVectors) {
						workPool.executeAsync(() -> {
							for (SparseVector cv2 : compareVectors) {

								String csvLine = simConfig.getCSVLine(cv1, cv2);
								if (csvLine != null) {
									writer.write(csvLine);
								}

							}
							return null;
						});
					}

				}

			}

		}
	}

	public static TreeSet<Integer> readCSVToList(String inputCsv) throws Exception {
		TreeSet<Integer> pmids = new TreeSet<>();
		try (SpreadsheetReader reader = SpreadsheetFactory.reader(inputCsv, false)) {
			while (reader.readRow()) {
				int pmid = Integer.parseInt(reader.get(0));
				pmids.add(pmid);
			}
		}
		return pmids;
	}

}
