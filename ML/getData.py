import pyrebase
import pandas as pd
import numpy as np

configfb = {"apiKey": "AIzaSyBkvh_XeJKp1v5XiRSiQEOsfiG5tfV7d9Y",
"authDomain": "total-cascade-210406.firebaseapp.com",
"databaseURL": "https://total-cascade-210406.firebaseio.com",  
"storageBucket": "total-cascade-210406.appspot.com"
}
timestep_size = 450

train_p = 0.8

def connect_firebase():
# add a way to encrypt those, I'm a starter myself and don't know how
    username = "chester11206@gmail.com"
    password = "abcd1234"

    firebase = pyrebase.initialize_app(configfb)
    auth = firebase.auth() 

    user = auth.sign_in_with_email_and_password(username, password)

    #user['idToken']
    # At pyrebase's git the author said the token expires every 1 hour, so it's needed to refresh it
    user = auth.refresh(user['refreshToken'])

    #set database
    db = firebase.database()
    valuesX = db.child('SensorDataSet').get()
    print (valuesX.val())
    dataX = pd.DataFrame(valuesX.val())
    npdataX = np.array(dataX.values).T

    valuesY = db.child('GroundTruth').get()
    dataY = pd.DataFrame(valuesY.val())
    npdataY = np.array(dataY.values).T

    new_dataX = []
    for i in range(0, npdataX.shape[0] , timestep_size):
        if i+timestep_size < npdataX.shape[0]-1 :
            new_dataX.append(npdataX[i:i+timestep_size, :].flatten())
    new_dataX = np.array(new_dataX)

    dataNum = new_dataX.shape[0]
    new_dataY = npdataY[0:dataNum,:]

    trainNum = int(dataNum*train_p)
    trainX = new_dataX[0:trainNum,:]
    trainY = new_dataY[0:trainNum,:]
    testX = new_dataX[trainNum:dataNum,:]
    testY = new_dataY[trainNum:dataNum,:]

    print (trainX.shape)
    print (trainY.shape)
    print (testX.shape)
    print (testY.shape)

    return trainX, trainY, testX, testY

trainX, trainY, testX, testY = connect_firebase()

#and now the hard/ easy part that took me a while to figure out:
# notice the value inside the .child, it should be the parent name with all the cats keys

# adding all to a dataframe you'll need to use the .val()  