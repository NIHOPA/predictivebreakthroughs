package gov.nih.opa.ccn;

import gov.nih.opa.ccn.driver.CocitationVectorComputeDriverVsAll;
import gov.nih.opa.pico.PICOCommonCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "ccnPMIDs", description = "Computes co-citation network of a set of PMIDs vs all papers (the full CCN) for a maximum year using reference scaling")
public class CCNPMIDs implements Callable<Integer> {

	private static final Logger LOG = LoggerFactory.getLogger(CCNPMIDs.class);

	@CommandLine.ParentCommand
	private CCN ccnd;

	@CommandLine.Option(names = { "--input" }, description = "Full path to the input CSV file i.e. /path/to/input.csv", required = true)
	public String input;

	@CommandLine.Option(names = { "--output" }, description = "Full path to the output TSV file i.e. /path/to/output.tsv", required = true)
	public String output;

	@CommandLine.Option(names = "--threads", description = "Number of threads to use.", required = true)
	public Integer threads;

	@CommandLine.Option(names = "--threshold", description = "Threshold to write.", required = true)
	public Double threshold;

	@CommandLine.Option(names = "--cacheSize", description = "Cache size.", required = true)
	public Integer cacheSize;

	@CommandLine.Option(names = "--maxYear", description = "Maximum year to calculate.", required = true)
	public Integer maxYear;

	@Override
	public Integer call() throws Exception {

		LOG.info("Starting Processing");
		CocitationVectorComputeDriverVsAll.computeCCNVsAll(input, output, threads, threshold, maxYear);
		LOG.info("Finished Processing");

		return CommandLine.ExitCode.OK;
	}

	public static void main(String[] args) {
		PICOCommonCmd.runCommandLine(new CCNPMIDs(), args);
	}
}


