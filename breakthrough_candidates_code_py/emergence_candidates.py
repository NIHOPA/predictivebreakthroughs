import sqlite3
import os
import h5py
import pandas as pd
import numpy as np
import scipy.stats
import pathlib
import glob
import json

import cluster
import prediction

WORKING_DIR = pathlib.Path('/path/to/output/folder')
DATAPATH = pathlib.Path('/path/to/data')
REGRESSIONPATH = pathlib.Path('/path/to/Logistic_Regression_Output/')

pmidlist = None

def valid(df):
    return df['Status']=='Valid'

def process_candidates(df=None):

    p = REGRESSIONPATH

    if df is None: df = pd.concat(pd.read_csv(p / ('REGRESSION_DATE_'+str(f)+'-pred.csv')) for f in [1993,1994,1995,1996,1997,2013,2014,2015,2016,2017])
    df['Status'] = 'Valid'
    df = df[['Cluster','Status','Regression score']]
    df.loc[valid(df) & (df['Regression score']==0),'Status'] = 'Invalid: Regression'
    df = df.rename(columns={'Cluster':'cluster'})

    cluster.init_node_attr()
    df = df.merge(cluster.node_attr.loc[cluster.node_attr['cluster'].isin(df.loc[valid(df),'cluster'].values),['cluster','n','year','pctNew','NIH_linked','biggest_anc']],how='left')

    df.loc[valid(df) & (df['n']< 40), 'Status'] = 'Invalid: size'
    df.loc[valid(df) & (df['NIH_linked']<=0.05),'Status'] = 'Invalid: NIH funding'
    df.loc[valid(df) & (df['pctNew']<=0.15),'Status'] = 'Invalid: pct_new'

    xtra = generate_extra_fields(df[valid(df)])
    df = df.merge(xtra[['cluster', 'whrcr', 'avg_hrcr', 'frac_hrcr_over_1', 'pct_ICC']],how='left')

    df.loc[valid(df) & (df['avg_hrcr'] < 1.3),'Status'] = 'Invalid: low avg_hrcr (must be 1.3 or higher)'
    df.loc[valid(df) & (df['frac_hrcr_over_1'] < 0.3),'Status'] = 'Invalid: low hrcr growth (must be 0.3 or higher)'

    df.loc[valid(df),'Candidate'] = 'Valid'

    df.loc[valid(df) & (df['pct_ICC'] < 0.09),'Candidate'] = 'Not chosen: within-cluster citation ratio out of range'
    df.loc[valid(df) & (df['year']<2000) & (df['hrcr-T1'] < 150),'Candidate'] = 'Not chosen: hwRCR below breakthrough gold standards'
    df.loc[valid(df) & (df['year']>2000) & (df['hrcr-T1'] < 314),'Candidate'] = 'Not chosen: hwRCR below breakthrough gold standards'
    
    validclus = df.loc[df['Candidate']=='Valid','cluster'].values
    df.loc[df['Candidate']=='Valid','Echo'] = df.loc[df['Candidate']=='Valid','biggest_anc'].isin(validclus)

    df.loc[valid(df) & df['Echo'],'Status'] = 'Invalid: echo'
    df.loc[valid(df) & (df['year'].isin([2013,1993])), 'Status'] = 'Invalid: preceding year'
    df.loc[(df['Status'] == 'Invalid: echo') | (df['Status']=='Invalid: preceding year'),'Candidate'] = None

    #rename columns to better match language used in manuscript
    df=df.rename(columns={'cluster':'Cluster','n':'Cluster Size','whrcr':'whRCR','pct_ICC':'Within-cluster citations'})
    return df

def load_ccn(clus=None):
    cm = [c.split('_')[1]+'-'+c.split('_')[0] for c in clus]
    print(cm)
    ccn = []
    for f in glob.glob(DATAPTH / 'trajectory_pmid_info/*.csv'):
        if 'combined' in f: continue
        c = f.split('/')[-1].split('_whrcr')[0]
        if clus is None or c in cm:
            print(f)
            ccn.append(pd.read_csv(f))
    ccn = pd.concat(ccn)
    ccn['cluster'] = ccn.apply(lambda x: str(int(x['clusterId']))+'_'+str(int(x['clusterYear'])), axis=1)
    print ('Missing clusters:',' '.join([c for c in clus if c not in ccn['cluster'].values]))
    return ccn

def generate_extra_fields(cands):
    cluster.init_node_attr()
    load_citation_sheet()

    if 'Cluster' in cands.columns: cands = cands.rename(columns={'Cluster':'cluster'})
    print(cands)

    ccn = load_ccn(cands['cluster'].values)

    grp = ccn.groupby('cluster')['whrcr'].agg('sum').reset_index()
    cands = cands.merge(grp,how='left')

    hrcr_fields = []
    for clus in cands['cluster'].values:
        print(clus)
        c,y = cluster.split_cluster(clus)
        pmids = ccn.loc[ccn['cluster']==clus,'pmid'].astype(int).values
        print(pmids)
        incite,outcite = pmid_citation_lookup(pmids)
        print(len(pmids),incite,outcite)
        hrcr_df = pd.DataFrame(pmids,columns=['pmid'])
        hrcr_df['relative_citation_ratio'] = cluster.fetch_hrcrs(pmids,y)
        print(hrcr_df)
        hrcrs = hrcr_df['relative_citation_ratio'].values
        high_pmids = hrcr_df.loc[hrcr_df['relative_citation_ratio'] > 1,'pmid'].values
        hrcr_growth_pmids, hrcr_growth_values = prediction.fetch_hrcr_growth(high_pmids,y,2017)
        growth2 = sum(hrcr_growth_values >= 2) / len(hrcr_growth_values) if len(hrcr_growth_values)>0 else 0
        print(hrcr_growth_values, growth2)
        print(hrcrs)

        hrcr_fields.append([clus,hrcr_df['relative_citation_ratio'].mean(),[sum(hrcrs > 1)/len(hrcrs) if \
                     len(hrcrs)>0 else 0][0],incite,outcite])
    hrcr_fields = pd.DataFrame(hrcr_fields,columns=['cluster','avg_hrcr','frac_hrcr_over_1','intracluster_citations',\
                                     'intercluster_citations'])
    cands = cands.merge(hrcr_fields,how='left')

    cands['pct_ICC'] = cands.apply(lambda x: 0 if x['intracluster_citations']==0 else x['intracluster_citations'] / (x['intracluster_citations']+ \
                       x['intercluster_citations']), axis=1)
    cands = cands.drop(columns=['intracluster_citations','intercluster_citations'])

    return cands

def load_citation_sheet():
    global pmidlist
    if pmidlist is None:
        pmidlist = []
        with open(DATAPATH / 'cited_2019_snapshot-f39ea.json','r') as file:
            for line in file:
                pmidlist.append(json.loads(line).values())
        pmidlist = pd.DataFrame(pmidlist,columns=['pmid','citesPmid','pubYear','updated']).set_index('pmid')
        

def pmid_citation_lookup(pmids):
    global pmidlist
    pm = pmidlist.loc[pmids].copy()
    pm = pm.explode('citesPmid')
    pm = pm[~pm['citesPmid'].isna()]
    incite = sum(pm['citesPmid'].isin(pmids))
    outcite = len(pm) - incite
    return incite, outcite


if __name__ == '__main__':
    cluster.init_node_attr()
    load_citation_sheet()
    df = process_candidates()
    df.to_csv(WORKING_DIR / 'breakthrough_candidates.csv',index=False)

