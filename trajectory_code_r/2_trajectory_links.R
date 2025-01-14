library(data.table)
library(igraph)
library(snow)
source("./utils.R")

configs = read_configs()

ys = 1980:2019

basepath = configs$clusters_out_path
suf = "_cluster_stability.csv"

maj_links = NULL
min_links = NULL
disintegrations = NULL
denovos = NULL
outcome_df = NULL
precedent_df = NULL
for (y in ys) {
  this_year = y
  print(this_year)
  path = paste0(basepath, y, suf)
  data = fread(path, data.table=F)
  filters = list()
  filters[["has_host"]] = data$pct_in_biggest > 0.5 & !is.na(data$pct_in_biggest)
  filters[["comprises_host"]] = data$pct_of_biggest > 0.5 & !is.na(data$pct_of_biggest)
  filters[["comprises_alt"]] = data$pct_of_secbiggest > 0.5 & !is.na(data$pct_of_secbiggest)
  filters[["fwd_one_to_one"]] = filters[["has_host"]] & filters[["comprises_host"]]
  filters[["bud"]] = filters[["has_host"]] & filters[["comprises_host"]] & filters[["comprises_alt"]]
  # Next 2 should be mutually exclusive
  filters[["shed_host"]] = !filters[["has_host"]] & filters[["comprises_host"]] & !filters[["comprises_alt"]]
  filters[["shed_alt"]] = !filters[["has_host"]] & !filters[["comprises_host"]] & filters[["comprises_alt"]]
  summary(filters[["shed_host"]] & filters[["shed_alt"]])
  if (any(filters[["shed_host"]] & filters[["shed_alt"]])) stop("Shed exclusivity test failed")
  filters[["shed_any"]] = filters[["shed_host"]] | filters[["shed_alt"]]
  filters[["split"]] = !filters[["has_host"]] & filters[["comprises_host"]] & filters[["comprises_alt"]]
  filters[["disintegrate"]] = !filters[["has_host"]] & !filters[["comprises_host"]] & !filters[["comprises_alt"]]
  filters[["fragment"]] = filters[["fwd_one_to_one"]] & filters[["comprises_alt"]]
  filters[["captured"]] = filters[["has_host"]] & !filters[["comprises_host"]]
  # Primary outcomes OR should be true in all cases
  # fwd_one_to_one, captured, split, shed, disintegrate
  summary(filters[["fwd_one_to_one"]] | filters[["captured"]] | filters[["split"]] | filters[["shed_any"]] | filters[["disintegrate"]])
  #if (!all(filters[["fwd_one_to_one"]] | filters[["captured"]] |
  #         filters[["split"]] | filters[["shed_any"]] | filters[["disintegrate"]])) stop("Sum of primary outcomes test failed")
  
  primary_outcomes = c("fwd_one_to_one"=mean(filters[["fwd_one_to_one"]]), "captured"=mean(filters[["captured"]]),
                       "split"=mean(filters[["split"]]), "shed_any"=mean(filters[["shed_any"]]),
                       "disintegrate"=mean(filters[["disintegrate"]]))
  if (round(sum(primary_outcomes),2) != 1) stop("Primary outcomes do not sum to 1")
  outcome_df = rbind(outcome_df, primary_outcomes)
  # Major links to keep: fwd_one_to_one, split, shed_host, shed_alt
  # Minor links to keep: captured
  
  filters[["has_anc"]] = data$pct_in_biggest_anc > 0.5 & !is.na(data$pct_in_biggest_anc)
  filters[["comprises_anc"]] = data$pct_of_biggest_anc > 0.5 & !is.na(data$pct_of_biggest_anc)
  filters[["comprises_alt_anc"]] = data$pct_of_secbiggest_anc > 0.5 & !is.na(data$pct_of_secbiggest_anc)
  filters[["back_one_to_one"]] = filters[["has_anc"]] & filters[["comprises_anc"]] 
  filters[["meiosis"]] = filters[["has_anc"]] & !filters[["comprises_anc"]] & filters[["comprises_alt_anc"]]
  filters[["mitosis"]] = filters[["has_anc"]] & !filters[["comprises_anc"]] & !filters[["comprises_alt_anc"]]
  filters[["merger"]] = !filters[["has_anc"]] & filters[["comprises_anc"]] & filters[["comprises_alt_anc"]]
  filters[["planetoid"]] = !filters[["has_anc"]] & filters[["comprises_anc"]] & (!filters[["comprises_alt_anc"]] )
  filters[["denovo"]] = (!filters[["has_anc"]]) & (!filters[["comprises_anc"]]) & (!filters[["comprises_alt_anc"]])
  filters[["charisma"]] = !filters[["has_anc"]] & !filters[["comprises_anc"]] & filters[["comprises_alt_anc"]]
  primary_precedents = c("back_one_to_one"=mean(filters[["back_one_to_one"]], na.rm=T), "split_recomb"=mean(filters[["meiosis"]]),
                         "split_clean" = mean(filters[["mitosis"]]), "merger_two"=mean(filters[["merger"]]),
                         "merger_lots"=mean(filters[["planetoid"]]),
                         "denovo" = mean(filters[["denovo"]]), "minor_merger"=mean(filters[["charisma"]]))
  if (round(sum(primary_precedents),2) != 1) stop("Primary precedents do not sum to 1")
  precedent_df = rbind(precedent_df, primary_precedents)
  # Major links to keep: back_one_to_one, merger, planetoid
  # Minor links to keep: mitosis, meiosis, charisma
  
  # Forward pass first
  these_nodes = paste(data$cluster, this_year, sep="_")
  host_nodes = paste(data$biggest, this_year + 1, sep="_")
  host_sec_nodes = paste(data$secbiggest, this_year + 1, sep="_")
  anc_nodes = paste(data$biggest_anc, this_year - 1, sep="_")
  anc_sec_nodes = paste(data$secbiggest_anc, this_year - 1, sep="_")
  
  df_major_temp = NULL
  df_minor_temp = NULL
  # Forward major
  df_major_temp = rbind(df_major_temp, data.frame("node1"=these_nodes[filters[["fwd_one_to_one"]]],
                                                  "node2"=host_nodes[filters[["fwd_one_to_one"]]], "link"="fwd_one_to_one", stringsAsFactors=F))
  df_major_temp = rbind(df_major_temp, data.frame("node1"=these_nodes[filters[["split"]]],
                                                  "node2"=host_nodes[filters[["split"]]], "link"="split", stringsAsFactors=F))
  df_major_temp = rbind(df_major_temp, data.frame("node1"=these_nodes[filters[["split"]]],
                                                  "node2"=host_sec_nodes[filters[["split"]]], "link"="split", stringsAsFactors=F))
  df_major_temp = rbind(df_major_temp, data.frame("node1"=these_nodes[filters[["shed_host"]]],
                                                  "node2"=host_nodes[filters[["shed_host"]]], "link"="shed", stringsAsFactors=F))
  df_major_temp = rbind(df_major_temp, data.frame("node1"=these_nodes[filters[["shed_alt"]]],
                                                  "node2"=host_sec_nodes[filters[["shed_alt"]]], "link"="shed", stringsAsFactors=F))
  # Backward major
  df_major_temp = rbind(df_major_temp, data.frame("node1"=anc_nodes[filters[["back_one_to_one"]]],
                                                  "node2"=these_nodes[filters[["back_one_to_one"]]], "link"="back_one_to_one", stringsAsFactors=F))
  df_major_temp = rbind(df_major_temp, data.frame("node1"=anc_nodes[filters[["merger"]]],
                                                  "node2"=these_nodes[filters[["merger"]]], "link"="merger_two", stringsAsFactors=F))
  df_major_temp = rbind(df_major_temp, data.frame("node1"=anc_sec_nodes[filters[["merger"]]],
                                                  "node2"=these_nodes[filters[["merger"]]], "link"="merger_two", stringsAsFactors=F))
  df_major_temp = rbind(df_major_temp, data.frame("node1"=anc_nodes[filters[["planetoid"]]],
                                                  "node2"=these_nodes[filters[["planetoid"]]], "link"="merger_lots", stringsAsFactors=F))
  
  # Forward minor
  df_minor_temp = rbind(df_minor_temp, data.frame("node1"=these_nodes[filters[["captured"]]],
                                                  "node2"=host_nodes[filters[["captured"]]], "link"="captured", stringsAsFactors=F))
  df_minor_temp = rbind(df_minor_temp, data.frame("node1"=these_nodes[filters[["bud"]]],
                                                  "node2"=host_sec_nodes[filters[["bud"]]], "link"="bud", stringsAsFactors=F))
  # Backward minor
  df_minor_temp = rbind(df_minor_temp, data.frame("node1"=anc_nodes[filters[["meiosis"]]],
                                                  "node2"=these_nodes[filters[["meiosis"]]], "link"="split_recomb", stringsAsFactors=F))
  df_minor_temp = rbind(df_minor_temp, data.frame("node1"=anc_sec_nodes[filters[["meiosis"]]],
                                                  "node2"=these_nodes[filters[["meiosis"]]], "link"="split_recomb", stringsAsFactors=F))
  df_minor_temp = rbind(df_minor_temp, data.frame("node1"=anc_nodes[filters[["mitosis"]]],
                                                  "node2"=these_nodes[filters[["mitosis"]]], "link"="split_clean", stringsAsFactors=F))
  df_minor_temp = rbind(df_minor_temp, data.frame("node1"=anc_sec_nodes[filters[["charisma"]]],
                                                  "node2"=these_nodes[filters[["charisma"]]], "link"="minor_merger", stringsAsFactors=F))
  
  disintegrations = c(disintegrations, paste(data$cluster[filters[["disintegrate"]]], data$year[filters[["disintegrate"]]], sep="_"))
  denovos = c(denovos, paste(data$cluster[filters[["denovo"]]], data$year[filters[["denovo"]]], sep="_"))
  maj_links = rbind(maj_links, df_major_temp)
  min_links = rbind(min_links, df_minor_temp)
}

# Write
write.csv(maj_links, paste0(configs$finalized_path, "major_links.csv"), row.names=F)
write.csv(min_links, paste0(configs$finalized_path, "minor_links.csv"), row.names=F)
write.table(disintegrations, paste0(configs$finalized_path, "disintegrations.txt"), row.names=F, col.names=F, quote=F)
write.table(denovos, paste0(configs$finalized_path, "de_novos.txt"), row.names=F, col.names=F, quote=F)
write.csv(outcome_df, paste0(configs$finalized_path, "outcome_df.csv"), row.names=F)
write.csv(precedent_df, paste0(configs$finalized_path, "precedent_df.csv"), row.names=F)

