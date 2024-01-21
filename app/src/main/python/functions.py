from sklearn.metrics import mean_absolute_error
from sklearn.metrics import mean_squared_error
import math

from pandas import DataFrame

def get_dict(tt_feats):
    dict = {
        'Time':tt_feats[:, 0],
        'Current':tt_feats[:, 1],
        'pH Elapsed':tt_feats[:, 2] ,
        'Temperature':tt_feats[:, 3],
        'Rinse':tt_feats[:, 4],
        'Integrals':tt_feats[:, 5]
    }
    return DataFrame(dict)

def get_model_folder_name():
    return "gold_fc_prod"

def get_table_name():
    return 'gold_fc'

def get_std_params_table_name():
    return 'std_params_gold'

def get_data_folder_name():
    return "Data"

def get_num_folds():
    return 4

def predict_ppm(model, features):
    return model.predict(features)


