package gov.nih.opa.mcl;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;


public class MCLCmd {
	private static final Logger LOG = LoggerFactory.getLogger(MCLCmd.class);

	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();

		JCommanderMCLParameters params = new JCommanderMCLParameters();
		JCommander jCommander = JCommander.newBuilder().addObject(params).build();

		try {
			jCommander.parse(args);
		}
		catch (ParameterException e) {
			System.err.println(e.getMessage());
			e.usage();
			System.exit(2);
		}

		MCL mcl = new MCL(params);

		MCLResults mclResults = mcl.run(new SpreadsheetDataSource(params.getInput()));

		String inflationSuffix = "_I" + String.format("%.2f", params.getInflation());
		String typeSuffix = params.isRegularized() ? "RMCL" : "MCL";
		String output = params.getOutput() + inflationSuffix + "_" + typeSuffix;
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

		LOG.info("Generated " + mclResults.getNumberOfClusters() + " clusters");
		LOG.info("Took " + String.format("%.2f", (end - start) / 1000.0) + " seconds");

	}
}
