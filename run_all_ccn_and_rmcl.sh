#!/bin/bash
date=2023-04-20
threads=64
cache_size=40000000
threshold=0.35
inflation=1.2
max_iter=500
#ccn need at least 175G
#rmcl needs unknown but high

jar_dir=/data/emerging_areas/util
jar_version=0.4.0
out_dir=/data/emerging_areas/${date}-ccn
mkdir ${out_dir}

heap=350g
for year in {2018..2021}
do
  ccn_output=${out_dir}/${year}_${date}_${threshold}_ccn.tsv
  rmcl_output=${out_dir}/${year}_${date}_${threshold}_rmcl_${inflation}
  ccn_log=${out_dir}/${year}_${date}__ccn.log
  rmcl_log=${out_dir}/${year}_${date}__rmcl.log
  java -Xmx${heap} -jar ${jar_dir}/citationNetworkCompute-${jar_version}-all.jar ${ccn_output} ${threads} ${threshold} ${cache_size} ${year} &>${ccn_log}
  java -Xmx${heap} -jar ${jar_dir}/mcl-${jar_version}-all.jar --in ${ccn_output} --out ${rmcl_output} --inflation ${inflation} --max ${max_iter} &>${rmcl_log}
done
