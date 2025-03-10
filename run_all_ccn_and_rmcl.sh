#!/bin/bash
date=2019-02-01
# set threads to number of cores / threads available
threads=64
cache_size=20000000
threshold=0.35
inflation=1.2
max_iter=500

jar_dir=/data/predictive_breakthroughs/util
ccn_scripts_dir=ccnCmd/build/install/ccnCmd/bin
mcl_scripts_dir=mclCmd/build/install/mclCmd/bin
jar_version=1.0.1-SNAPSHOT
out_dir=/data/predictive_breakthroughs/${date}-ccn
mkdir ${out_dir}

#ccn need at least 175GB
#rmcl needs at least 350GB
heap=350g
for year in {1980..2017}
do
  ccn_output=${out_dir}/${year}_${date}_${threshold}_ccn.tsv
  rmcl_output=${out_dir}/${year}_${date}_${threshold}_rmcl_${inflation}
  ccn_log=${out_dir}/${year}_${date}__ccn.log
  rmcl_log=${out_dir}/${year}_${date}__rmcl.log
  JAVA_OPTS="-Xmx175g" ./${ccn_scripts_dir}/ccn ccnFirstOrder --output ${ccn_output} --threads ${threads} --threshold ${threshold} --cacheSize ${cache_size} --maxYear ${year} &>${ccn_log}
  JAVA_OPTS="-Xmx350g" ./${mcl_scripts_dir}/mcl run --input ${ccn_output} --output ${rmcl_output} --inflation ${inflation} --maxIterations ${max_iter} &>${rmcl_log}
done
