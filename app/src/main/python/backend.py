
# ## Importing Data

#
# -*- coding: utf-8 -*-
# Regression Example With Boston Dataset: Standardized and Wider
import os
import functions

from sklearn.preprocessing import StandardScaler
from keras.models import load_model

import pandas as pd
from pandas import DataFrame
import aws
import file_management
import sql_manager

from sql_manager import sql_to_pandas

from sql_manager import pandas_to_sql_if_exists

import re
import tensorflow as tf
import numpy as np


def get_models():
    global optimal_NNs, sensor_data

    ## DATA IMPORTING AND HANDLING
    engine = aws.connect()

    # The dataset should be the 50 s array of time from the app
    # TODO GET SENSOR DATA
    sensor_data = "get from app"
    table_name = sql_manager.get_table_name()
    std_params = sql_to_pandas(sql_manager.get_params_table_name(), engine)

    # GET FILE FROM S3 BUCKET, UNZIP IT,
    path = file_management.get_file_path() #os.path.join(os.environ['HOME'], f'{folder_name}')
    s3 = aws.s3_bucket()

    # TODO train gold_fc_prod models without Rinse, and one without Temp/pH
    aws.load_from_bucket(s3, path, table_name)

    # Downloading files and such
    k_folds = functions.get_num_folds()
    local_download_path = os.path.join(os.environ['HOME'], f'{path}')

    optimal_NNs = [None]*k_folds

    i = 0
    tmp = ""
    for filename in os.listdir(local_download_path):

        if "Model" in filename:
            #optimal_NNs[i] = load_model(f"{path}\\{filename}")
            # TODO there could be some bugs here
            optimal_NNs[i] = load_model(os.path.join(os.environ['HOME'], f'{path}\\{filename}'))


            #print(optimal_NNs[i].optimizer)
            i+=1



def calculate_ppm():

    k_folds = functions.get_num_folds()
    tmp_ppm = [None]*k_folds
    for fold in range(k_folds):
        tmp_ppm[fold] = functions.predict_ppm(optimal_NNs[fold], sensor_data)

    return sum(tmp_ppm)/k_folds





