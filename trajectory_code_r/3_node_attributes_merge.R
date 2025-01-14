library(data.table)
library(fastmatch)
source("./utils.R")

configs = read_configs()

meta <- fread(configs$pubyear_in_file)

ianpath = configs$clusters_out_path
mergedata = NULL

# Pull and merge data
for (y in 1980:2019) {
  print(y)

  #CHANGE THIS v TO MATCH RMCL FILE FORMAT
  pmids = readLines(paste0(configs$clusters_in_path, y, "_RMCLDATE_0.35_rmcl_1.2_I1.20_RMCL_clusters.txt"))
  pmids = lapply(strsplit(pmids, " ", fixed=T), as.integer)
  names(pmids) = paste(0:(length(pmids)-1), y, sep="_")
  wrcrs = sapply(lapply(pmids, function(x) meta$rcr[fmatch(x, meta$pmid)]), sum, na.rm=T)
  
  iandata = fread(paste0(ianpath, y, "_cluster_stability.csv"), data.table=F)
  colnames(iandata)[which(colnames(iandata) == "pct_is_new")] = "pctNew"
  iandata$cluster = paste(iandata$cluster, iandata$year, sep="_")
  iandata$biggest = paste(iandata$biggest, iandata$year + 1, sep="_")
  iandata$secbiggest = paste(iandata$secbiggest, iandata$year + 1, sep="_")
  iandata$biggest_anc = paste(iandata$biggest_anc, iandata$year - 1, sep="_")
  iandata$secbiggest_anc = paste(iandata$secbiggest_anc, iandata$year - 1, sep="_")
  mergedata = rbind(mergedata, iandata)
}
#limit to columns used in later breakthroughs steps
mergedata = mergedata[c("cluster","year","n","pctNew","biggest","secbiggest","biggest_anc","pct_of_biggest_anc_new","secbiggest_anc","pct_in_biggest_anc","pct_in_secbiggest_anc")]

# Write
write.csv(mergedata, paste0(configs$finalized_path, "cluster_node_attr.csv"), row.names=F, na="")
