package gov.nih.opa.ccn;

import gov.nih.opa.ccn.common.MongoCited;
import gov.nih.opa.ccn.driver.CocitationVectorComputeDriverAllVsCCN;
import gov.nih.opa.pico.PICOCommonCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "ccnFirstOrder", description = "Computes co-citation network of a set of all PMIDs in the database vs the papers co-cited by those PMIDs (first order CCN) for a maximum year using reference scaling")
public class CCNFirstOrder implements Callable<Integer> {

	private static final Logger LOG = LoggerFactory.getLogger(CCNFirstOrder.class);

	@CommandLine.ParentCommand
	private CCN ccnd;

	@CommandLine.Option(names = { "--output" }, description = "Full path to the output TSV file i.e. /path/to/output.tsv", required = true)
	public String output;

	@CommandLine.Option(names = "--threads", description = "Number of threads to use.", required = true)
	public Integer threads;

	@CommandLine.Option(names = "--threshold", description = "Threshold to write. Recommended: 0.35", required = true)
	public Double threshold;

	@CommandLine.Option(names = "--cacheSize", description = "Cache size. Recommended: 20000000 i.e. 200Gs if available.", required = true)
	public Integer cacheSize;

	@CommandLine.Option(names = "--maxYear", description = "Maximum year to calculate.", required = true)
	public Integer maxYear;

	@Override
	public Integer call() throws Exception {

		LOG.info("Starting Processing");

		Set<Integer> allPmids = MongoCited.getMongoCited().getAllCitedPmids(maxYear);

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
			CocitationVectorComputeDriverAllVsCCN.computeFirstOrderCCN(threads, cacheSize, threshold, writer, allPmids, maxYear);
		}
		LOG.info("Finished Processing");

		return CommandLine.ExitCode.OK;
	}

	public static void main(String[] args) {
		PICOCommonCmd.runCommandLine(new CCNFirstOrder(), args);
	}
}


