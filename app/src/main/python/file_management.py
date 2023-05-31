import functions
import os

def get_file_path():
    folder_name = functions.get_model_folder_name()
    return os.path.join(os.environ['HOME'], f'{folder_name}')

def get_data_path():
    folder_name = functions.get_data_folder_name()
    return os.path.join(os.environ['HOME'], f'{folder_name}')

def importData(data, scaler):

    train_dataset = data.sample(frac=0.8, random_state=5096)
    test_dataset = data.drop(train_dataset.index)

    train_features = train_dataset.copy()
    test_features = test_dataset.copy()

    train_labels = train_features.pop('Concentration')
    test_labels = test_features.pop('Concentration')

    try:
        train_features = train_features.drop(['index'], axis = 1)
        test_features = test_features.drop(['index'], axis = 1)
    except:
         pass
        #print("'index' parameter does not exist");


    train_features = functions.get_dict(scaler.fit_transform(train_features.to_numpy()))
    test_features = functions.get_dict(scaler.transform(test_features.to_numpy()))

    #For later use
    data_labels = data.pop('Concentration')

    return data, data_labels, train_dataset, test_dataset, train_features, test_features, train_labels, test_labels, scaler
