import itertools
import pickle

import sklearn.preprocessing
import sklearn.impute
import numpy as np
import pandas as pd

from sklearn.linear_model import LinearRegression, LogisticRegression

import prediction
import cluster
import emergence_features as ef

TODAY_DATE = 'REGRESSION_DATE'
SAVE_FOLDER = '/path/to/regression/output'

scaler = None
imp = None
def prep_data(feat_mask = None):

    print('Prepping training data...')
    #golds
    gold_feats = [prediction.regression_features(n, feat_mask) for n in prediction.gold_dict.values()]

    #randos
    neg_trajs = cluster.size_matched_negatives(prediction.gold_dict.values())
    #print(neg_trajs)

    neg_feats = [prediction.regression_features(n, feat_mask) for n in neg_trajs]

    print('...Features extracted...')

    #remove invalid trajectories
    gold_feats = [x for x in gold_feats if x is not None]
    neg_feats = [x for x in neg_feats if x is not None]

    all_y = np.zeros(len(gold_feats)+len(neg_feats))
    all_y[:len(gold_feats)] = 1    
    all_feats = gold_feats+neg_feats #list of dataframes, one per traj
    all_feats = [f.values.flatten() for f in all_feats]
    all_x = np.array(all_feats)
    
    print(all_x.shape)
    
    #use an imputer to substitute NaN values
    global imp
    imp = sklearn.impute.SimpleImputer()
    all_x = imp.fit_transform(all_x)

    global scaler
    scaler = sklearn.preprocessing.MinMaxScaler()
    all_x = scaler.fit_transform(all_x)
    
    print('Training data ready!')
    return all_x, all_y


def prep_validation_data(feat_mask = None):
    val_feats=[prediction.regression_features(n, feat_mask) for n in prediction.gold_dict.values()]
    val_x = np.array([f.values.flatten() for f in val_feats])
    val_x = imp.transform(val_x)
    val_x = scaler.transform(val_x)
    return val_x


def train_regressor(all_x, all_y, val_x = None, split=0.75,verbose=False,state=None):
    if verbose: print('Training model...')
    ind = np.array(range(len(all_y)))
    np.random.shuffle(ind)
    train_ind = ind[:int(ind.shape[0]*split)]
    test_ind = ind[int(ind.shape[0]*split):]
    test_x = all_x[test_ind]
    test_y = all_y[test_ind]
    train_x = all_x[train_ind]
    train_y = all_y[train_ind]

    if state is None: state = 0
    #reg = LogisticRegression(class_weight='balanced',random_state=state).fit(train_x, train_y)
    #reg = LogisticRegression(class_weight={0:1.0,1:1.20},random_state=state).fit(train_x, train_y)
    reg = LogisticRegression(class_weight={0:1.0,1:0.7},random_state=state).fit(train_x, train_y)

    reg.test_score = reg.score(test_x, test_y)
    if verbose:    
        print(reg.score(train_x, train_y))
        print(reg.predict(test_x), test_y)
    
        print('score:',reg.test_score)
        print(reg.get_params())
        coef = reg.coef_
   
        print(coef[:9])
        print(coef[9:18])
        print(coef[18:])

        print(reg.intercept_)

    if val_x is not None:
        reg.val_score = reg.score(val_x, np.ones(len(val_x)))
        if verbose:
            print('Validation score:',reg.val_score)

    with open(SAVE_FOLDER+TODAY_DATE+'_regression.model', 'wb') as file:
        pickle.dump(reg, file)
    print('Model trained!')
    return reg
   

def analyze_year(model, year,min_size=0,cutoff=None):
    c,f = prediction.get_year(year, min_size=min_size, cutoff=cutoff)

    mask = np.array([True for x in f])
    print (mask.shape)
    fn = f[mask]
    cn = [c[en] for en,n in enumerate(mask) if n]
    
    df = pd.DataFrame(cn)
    df = pd.concat([df,pd.DataFrame(fn)],axis=1,sort=False)

    r = model.predict(scaler.transform(imp.transform(fn)))
    
    ind = np.argsort(r)[::-1]

    for i in ind[0:100]:
        print(cn[i],r[i])

    df = pd.DataFrame([np.array(cn),r]).T
    df.columns = ['Cluster','Regression score']
    df = df.sort_values(by=['Cluster'])
    df.to_csv(SAVE_FOLDER+'/'+TODAY_DATE+'_'+str(year)+'-pred.csv',index=False)


def analyze_clusters(model, clus_list):
    #remove invalid clusters
    clusters = [c for c in clus_list if prediction.cluster_valid(c)]    
    feats = [prediction.regression_features(n).values.flatten() for n in clusters]
    feats = np.array(feats)

    r = model.predict(scaler.transform(imp.transform(feats)))

    df = pd.DataFrame([np.array(clusters),r]).T
    df.columns = ['Cluster','Regression score']
    df = df.sort_values(by=['Regression score'])[::-1]
    df.to_csv(SAVE_FOLDER+'/'+TODAY_DATE+'_selection-pred.csv',index=False)

if __name__ == '__main__':
    x,y = prep_data()    
    val = prep_validation_data()
    m = train_regressor(x, y, val_x = val, split = 0.99, verbose = True)

    for i in [1993,1994,1995,1996,1997,2013,2014,2015,2016,2017]:
        analyze_year(m,i,min_size = 40)
