package gov.nih.opa.ccn.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.koloboke.collect.map.IntFloatMap;
import com.koloboke.collect.map.hash.HashIntFloatMaps;
import com.koloboke.collect.set.IntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntConsumer;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lte;

/**
 * Base class for interacting with the mongo database of citations
 * Use mongoURI property to set path to mongodb
 */
public class MongoCited {

	private static final Logger LOG = LoggerFactory.getLogger(MongoCited.class);
	public static final String CITED_DATABASE = "cocitations";
	public static final String CITED_COLLECTION = "cited";
	public static final String ID = "_id";
	public static final String CITES_PMID = "citesPmid";
	public static final String PUB_YEAR = "pubYear";
	private static final MongoCited INSTANCE = new MongoCited();
	private final MongoClient mongo;
	private final Cache<Integer, IntFloatMap> citedCache = CacheBuilder.newBuilder().maximumSize(100000).build();

	private MongoCited() {
		String mongoURI = System.getProperty("mongoURI");

		if (mongoURI == null) {
			LOG.info("mongoURI property not set, defaulting mongo to localhost on default port");
			mongoURI = "mongodb://localhost:27017";
		}

		MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(new ConnectionString(mongoURI)).build();

		mongo = MongoClients.create(settings);
		createIndexes();
	}

	public static MongoCited getMongoCited() {
		return INSTANCE;
	}

	public Set<Integer> getAllCitedPmids() {
		return getAllCitedPmids(null);
	}

	public Set<Integer> getAllCitedPmids(Integer maxYear) {
		Set<Integer> allCitedPmids = new TreeSet<>();

		MongoCollection<Document> citedCollection = MongoCited.getMongoCited().getCitedCollection();

		FindIterable<Document> documents;
		if (maxYear != null) {
			documents = citedCollection.find(lte(PUB_YEAR, maxYear));
		}
		else {
			documents = citedCollection.find();
		}

		Set<Integer> pmidsFromYears = new TreeSet<>();

		for (Document doc : documents) {
			int pmid = doc.getInteger("_id");
			pmidsFromYears.add(pmid);

			List<Integer> cocitedList = doc.getList(MongoCited.CITES_PMID, Integer.class);
			if (cocitedList != null) {
				allCitedPmids.addAll(cocitedList);
			}
		}

		LOG.info("Found " + allCitedPmids.size() + " PMIDs cited by pubs with year <= " + maxYear);
		allCitedPmids.retainAll(pmidsFromYears);
		LOG.info("Filtered to " + allCitedPmids.size() + " PMIDs cited by pubs with year <= " + maxYear);

		return allCitedPmids;
	}

	public void createIndexes() {
		getCitedCollection().createIndex(new Document(CITES_PMID, 1));
	}

	public long getCitedForPaperByYear(int pmid, int maxYear) {
		Bson query = and(lte(PUB_YEAR, maxYear), eq(CITES_PMID, pmid));
		return getCitedCollection().countDocuments(query);
	}

	public List<Integer> getPapersByYear(int year) {
		Bson query = eq(PUB_YEAR, year);
		List<Integer> pmids = new ArrayList<>();
		for (Document document : getCitedCollection().find(query).projection(Projections.include("_id"))) {
			pmids.add(document.getInteger("_id"));
		}
		return pmids;
	}

	public MongoCollection<Document> getCitedCollection() {
		MongoDatabase citations = getCocitationDatabase();
		return citations.getCollection(CITED_COLLECTION);
	}

	private MongoDatabase getCocitationDatabase() {
		return mongo.getDatabase(CITED_DATABASE);
	}

	public SparseVector getCitesVector(int id) {
		IntFloatMap intFloatMap = getCitesVectorMap(id);

		return new SparseVector(id, intFloatMap);
	}

	public IntFloatMap getCitesVectorMap(int id) {
		IntFloatMap intDoubleMap = HashIntFloatMaps.newMutableMap();

		MongoCollection<Document> citedCollection = getCitedCollection();
		Document query = new Document(ID, id);
		Document matchingDoc = citedCollection.find(query).first();

		if (matchingDoc != null) {
			if (matchingDoc.containsKey(CITES_PMID) && matchingDoc.get(CITES_PMID) != null) {

				List<Integer> cocitedList = matchingDoc.getList(CITES_PMID, Integer.class);
				for (int cocited : cocitedList) {
					intDoubleMap.addValue(cocited, 1);
				}
			}
		}
		return intDoubleMap;
	}

	public SparseVector getCitedVector(int id) {

		IntFloatMap intDoubleMap = getCitedVectorMap(id);
		return new SparseVector(id, intDoubleMap);
	}

	public IntFloatMap getCitedVectorMap(int id) {

		try {
			return citedCache.get(id, () -> {
				IntFloatMap intDoubleMap = HashIntFloatMaps.newMutableMap();

				MongoCollection<Document> citedCollection = getCitedCollection();
				Document query = new Document(CITES_PMID, id);
				FindIterable<Document> matchingDocs = citedCollection.find(query);

				for (Document matchingDoc : matchingDocs) {
					intDoubleMap.addValue(matchingDoc.getInteger(ID), 1);
				}
				return intDoubleMap;
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public SparseVector getCocitationVector(int id, boolean useRefScaling) {
		return getCocitationVector(id, useRefScaling, null);
	}

	public SparseVector getCocitationVector(int id, boolean useRefScaling, Integer maxYear, Set<Integer> allowedIds, IntConsumer filteredConsumer) {
		IntFloatMap intFloatMap = getCocitationVectorMap(id, useRefScaling, maxYear);

		IntSet filtered = HashIntSets.newMutableSet();
		for (int ccnId : intFloatMap.keySet()) {
			if (!allowedIds.contains(ccnId)) {
				filtered.add(ccnId);
			}
		}
		for (int ccnIdToFilter : filtered) {
			intFloatMap.remove(ccnIdToFilter);
			filteredConsumer.accept(ccnIdToFilter);
		}

		return new SparseVector(id, intFloatMap);
	}

	public SparseVector getCocitationVector(int id, boolean useRefScaling, Integer maxYear) {
		IntFloatMap intFloatMap = getCocitationVectorMap(id, useRefScaling, maxYear);

		return new SparseVector(id, intFloatMap);
	}

	public IntFloatMap getCocitationVectorMap(int id, boolean useRefScaling) {
		return getCocitationVectorMap(id, useRefScaling, null);
	}

	public IntFloatMap getCocitationVectorMap(int id, boolean useRefScaling, Integer maxYear) {
		IntFloatMap intFloatMap = HashIntFloatMaps.newMutableMap();

		MongoCollection<Document> citedCollection = getCitedCollection();
		Bson query;
		if (maxYear != null) {
			query = and(lte(PUB_YEAR, maxYear), eq(CITES_PMID, id));
		}
		else {
			query = eq(CITES_PMID, id);
		}
		FindIterable<Document> matchingDocs = citedCollection.find(query);

		for (Document matchingDoc : matchingDocs) {
			if (matchingDoc.containsKey(CITES_PMID) && matchingDoc.get(CITES_PMID) != null) {

				List<Integer> cocitedList = matchingDoc.getList(CITES_PMID, Integer.class);

				double citationScaleFactor = 1.0;

				if (useRefScaling) {
					citationScaleFactor = citationScaleFactor(cocitedList.size(), 1, 0.025, 200);
				}

				for (int cocited : cocitedList) {
					intFloatMap.addValue(cocited, (float) citationScaleFactor);
				}
			}
		}
		return intFloatMap;
	}

	public static double citationScaleFactor(int numberOfReferences, double l, double k, int x0) {
		return 1 - (l / (1 + Math.exp(-k * (numberOfReferences - x0))));
	}

	public SparseVector getBibliographicCouplingVector(int id) {
		IntFloatMap intFloatMap = getBibliographicCouplingVectorMap(id);

		return new SparseVector(id, intFloatMap);
	}

	public IntFloatMap getBibliographicCouplingVectorMap(int id) {
		IntFloatMap intFloatMap = HashIntFloatMaps.newMutableMap();

		MongoCollection<Document> citedCollection = getCitedCollection();
		List<Integer> citesList = Collections.emptyList();

		{
			Document query = new Document(ID, id);
			Document matchingDoc = citedCollection.find(query).first();

			if (matchingDoc != null) {
				if (matchingDoc.containsKey(CITES_PMID) && matchingDoc.get(CITES_PMID) != null) {
					citesList = matchingDoc.getList(CITES_PMID, Integer.class);
				}
			}
		}

		for (Integer cite : citesList) {

			IntFloatMap cited = getCitedVectorMap(cite);
			for (int pmid : cited.keySet()) {
				intFloatMap.addValue(pmid, 1);
			}

		}
		return intFloatMap;
	}

}
