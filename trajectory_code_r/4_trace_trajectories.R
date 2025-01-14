library(data.table)
library(igraph)
library(snow)
library(fastmatch)
source("./utils.R")

configs = read_configs()

maj_links = fread(paste0(configs$finalized_path, "major_links.csv"), data.table=F)
min_links = fread(paste0(configs$finalized_path, "minor_links.csv"), data.table=F)
node_attr = fread(paste0(configs$finalized_path, "cluster_node_attr.csv"), data.table=F)

maj_links_orig = maj_links
min_links_orig = min_links
# Exclude 1980
maj_links = maj_links[!(grepl("_1980", maj_links$node1, fixed=T) | grepl("_1980", maj_links$node2, fixed=T)),]
min_links = min_links[!(grepl("_1980", min_links$node1, fixed=T) | grepl("_1980", min_links$node2, fixed=T)),]
all_links = rbind(maj_links, min_links)

trajectories = function(year) {
  library(igraph)
  library(fastmatch)
  all_links_concat = paste(all_links$node1, all_links$node2, sep="|")
  summary(fmatch(all_links_concat, all_links_concat))
  print(year)
  for (seed_node in sort(grep(paste0("_", year), all_links$node1, fixed=T, value=T))) {
    linked_nodes = seed_node
    dir.create(paste0(configs$trajectories_out_path, year))
    if (file.exists(paste0(configs$trajectories_out_path, year, "/", seed_node, ".csv"))) next
    maj_linked = maj_links[maj_links$node1 %fin% linked_nodes | maj_links$node2 %fin% linked_nodes,]
    # Add minor links for the first step
    maj_linked = rbind(maj_linked, min_links[min_links$node1 %fin% linked_nodes | min_links$node2 %fin% linked_nodes,])
    new_node_cands = unique(c(maj_linked$node1, maj_linked$node2))
    new_nodes = new_node_cands[!(new_node_cands %fin% linked_nodes)]
    linked_nodes = unique(c(linked_nodes, new_nodes))
    mark_group = linked_nodes
    # Follow major links plus forward captures as the backbone of the trajectory
    while(length(new_nodes) > 0) {
      maj_linked = maj_links[maj_links$node1 %fin% linked_nodes | maj_links$node2 %fin% linked_nodes,]
      # Add forward captures
      maj_linked = rbind(maj_linked, min_links[min_links$node1 %fin% linked_nodes & min_links$link == "captured",])
      new_node_cands = unique(c(maj_linked$node1, maj_linked$node2))
      new_nodes = new_node_cands[!(new_node_cands %fin% linked_nodes)]
      linked_nodes = unique(c(linked_nodes, new_nodes))
    }
    
    # Decorate with minor links ("grass") at the end
    maj_linked = all_links[all_links$node1 %fin% linked_nodes | all_links$node2 %fin% linked_nodes,]
    
    # Start at seed node and create links going back as far as possible following the biggest anc pathway
    if (!grepl("_1980", seed_node, fixed=T)) {
      cluster_chain = seed_node
      backbone_df = NULL
      while (all(!grepl("_1980", cluster_chain, fixed=T))) {
        start_cluster = cluster_chain[length(cluster_chain)]
        end_cluster = node_attr$biggest_anc[node_attr$cluster == start_cluster]
        if (is.null(end_cluster) || is.na(end_cluster) || length(end_cluster) == 0 || end_cluster == "" || nchar(end_cluster) == 0) break
        if (grepl("_1980", end_cluster, fixed=T)) break
        cluster_chain = c(cluster_chain, end_cluster)
        backbone_df = rbind(backbone_df, data.frame("node1"=end_cluster, "node2"=start_cluster, "link"=NA))
      }
      
      backbone_df$link = all_links$link[fmatch(paste(backbone_df$node1, backbone_df$node2, sep="|"), all_links_concat)]
      maj_linked = rbind(maj_linked, backbone_df)
      maj_linked = maj_linked[!duplicated(maj_linked),]
    }
    maj_linked$link_is_major = maj_linked$link %in% c("fwd_one_to_one", "split", "shed", "back_one_to_one", "merger_two", "merger_lots")
    write.csv(maj_linked, paste0(configs$trajectories_out_path, year, "/", seed_node, ".csv"), row.names=F, na="", quote=F)
    
    # write node attributes too
    linked_nodes = unique(c(maj_linked$node1, maj_linked$node2))
    summary(linked_nodes %fin% linked_nodes)
    write.csv(node_attr[node_attr$cluster %fin% linked_nodes,], paste0(configs$trajectories_out_path, year, "/", seed_node, "_attr.csv"), 
              row.names=F, na="",
              quote = which(colnames(node_attr) %in% c("journalName_most_common", "wikipedia", "word2vec")))
    
    # Grab node attributes and make file
    
    # Make PNG if desired
    if (as.logical(configs$make_png)) {
      em=as.matrix(maj_linked[,1:2])
      g = graph_from_edgelist(em)
      png(paste0(configs$trajectories_out_path, year, "/", seed_node, ".png"), width=1024, height=1024)
      plot.igraph(g, vertex.size=2, edge.curved=T, edge.arrow.size=.25, vertex.label=NA, mark.groups=list("End"=mark_group))
      dev.off()
    }
  }
}

# Run 10 years at a time. Takes a while
#cl = makeCluster(20)
#clusterExport(cl, c("maj_links", "min_links", "all_links", "configs", "node_attr"))
#temp = parLapply(cl, 1980:1989, trajectories)
#stopCluster(cl)

#cl = makeCluster(20)
#clusterExport(cl, c("maj_links", "min_links", "all_links", "configs", "node_attr"))
#temp = parLapply(cl, 1990:1999, trajectories)
#stopCluster(cl)

cl = makeCluster(24)
clusterExport(cl, c("maj_links", "min_links", "all_links", "configs", "node_attr"))
temp = parLapply(cl, 2000:2019, trajectories)
stopCluster(cl)

