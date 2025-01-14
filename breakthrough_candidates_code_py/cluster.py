import sqlite3
import os
import h5py
import pandas as pd
import numpy as np
import collections
import pickle
import json
import more_itertools

WORKING_DIR = '/path/to/data/folder'
TRAJECTORY_DIR = '/path/to/trajectories'


clus_pmids = None #a dict of clusters, with the pmids they're composed of
node_attr = None #per-cluster attribute file
pmid_hrcr = None #historical rcrs per pmid and year
pmid_clus = None #a dict of pmids, with the clusters they appear in


############################ Cluster-centric methods ##########################

def init_cluster_dict(min_size=40):
    global clus_pmids
    if clus_pmids is None:     
        print('Loading cluster pmid data...', end="", flush=True)
        pmid_db = sqlite3.connect(os.path.join(WORKING_DIR,'RMCL_1.20_all_years_lookup.sqlite'))
        pmid_cur = pmid_db.cursor()

        everything = pmid_cur.execute("SELECT * FROM 'RMCL_1.20_all_years_lookup'").fetchall()
           
        clus_pmids = {}    
        for e in everything:
            e_1 = e[1].split(';')
            if len(e_1) >= min_size:
                clus_pmids[e[0]] = [int(f) for f in e_1]
        print(' done.')
    return True

def init_pmid_dict(year_range=[None,None]):
    global pmid_clus
    try:        
        with open(os.path.join(WORKING_DIR,'pmid_dict.json'),'r') as f:
            pmid_clus = json.load(f)
    except:
        global clus_pmids
        init_cluster_dict()    
        if pmid_clus is None:     
            print('Loading pmid cluster data...', end="", flush=True)
            pmid_clus = {}    
            for clus in clus_pmids:
                y = int(clus.split('_')[1])
                if year_range[0] is not None and y < year_range[0]: continue
                if year_range[1] is not None and y > year_range[1]: continue
                for p in clus_pmids[clus]:
                    try:
                        pmid_clus[p].append(clus)
                    except:
                        pmid_clus[p] = [clus]            
            print(' done.')
        with open(os.path.join(WORKING_DIR,'pmid_dict.json'),'w') as f:
            json.dump(pmid_clus, f)
    return True

def init_node_attr():    
    global node_attr
    if node_attr is None: 
        print('Loading cluster attribute data...', end="", flush=True)
        node_attr = pd.read_csv(os.path.join(WORKING_DIR,'cluster_node_attr.csv'))
        print(' done.')
    return True

def init_hrcr():
    global pmid_hrcr
    if pmid_hrcr is None: 
        with open(WORKING_DIR / 'hrcrs.pickle','r') as file:
            pmid_hrcr = pickle.load(file)
        return True
    return True

def get_pmid_hrcr(pmid,year=None):
    if year is not None and year < 2020:
        init_hrcr()
        try:
            return pmid_hrcr[year].loc[pmid]['relative_citation_ratio']
        except Exception as e:
            print('Exception!',e)
            return None
    else:
        return 0


def split_cluster(cluster):
    if '_' not in cluster:
        print('Invalid cluster id:',cluster)
        return None, None
    clus,year = cluster.split('_')
    if clus == 'NA': return None, None
    clus = int(clus)
    year = int(year.strip('*'))
    return clus, year


def fetch_cluster(cluster):
    try:
        return fetch_cluster_local(cluster)
    except:
        print('Fetch local cluster failed for cluster ',cluster)
        return None, None  


def fetch_cluster_local(cluster):
    init_node_attr()
    clus,year = split_cluster(cluster)
    traj = pd.read_csv(os.path.join(TRAJECTORY_DIR,'trajectory',str(year),cluster+'.csv'))
    nodes = traj['node1'].values.tolist()
    nodes.extend(traj['node2'].values.tolist())
    nodes = set(nodes)
    attr = node_attr[node_attr['cluster'].isin(nodes)].copy()
    attr = attr.set_index('cluster')
    
    return traj,attr


def get_clusters_containing_pmids(pmids=[], metric='count', year_range=(None, None)):
    '''Returns the clusters with nonzero pmids, and the fraction of the cluster composed of the pmids'''
    
    if clus_pmids is None: init_cluster_dict()
    a = set(pmids)

    out_clusters = {}
    for clus in clus_pmids:
        c,year = split_cluster(clus)
        if year_range[0] is not None and year < year_range[0]: continue
        if year_range[1] is not None and year > year_range[1]: continue            
        b = set(clus_pmids[clus])
        inter = a.intersection(b)
        if len(inter) > 0:
            if metric == 'count':
                out_clusters[clus] = len(inter)
            elif metric == 'ratio':
                out_clusters[clus] = len(inter)/len(b)
            else: #assume binary
                out_clusters[clus] = 1
    return out_clusters
    

def get_pmids_in_clusters(list_clus=[]):
    if not isinstance(list_clus,list) and not isinstance(list_clus,np.ndarray):         
        list_clus = [list_clus]
    if clus_pmids is None:         
        init_cluster_dict()
    pmids = []
    for clus in list_clus:
        if clus in clus_pmids:
            pmids.extend(clus_pmids[clus])
    return list(set(pmids))


def size_matched_negatives(clus_list,limit=5):
    negs=[]
    for cluster in clus_list:
        clus,yr = split_cluster(cluster)
        if clus is None: continue
        offsets = list(range(-1*limit,0))
        offsets.extend(list(range(1,limit+1)))
        
        for offset in offsets:
            negs.append(str(int(clus+offset))+'_'+str(yr))    

    return negs


def fetch_hrcrs(pmids, year=2017):
    if year < 1981:
        return np.array([get_pmid_hrcr(i,year) for i in pmids])
    init_hrcr()
    inter = pmid_hrcr[year].index.intersection(set(pmids))
    outdf = pd.DataFrame(index=pmids,columns=['rcr'])
    
    outdf['rcr'][inter] = pmid_hrcr[year].loc[inter]['relative_citation_ratio'].values
    return outdf.values.squeeze()
    
