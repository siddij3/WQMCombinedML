import boto3
import shutil
import secret

from sqlalchemy import create_engine
import functions

def s3_bucket():
    return boto3.resource(
        service_name='s3',
        region_name='us-east-2',

    )


def save_to_bucket(s3, filename):
    s3.meta.client.upload_file(filename, get_bucket_name(), filename)
    #print("Uploaded to AWS")

def load_from_bucket(s3, path, folder_key):

    for bucket in s3.buckets.all():
        if "wqm" in bucket.name:
            for obj in bucket.objects.all():
                #print(obj.key)

                with open(obj.key, 'wb') as data:
                    if folder_key in obj.key:

                        # TODO https://chaquo.com/chaquopy/doc/current/android.html#android-studio-plugin
                        s3.meta.client.download_fileobj(bucket.name, obj.key, data)
                        shutil.unpack_archive(obj.key, path)

def get_bucket_name():
    return 'wqm-ml-models'


def get_creds():
    server = secret.server
    database = secret.databse
    username = secret.username
    password = secret.password
    port = 3306

    con = f'mysql+pymysql://{username}:{password}@{server}:{port}/{database}'
    return con

def connect():

    rds = boto3.client('rds', region_name=secret.region)

    db_instance = rds.describe_db_instances(DBInstanceIdentifier=secret.instance)['DBInstances'][0]
    endpoint = db_instance['Endpoint']['Address']

    engine = create_engine(
        get_creds(),
        pool_recycle=3600)

    return engine


def get_table_name():
    return functions.get_table_name()




