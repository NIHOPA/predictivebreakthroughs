package gov.nih.opa.ccn.driver;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.koloboke.collect.set.IntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import gov.nih.opa.ccn.common.MongoCited;
import gov.nih.opa.ccn.common.SimConfig;
import gov.nih.opa.ccn.common.SimType;
import gov.nih.opa.ccn.common.SparseVector;
import gov.nih.opa.ccn.common.WorkPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Writer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Matt Davis on 6/27/17.
 * Computes co citation network of all PMIDs in the database vs the PMIDs they were cited with (first order CCN)
 * In a 50k random sample that was tested, 92% of edges in testing above 0.35 similarity exist in the first order network while requiring only ~1k CCN calculations instead of ~20 million
 * Max Year can be set to get a point in time view of the CCN
 * A cache size of 10_000_000 or more is recommended but 20_000_000 is ideal (fully cached).  With 20_000_000 cache size this needs around a 175GB heap
 * @author mdavis
 */
public class CocitationVectorComputeDriverAllVsCCN {

	private static final Logger LOG = LoggerFactory.getLogger(CocitationVectorComputeDriverAllVsCCN.class);

	public static void computeFirstOrderCCN(int threads, int cacheSize, double thresholdToWrite, Writer writer, Set<Integer> allPmids, Integer maxYear)
			throws Exception {
		LOG.info("Total Pmids to Compare Against: " + allPmids.size());

		AtomicLong counter = new AtomicLong();

		IntSet filteredPmids = HashIntSets.newMutableSet();
		try (WorkPool workPool = new WorkPool(threads)) {

			SimConfig simConfig = new SimConfig(Collections.singletonList(SimType.COSINE), thresholdToWrite, '\t');
			writer.write(simConfig.getCSVHeader());

			MongoCited mongoCited = MongoCited.getMongoCited();

			CacheLoader<Integer, SparseVector> loader = new CacheLoader<>() {
				@Override
				public SparseVector load(Integer key) {
					return mongoCited.getCocitationVector(key, true, maxYear, allPmids, filteredPmids::add);
				}
			};
			LoadingCache<Integer, SparseVector> cache = CacheBuilder.newBuilder().maximumSize(cacheSize).concurrencyLevel(threads).build(loader);

			for (int pmid : allPmids) {

				workPool.executeAsync(() -> {
					SparseVector citationVector = cache.get(pmid);

					for (int inCcn : citationVector.getIndexes()) {
						SparseVector inCcnVector = cache.get(inCcn);

						String csvLine = simConfig.getCSVLine(citationVector, inCcnVector);
						if (csvLine != null) {
							writer.write(csvLine);
						}

					}

					long n = counter.getAndIncrement();
					if (n % 10000 == 0) {
						LOG.info("Processed: " + n + " Cache Size: " + cache.size() + " Filtered: " + filteredPmids.size());

						int mb = 1024 * 1024;

						Runtime runtime = Runtime.getRuntime();
						System.gc();
						LOG.info("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);

					}

					return null;
				});

			}
		}
		LOG.info("Filtered <" + filteredPmids.size() + "> ids");

	}

}
