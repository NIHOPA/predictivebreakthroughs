package gov.nih.opa.mcl;

import gov.nih.opa.pico.PICOCommonCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "run", description = "The MCL algorithm is short for the Markov Cluster Algorithm, a fast and scalable unsupervised cluster algorithm for graphs (also known as networks) based on simulation of (stochastic) flow in graphs. More: https://micans.org/mcl/")
public class MCLRun implements Callable<Integer>, MCLParameters {

	private static final Logger LOG = LoggerFactory.getLogger(MCLRun.class);

	@CommandLine.ParentCommand
	private MCL mcl;

	@CommandLine.Option(names = {
			"--inflation" }, description = "The inflation parameter to set", defaultValue = "2.0f", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
	public Float inflation = 2.0f;

	@CommandLine.Option(names = "--loop", description = "Self loop ratio.", defaultValue = "0.001f", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
	public Float loop = 0.001f;

	@CommandLine.Option(names = "--pruningThreshold", description = "Minimum probability.", defaultValue = "0.0005f", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
	public Float pruningThreshold = 0.0005f;

	@CommandLine.Option(names = "--maxIterations", description = "Maximum iterations.", defaultValue = "250", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
	public Integer maxIterations = 250;

	@CommandLine.Option(names = "--shiftThreshold", description = "Shift threshold.", defaultValue = "0.0001f", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
	public Float shiftThreshold = 0.0001f;

	@CommandLine.Option(names = "--regularized", description = "Use R-MCL.", defaultValue = "true", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
	public Boolean regularized = true;

	@CommandLine.Option(names = {
			"--input" }, description = "Full path to the input spreadsheet (CSV, TSV, or Excel) with header and three columns with any name (node a, node b, weight)", required = true)
	public String input;

	@CommandLine.Option(names = { "--output" }, description = "Full path to the output file i.e. /path/to/output.tsv", required = true)
	public String output;

	@Override
	public Integer call() throws Exception {

		long start = System.currentTimeMillis();
		LOG.info("Starting Processing");

		MCLRunner mcl = new MCLRunner(this);

		MCLResults mclResults = mcl.run(new SpreadsheetDataSource(getInput()));

		String inflationSuffix = "_I" + String.format("%.2f", getInflation());
		String typeSuffix = isRegularized() ? "RMCL" : "MCL";
		String output = getOutput() + inflationSuffix + "_" + typeSuffix;
		try (FileWriter results = new FileWriter(output + "_id_to_cluster.tsv")) {

			mclResults.iterateResuts(mclCluster -> {
				try {
					for (String node : mclCluster.getNodes()) {
						results.write(mclCluster.getId() + "\t" + node + "\n");
					}
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}

		try (FileWriter results = new FileWriter(output + "_clusters.txt")) {
			mclResults.iterateResuts(mclCluster -> {
				try {
					for (String node : mclCluster.getNodes()) {
						results.write(node + " ");
					}
					results.write("\n");
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}

		try (FileWriter results = new FileWriter(output + "_cluster_size.txt")) {
			mclResults.iterateResuts(mclCluster -> {
				try {
					results.write(mclCluster.getSize() + "\n");
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}

		long end = System.currentTimeMillis();
		LOG.info("Finished Processing");
		LOG.info("Generated " + mclResults.getNumberOfClusters() + " clusters");
		LOG.info("Took " + String.format("%.2f", (end - start) / 1000.0) + " seconds");

		return CommandLine.ExitCode.OK;
	}

	public static void main(String[] args) {
		PICOCommonCmd.runCommandLine(new MCLRun(), args);
	}

	@Override
	public float getInflation() {
		return inflation;
	}

	@Override
	public float getLoop() {
		return loop;
	}

	@Override
	public float getPruningThreshold() {
		return pruningThreshold;
	}

	@Override
	public int getMaxIterations() {
		return maxIterations;
	}

	@Override
	public float getShiftThreshold() {
		return shiftThreshold;
	}

	@Override
	public boolean isRegularized() {
		return regularized;
	}

	@Override
	public String getInput() {
		return input;
	}

	@Override
	public String getOutput() {
		return output;
	}
}


