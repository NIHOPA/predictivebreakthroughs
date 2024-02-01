#!/bin/bash
date=2019-02-01
# set threads to number of cores / threads available
threads=64
cache_size=20000000
threshold=0.35
inflation=1.2
max_iter=500

jar_dir=/data/predictive_breakthroughs/util
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
  java -Xmx${heap} -jar ${jar_dir}/citationNetworkCompute-${jar_version}-all.jar ${ccn_output} ${threads} ${threshold} ${cache_size} ${year} &>${ccn_log}
  java -Xmx${heap} -jar ${jar_dir}/mcl-${jar_version}-all.jar --in ${ccn_output} --out ${rmcl_output} --inflation ${inflation} --max ${max_iter} &>${rmcl_log}
done
