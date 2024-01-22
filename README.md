# Water Quality Monitor App

Installation:
1. Since we are installing the APK manually, we must allow App installations from unknown sources on the target device:

Go to Menu > Settings > Security > and check Unknown Sources to allow your phone to install apps from sources other than the Google Play Store

2. Download APK File onto device from repository (WaterQualityMonitorCombined/app-release-unsigned.apk)

# Platforms

The app uses Chaquopy, an API to allow a back-end Python interpreter, allowing the use of Python-based Machine Learning libraries such as Tensorflow and Keras. 

![image](https://github.com/siddij3/WQMCombinedML/assets/16713437/ceab2473-7654-4772-ba47-5f90980f142c)


### Why Chaquopy?

Chapquopy was used to interface Python APIs with the app such as the API for a cloud-bucket service, cloudinary, and MySQL interface on the Google Cloud Platform (GCP). The digram shows how the app connects with the 2 cloud-based services to enable functioanlity with downloading machine learning models from the cloud, and storing the sensor data from the user's phone into a GCP databse. 

![image](https://github.com/siddij3/WQMCombinedML/assets/16713437/1978cd74-2a0f-49cb-9897-6ae9f9db5a9a)

## How are these inter-connected in the app?

Once the app starts, it connects to a storage bucket on Cloudinary to download neural network models.
If the sensor is connecting and while it's sensing the compounds, after 26 s the parameters will be organized into their relevant features at a sampling rate of 0.1 Hz, and calculating the integral of the signals.

After the 26 s, the model calculates the analyte concentration and uploads the new data into the GCP server.

![image](https://github.com/siddij3/WQMCombinedML/assets/16713437/64cd18d2-6b76-4ee4-9209-c33ea6715eca)
