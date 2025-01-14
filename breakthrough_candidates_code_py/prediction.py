import os

import pandas as pd
import numpy as np
import h5py
import sqlite3
import json
import tarfile
import scipy.spatial
import scipy.linalg
import scipy.stats
import sklearn.preprocessing
import sklearn.decomposition
import sklearn.feature_selection
import glob
import pickle
import time

import matplotlib.pyplot as plt
import seaborn as sns

import cluster


#gold standard
goldclus = ['4032_1987','2611_1994','3895_1987','18049_2008','4999_1985','2937_1990','9621_1995','1316_1981','457_1991','3002_1985','4430_1993','2095_2004','4316_1996','8018_2010','13712_2007','10374_2004','5237_1996','1538_1981','493_1988','1134_1984','1271_1990']
goldnames = ['Apoptosis Bcl2-Bax','Breast cancer genetics','Carbon nanotubes','CRISPR','Cystic fibrosis drugs','Cancer drugs - EGFR inhibitors','Super-resolution microscopy & GFP','Helicobacter','HepC drugs','Hypertension - bosentan','Immunotherapy','iPSCs (Medicine 2012)','Leptin/Orexin','mHealth E-mental health','Microbiome','miRNA/exosomes','Neurogenesis in Hippocampus','Olfactory Biology','TLR & immunity','Vesicle traffic','RNA interference']
gold_dict = {goldnames[i]: goldclus[i] for i in range(len(goldnames))}

ANALYSIS_COLS = ['pct_of_biggest_anc_new',
 'pctNew',
 'hrcr_top']

ANALYSIS_LENGTH = 5 #years to look backwards into the past

    
def fetch_hrcr_growth(pmids,year1,year2):
    ''' returns the hrcr growth of the given pmids between the two years '''
    cluster.init_hrcr()
    hrcr = cluster.pmid_hrcr[year1]

    pmid_df = hrcr.loc[hrcr.index.intersection(pmids)][['relative_citation_ratio','year']].copy()
    hrcr_n = cluster.pmid_hrcr[year2]
    pmid_df_2 = hrcr_n.loc[hrcr_n.index.intersection(pmid_df.index)][['relative_citation_ratio','year']].copy()
    first = pmid_df.loc[pmid_df.index.intersection(pmid_df_2.index)]['relative_citation_ratio'].values
    second = pmid_df_2.loc[pmid_df.index.intersection(pmid_df_2.index)]['relative_citation_ratio'].values

    ratio = np.divide(second,first)
    return pmid_df.index.intersection(pmid_df_2.index), ratio

def fetch_lookup_features_cluster(clus, feat_mask = None, attr=None):
    '''returns features of specified cluster'''
    if attr is None: 
        edges,attr = cluster.fetch_cluster(clus)
        if edges is None: return #skip clusters we can't load        
    
    if clus not in attr.index:
        #log?
        return None

    #lookup features
    yr = cluster.split_cluster(clus)[1]
    ft = attr.loc[clus][ANALYSIS_COLS].squeeze()   

    if feat_mask is not None:
        ft=ft[feat_mask]

    #ancestral entropy
    new = attr.loc[clus]['pctNew']
    anc = attr.loc[clus]['pct_in_biggest_anc']
    sec = attr.loc[clus]['pct_in_secbiggest_anc']
    if np.isnan(new): new = 0
    if np.isnan(anc): anc = 0
    if np.isnan(sec): sec = 0

    sa = 1 - new - anc - sec
    if sa < 0: sa = 0
    if sa > 1: sa = 1

    ft['ancestral_entropy'] = sa
    return pd.DataFrame(ft).T
    

def fetch_lookup_features_cluster_list(clus_list=None, feat_mask=None, attr=None):
    if clus_list is None: return None
    if clus_list == []: return None
    if '_' in clus_list: clus_list = [clus_list]

    features = [] #a list of dataframes containing the features for each cluster
    for clus in clus_list:
        feat = fetch_lookup_features_cluster(clus, feat_mask, attr=attr)
        if feat is not None:
            features.append(feat)

    return pd.concat(features)


def regression_features(traj, feat_mask=None):
    print(traj)
    t = time.time()
    feat=[]
    edges,attr = cluster.fetch_cluster(traj)
    yr = cluster.split_cluster(traj)[1]
    if edges is None: return None #skip trajectories we can't load    

    #latest year features
    feat = fetch_lookup_features_cluster(traj, feat_mask, attr=attr)
    if feat is None: return None

    #older clusters
    clusters = []
    for y in range(yr-ANALYSIS_LENGTH,yr):
        clus = attr[attr['year']==y].index.values
        clus = [c for c in clus if edges[edges['node2'] == c]['link_is_major'].any()]
        clusters.extend(clus)
    #older cluster features
    hist_feat = fetch_lookup_features_cluster_list(clusters, feat_mask, attr=attr)       
    
    if hist_feat is None:
        hist_mean = pd.Series([np.nan for j in range(len(feat.values.tolist()[0]))])
        hist_std = pd.Series([np.nan for j in range(len(feat.values.tolist()[0]))])
    else:
        hist_mean = hist_feat.mean(axis=0)
        hist_std = hist_feat.std(axis=0)

    #reshape to 1D
    feats = feat.values.tolist()[0] + hist_mean.values.tolist() + hist_std.values.tolist()
    cols = feat.columns.tolist() + ['mean_'+c for c in feat.columns.tolist()] + ['std_'+c for c in feat.columns.tolist()]
    
    print('Features for trajectory',traj,time.time()-t,'seconds')
    
    out = pd.DataFrame(feats).T
    out.columns = cols

    print('Fetch for trajectory',traj,time.time()-t,'seconds')
    return out


def get_year(year=2017,min_size=0, max_size=1000000,cutoff=None):
    print('Loading '+str(year)+' clusters')    

    #filter big node table to get valid clusters
    clusters = cluster.node_attr[(cluster.node_attr['year'] == year) & (cluster.node_attr['n'] >= min_size) & (cluster.node_attr['n'] <= max_size)]['cluster'].values.tolist()

    if cutoff is not None:
        clusters = [c for c in clusters if cluster.split_cluster(c)[0] < cutoff]
        print(len(clusters),'remaining after cutoff')

    print('Cluster count:',len(clusters))
    feats=[]
    valid=[]
    for en,n in enumerate(clusters):
        if len(clusters) > 1000 and en%1000==1:
            print('Processing cluster',en, n)
        feat = regression_features(n)
        if feat is not None:
            feats.append(feat.values.flatten())
            valid.append(True)
        else:
            valid.append(False)

    clusters= [x for ex,x in enumerate(clusters) if valid[ex] ]
    return clusters, np.array(feats)


def cluster_valid(clus):
    '''returns true if the cluster can be used for classification, i.e. n>40 and nih support > 0.05'''
    t,a = cluster.fetch_cluster(clus)
    
    
    if a.loc[clus]['n'] < 40: return False
    if a.loc[clus]['NIH_linked'] < 0.05: return False    
    
    return True

