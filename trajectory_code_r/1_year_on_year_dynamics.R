library(data.table)
library(snow)
source("./utils.R")

configs = read_configs()

dir.create(configs$clusters_out_path)
dir.create(configs$analysis_path)
dir.create(configs$finalized_path)
dir.create(configs$trajectories_out_path)

meta_orig <- fread(configs$pubyear_in_file) 
setkey(meta_orig, pmid)
meta = meta_orig[,1:2]

print(meta)

cluster_stability_mean = function(path_cat) {
  library(data.table)
  # Next year is "next", this year is "prev", and year before is "anc" for "ancestor"
  path_prev = strsplit(path_cat, "|", fixed=T)[[1]][1]
  path_next = strsplit(path_cat, "|", fixed=T)[[1]][2]
  path_anc = strsplit(path_cat, "|", fixed=T)[[1]][3]
  y_prev = as.integer(strsplit(path_prev, "[_/]")[[1]][6])
  y_next = y_prev + 1
  y_anc =  y_prev - 1
  j = y_prev - 1979

  print('Years:')
  print(c(y_anc,y_prev,y_next,j))

  # Load and parse historical clusters and their mebmership
  
  clust_prev_lines <- readLines(path_prev)
  clust_prev = strsplit(clust_prev_lines, ' ', fixed=T)
  clust_prev = lapply(clust_prev, as.integer)
  
  clust_prev_dt = data.table(pmid=1:length(clust_prev), members=clust_prev)
  setkey(clust_prev_dt, pmid)
  
  clust_prev_df = data.frame("id"=1:length(clust_prev_lines), "members"=clust_prev_lines, stringsAsFactors = F)
  clust_prev_2col = as.data.table(explode(clust_prev_df, val_col = 2, val_delim = " ", verbose = T, recast=T))
  
  setkey(clust_prev_2col, members)
  
  clust_next_lines <- readLines(path_next)
  clust_next = strsplit(clust_next_lines, ' ', fixed=T)
  clust_next = lapply(clust_next, as.integer)
  
  clust_next_dt = data.table(pmid=1:length(clust_next), members=clust_next)
  setkey(clust_next_dt, pmid)
  
  clust_next_df = data.frame("id"=1:length(clust_next_lines), "members"=clust_next_lines, stringsAsFactors = F)
  clust_next_2col = as.data.table(explode(clust_next_df, val_col = 2, val_delim = " ", verbose = T, recast=T))
  
  setkey(clust_next_2col, members)
  
  clust_anc_lines <- readLines(path_anc)
  clust_anc = strsplit(clust_anc_lines, ' ', fixed=T)
  clust_anc = lapply(clust_anc, as.integer)
  
  clust_anc_dt = data.table(pmid=1:length(clust_anc), members=clust_anc)
  setkey(clust_anc_dt, pmid)
  
  clust_anc_df = data.frame("id"=1:length(clust_anc_lines), "members"=clust_anc_lines, stringsAsFactors = F)
  clust_anc_2col = as.data.table(explode(clust_anc_df, val_col = 2, val_delim = " ", verbose = T, recast=T))
  
  setkey(clust_anc_2col, members)
  
  # Filter to only include clusters of specified size (was 50, now is 10)
  
  clust_prev_50plus = clust_prev[sapply(clust_prev, length) >= 10]
  clust_next_50plus = clust_next[sapply(clust_next, length) >= 10]
  clust_anc_50plus = clust_anc[sapply(clust_anc, length) >= 10]
  
  data = NULL
  for (i in 1:length(clust_prev_50plus)) {
    if (i == 1) print(i/length(clust_prev_50plus))
    if (i == 10) print(i/length(clust_prev_50plus))
    if (i == 25) print(i/length(clust_prev_50plus))
    if (i == 50) print(i/length(clust_prev_50plus))
    if (i == 100) print(i/length(clust_prev_50plus))
    if (i %% 300 == 0) print(i/length(clust_prev_50plus))
    # Get features about this cluster
    members_prev = clust_prev_50plus[[i]]
    pct_is_new = mean(meta[pmid %in% members_prev,pubYear] >= (y_prev-1), na.rm=T)
    pct_is_newish = mean(meta[pmid %in% members_prev,pubYear] >= (y_prev-2) &
                           meta[pmid %in% members_prev,pubYear] <= (y_prev-1), na.rm=T)
    # Get features for top 2 clusters from next year with largest overlap
    endedup_dt = clust_next_2col[members %in% members_prev,]
    if (nrow(endedup_dt) == 0) {
      data_next = data.frame("cluster"=i-1, "year"=y_prev, "n"=length(members_prev), "pct_is_new"=pct_is_new,
                             "pct_is_newish"=pct_is_newish, "pct_dusted_ccn"=NA, "pct_dusted_rmcl"=NA,
                             "biggest"=NA, "pct_in_biggest"=NA, "pct_of_biggest"=NA, "pct_of_biggest_new"=NA,
                             "pct_of_biggest_newish"=NA, "secbiggest"=NA, "pct_in_secbiggest"=NA,
                             "pct_of_secbiggest"=NA, "pct_of_secbiggest_new"=NA,
                             "pct_of_secbiggest_newish"=NA, "n_clusts_90"=NA, "pct_top_5"=NA)
    } else {
      tab = sort(table(endedup_dt[,id]), decreasing = T)
      tab_dusted = tab[which(as.integer(names(tab)) > length(clust_next_50plus))]
      pct_dusted_rmcl = sum(tab_dusted)/length(members_prev)
      pct_dusted_ccn = 1-sum(tab)/length(members_prev)
      biggest = as.integer(names(tab[1]))
      secbiggest = as.integer(names(tab[2]))
      pct_in_biggest = tab[1]/length(members_prev)
      pct_in_secbiggest = tab[2]/length(members_prev)
      pct_of_biggest = tab[1]/length(clust_next[[biggest]])
      pct_of_secbiggest = tab[2]/length(clust_next[[secbiggest]])
      yrs = meta[pmid %in% clust_next[[biggest]],pubYear]
      yrssec = meta[pmid %in% clust_next[[secbiggest]],pubYear]
      pct_of_biggest_new = mean(yrs >= (y_next-1), na.rm = T)
      pct_of_biggest_newish = mean(yrs >= (y_next-2) & 
                                     yrs <= (y_next-1), na.rm = T)
      pct_of_secbiggest_new = mean(yrssec >= (y_next-1), na.rm = T)
      pct_of_secbiggest_newish = mean(yrssec >= (y_next-2) &
                                        yrssec <= (y_next-1), na.rm = T)
      n_clusts_90 = which(cumsum(tab) >= .9 * length(members_prev))[1]
      pct_top_5 = cumsum(tab)[min(5, length(tab))] / length(members_prev)
      data_next = data.frame("cluster"=i-1, "year"=y_prev, "n"=length(members_prev), pct_is_new, pct_is_newish,
                             pct_dusted_ccn, pct_dusted_rmcl, "biggest"=biggest-1, pct_in_biggest,
                             pct_of_biggest, pct_of_biggest_new, pct_of_biggest_newish,
                             "secbiggest"=secbiggest-1, pct_in_secbiggest, pct_of_secbiggest,
                             pct_of_secbiggest_new, pct_of_secbiggest_newish, n_clusts_90, pct_top_5)
    }
    
    # Get features for top 2 clusters from last year with largest overlap
    endeddown_dt = clust_anc_2col[members %in% members_prev,]
    if (nrow(endeddown_dt) == 0) {
      data_anc = data.frame("biggest_anc"=NA, "n_biggest_anc"=NA, 
                            "pct_in_biggest_anc"=NA, "pct_of_biggest_anc"=NA, "pct_of_biggest_anc_new"=NA,
                            "pct_of_biggest_anc_newish"=NA, "secbiggest_anc"=NA, "n_secbiggest_anc"=NA,
                            "pct_in_secbiggest_anc"=NA, "pct_of_secbiggest_anc"=NA,
                            "pct_of_secbiggest_anc_new"=NA, "pct_of_secbiggest_anc_newish"=NA)
    } else {
      tab_anc = sort(table(endeddown_dt[,id]), decreasing = T)
      biggest_anc = as.integer(names(tab_anc[1]))
      secbiggest_anc = as.integer(names(tab_anc[2]))
      pct_in_biggest_anc = tab_anc[1]/length(members_prev)
      pct_of_biggest_anc = tab_anc[1]/length(clust_anc[[biggest_anc]])
      pct_in_secbiggest_anc = tab_anc[2]/length(members_prev)
      pct_of_secbiggest_anc = tab_anc[2]/length(clust_anc[[secbiggest_anc]])
      yrs_anc = meta[pmid %in% clust_anc[[biggest_anc]],pubYear]
      yrssec_anc = meta[pmid %in% clust_anc[[secbiggest_anc]],pubYear]
      pct_of_biggest_anc_new = mean(yrs_anc >= (y_anc-1), na.rm = T)
      pct_of_biggest_anc_newish = mean(yrs_anc >= (y_anc-2) &
                                         yrs_anc <= (y_anc-1), na.rm = T)
      pct_of_secbiggest_anc_new = mean(yrssec_anc >= (y_anc-1), na.rm = T)
      pct_of_secbiggest_anc_newish = mean(yrssec_anc >= (y_anc-2) &
                                            yrssec_anc <= (y_anc-1), na.rm = T)
      data_anc = data.frame("biggest_anc"=biggest_anc-1, "n_biggest_anc"=length(clust_anc[[biggest_anc]]), 
                            pct_in_biggest_anc, pct_of_biggest_anc, pct_of_biggest_anc_new,
                            pct_of_biggest_anc_newish, "secbiggest_anc"=secbiggest_anc-1,
                            "n_secbiggest_anc"=length(clust_anc[[secbiggest_anc]]),
                            pct_in_secbiggest_anc, pct_of_secbiggest_anc, pct_of_secbiggest_anc_new,
                            pct_of_secbiggest_anc_newish)
    }
    
    data = rbind(data, cbind(data_next, data_anc, stringsAsFactors=F))
    # Write progress and temp file
    if (i %% 500 == 0) write.table((nrow(data)/length(clust_prev_50plus)),
                                   paste0(configs$clusters_out_path, j+1979,
                                          "prog.txt"), row.names=F, col.names=F) 
    if (i == 2500) {
      # Break at 2500 if test_only is set to True in configs
      write.csv(data, paste0(configs$clusters_out_path, j+1979,
                                          "_cluster_stability_small.csv"), row.names=F, na="")
      if (as.logical(configs$test_only)) return(data)
    }
  }
  # Write
  write.csv(data, paste0(configs$clusters_out_path, j+1979,
                         "_cluster_stability.csv"), row.names=F, na="")
  return(data)
}

paths = list.files(configs$clusters_in_path, "clusters.txt", full.names = T)
paths_concat = NULL
print(paths)
for (j in 1:40) {
  year_prev = 1979 + j
  year_next = year_prev + 1
  year_anc = year_prev - 1
  path_prev = grep(paste0(year_prev, "_2023-04-30_0.35_"), paths, fixed=T, value=T)[1]
  path_next = grep(paste0(year_next, "_2023-04-30_0.35_"), paths, fixed=T, value=T)[1]
  path_anc = grep(paste0(year_anc, "_2023-04-30_0.35_"), paths, fixed=T, value=T)[1]
  paths_concat = c(paths_concat, paste0(path_prev, "|", path_next, "|", path_anc))
  print(strsplit(path_prev, "[/_]")[[1]])      
  print("Detected year:")
  print(as.integer(strsplit(path_prev, "[_/]")[[1]][5]))
}
print("Detected paths:")
print(paths_concat)

cl = makeCluster(1)
clusterExport(cl, c("meta", "explode", "configs"))

data_list = parLapply(cl, paths_concat, cluster_stability_mean)
save(data_list, file=paste0(configs$analysis_path, "data_list.rdata"))
stopCluster(cl)

#for (p in rev(paths_concat)) {
#  print(p)
#  cluster_stability_mean(p)
#}
