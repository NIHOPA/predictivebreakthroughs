# Predictive Breakthroughs

This repository contains code to reproduce the results of the *Prediction of transformative breakthroughs in biomedical research manuscript*.  This includes the co-citation network calculation for all of PubMed, the R-MCL clustering of the co-cocitation networks, the trajectory generation, and the logistic regression and post regression processing for identification of breakthrough signals.

In addition, it includes code that can be used independently for co-citation network (CCN) computation and MCL/R-MCL (Regularized Markov Clustering Algorithm) clustering.
The CCN code has been tested on the entire nih open citation collection with more than 25 million cited publications and 750 million citations.
The R-MCL code algorithm has been tested on networks with more than 25 million nodes and 1 billion edges. 

For the CCN that includes all articles through the end of 2017, this requires 14.7 billion computations at ~500k/sec, the equivalent of approximately eight hours run time on an AWS EC2 server.  The computation requires at least 384GB of RAM, making computation on a desktop computer not feasible. 

### Setup Environment

* Checkout code from this GitHub repository
* Install Java 21 or later
* Install mongodb 7.x or later
* Restore citations database snapshot
    * Download data from https://figshare.com/s/ca05c0539e77fb2a1dc1
    * mongoimport cited_2019_snapshot-f39ea.json --db cocitations --collection cited
    * the first time the code runs it will create an index on citesPmid which will take some time
* Compile code
    * from the command line `./gradlew installDist`

### Run CCN calculation

* If using mongodb that is not local set `mongoURI` java system property to desired url (defaults to `mongodb://localhost:27017`)
* `./ccnCmd/build/install/ccnCmd/bin/ccn --output /path/to/output/ccn.tsv --threads threads --threshold thresholdToWrite --cacheSize cacheSize --maxYear maxYear`
    * set threads to the number of threads that you wish to use for the calculation
    * set threshold to the minimum edge weight to be written to the csv (0.35 used by manuscript and is recommended)
    * cacheSize determines the amount of CCN that are cached in memory. Set cacheSize based on the amount of RAM available
        * To cache all publications and achieve maximum performance for the most recent year set to 20000000
            * requires at least 175GB of RAM
            * Specific `JAVA_OPTS="-Xmx175g"` at the start of the command line, set the environment variable or edit the bash script
        * Good performance can be achieved at cache size of 10000000
            * Specific `JAVA_OPTS="-Xmx85g"` after java in the command line, set the environment variable or edit the bash script
        * In other cases adjust cache size based on memory available
    * maxYear filters the citation network to the year in question. Stepping the max year through a range over a series of runs allows the analysis of the CCN
      network over time

### Run R-MCL calculation
* `./mclCmd/build/install/mclCmd/bin/mcl run --input /path/to/output/ccn.tsv --output /path/to/output/rmcl --inflation 1.2 --maxIterations 500`
    * The recommend inflation is 1.2 to match the results of the manuscript. Inflation the amount of granularity in clustering with higher inflation values
      causing more broken up clusters
    * The recommend max iterations is 500 to match the results of the manuscript. This parameter determines the maximum iterations of the RMCL algorithm are run
      before terminating
    * If running latest year set JVM size to 350g, earlier years take far less
      * Specific `JAVA_OPTS="-Xmx350g"` at the start of the command line, set the environment variable or edit the bash script

# Automation

* The run_all_cnn_and_rmcl.sh script is provided as a helper to run the CCN and RMCL calculations with the values in the manuscript for a year range.

### Trajectory generation

* The trajectory_code_r subfolder contains the R scripts needed to generate trajectory information (and the cluster_node_attr summary). config.ini contains the necessary path information, after which run scripts in numerical order (1_, 2_, etc)

### Breakthrough candidate generation

* Logistic regression and breakthrough candidate generation scripts are in the breakthrough_candidates_code_py subfolder. Folder paths to the breakthrough data, trajectory output and regression/candidate output will need to be provided in the code, after which run logistic_regression.py to train a regressor and predict candidates for the SetA/B period, and breakthrough_candidates.py to filter them for breakthrough signals.

# License

Use of this software requires a license.  No rights are granted beyond the view access permitted by the GitHub Terms and Conditions.  Please contact the authors for a license agreement if you wish to use this software.
