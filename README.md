# Predictive Breakthroughs

### Setup Environment

* Checkout code from this GitHub repository
* Install Java 21 or later
* Install mongodb 7.x or later
* Restore citations database snapshot
    * Use 2019-02 citations database to match the data used in the manuscript
    * wget https://icite.od.nih.gov/covid19/dba8f5/af95/cited_2019_snapshot-f39ea.json
    * mongoimport cited_2019_snapshot-f39ea.json -db cocitations --collection cited
    * the first time the code runs it will create an index on citesPmid which will take some time
* Compile code
    * from the command line `./gradlew`

### Run CCN calculation

* If using mongodb that is not local set `mongoURI` java system property to desired url (defaults to `mongodb://localhost:27017`)
* `./ccnCmd/build/install/bin/ccn ccnFirstOrder --output /path/to/output/ccn.tsv --threads threads --threshold thresholdToWrite --cacheSize cacheSize --maxYear maxYear`
    * set threads to the number of threads that you wish to use for the calculation
    * set threshold to the minimum edge weight to be written to the csv (0.35 used by manuscript and is recommended)
    * cacheSize determines the amount of CCN that are cached in memory. Set cacheSize based on the amount of RAM available
        * To cache all publications and achieve maximum performance for the most recent year set to 20000000
            * requires at least 175GB of RAM
            * Specific `-Xmx175g` after java in the command line
        * Good performance can be achieved at cache size of 10000000
            * Specific `-Xmx85g` after java in the command line
        * In other cases adjust cache size based on memory available
    * maxYear filters the citation network to the year in question. Stepping the max year through a range over a series of runs allows the analysis of the CCN
      network over time

### Run MCL calculation

* `./mclCmd/build/install/bin/mcl run --input /path/to/output/ccn.tsv --output /path/to/output/rmcl --inflation 1.2 --maxIterations 500`
    * The recommend inflation is 1.2 to match the results of the manuscript. Inflation the amount of granularity in clustering with higher inflation values
      causing more broken up clusters
    * The recommend max iterations is 500 to match the results of the manuscript. This parameter determines the maximum iterations of the RMCL algorithm are run
      before terminating

# Automation

* the run_all_cnn_and_rmcl.sh is provided as a helper to run the calculation with the values in the manuscript for a year range
