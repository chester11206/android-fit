# -*- coding:utf-8 -*-
import tensorflow as tf
from tensorflow.contrib import rnn
from tensorflow.examples.tutorials.mnist import input_data
import pandas as pd
import numpy as np
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
import os

cred = credentials.Certificate('firebase-adminsdk.json')
firebase_admin.initialize_app(cred, {
    'databaseURL' : 'https://total-cascade-210406.firebaseio.com'
})

lr = 1e-3

batch_size = tf.placeholder(tf.int32, [], name='batch_size')  # tf.int32
# Feature Num
input_size = 28
# Time Series Num
timestep_size = 28
# Hidden Layer Feature Num 
hidden_size = 256
# LSTM Layer Num
layer_num = 2
# Y Class Num
class_num = 10

_batch_size = 128

epoch_num = 2000

# Training Data / Raw Data
train_p = 0.8

def connect_firebase():
    root = db.reference()
    values = root.child('SensorDataSet').get()
    data = pd.DataFrame(values).T

    data_index = [[i.split()[0], int(i.split()[1])] for i in list(data)[class_num:]]
    sorted_data_index = sorted(data_index, key=itemgetter(1))
    sorted_data_index = [i[0] + " " + str(i[1]) for i in sorted_data_index]
    sorted_data_index = list(data)[:class_num] + sorted_data_index
    
    data = data.reindex(columns=sorted_data_index)
    print (data)

    npdata = np.array(data.values)
    print (npdata.shape)
    print (npdata[:,0:10])

    return npdata

def write_train(raw_data):
    folder_path = "data"
    if not os.path.exists(folder_path):
        os.mkdir(folder_path)
    file_path = os.path.join(folder_path, sys.argv[1])
    train_file = open(file_path,"w") #submit_final.csv
    train_w = csv.writer(train_file)
    train_w.writerow(["Biking", "In Vehicle", "Running", "Still", "Tilting", "Walking", "Features"])
    for item in raw_data:
        train_w.writerow(item)
    train_file.close()


def canonical_name(x):
  return x.name.split(":")[0]

# Read raw data
raw_data = connect_firebase()
write_train(raw_data)

# Get train, test data
dataNum = raw_data.shape[0]
trainNum = int(dataNum*train_p)
trainX = raw_data[:trainNum, class_num:]
trainY = raw_data[:trainNum, :class_num].astype(int)
testX = raw_data[trainNum:, class_num:]
testY = raw_data[trainNum:,:class_num].astype(int)

print (trainX.shape)
print (trainY.shape)
print (testX.shape)
print (testY.shape)
print (trainX)
print (trainY)
print (testX)
print (testY)

mnist = input_data.read_data_sets('MNIST_data', one_hot=True)
print (mnist.train.images.shape)

# set training x, y placeholder
X_train = tf.placeholder(tf.float32, [_batch_size, timestep_size*input_size], name="imput_train_x")
Y_train = tf.placeholder(tf.float32, [_batch_size, class_num], name="imput_train_y")

# set testing x, y placeholder
X_test = tf.placeholder(tf.float32,[None, input_size*timestep_size], name='input_test_x')
Y_test = tf.placeholder(tf.float32,[None, class_num], name='input_test_y')
keep_prob = tf.placeholder(tf.float32, [])
print (X_train.shape)

# Training Data: [_batch_size, timestep_size*input_size] ==> [_batch_size, timestep_size, input_size]
# Testing Data: [None, timestep_size*input_size] ==> [None, timestep_size, input_size]
# Build RNN LSTM layer
####################################################################

# **Step 1: Input Shape = (batch_size, timestep_size, input_size)
X = tf.placeholder(tf.float32, [None, timestep_size, input_size])

# **Step 2: Run MultiRNN with ((lstm + dropout) * 2)
mlstm_cell = []
for i in range(layer_num):
	lstm_cell = rnn.BasicLSTMCell(num_units=hidden_size, forget_bias=1.0, state_is_tuple=True)
	lstm_cell = rnn.DropoutWrapper(cell=lstm_cell, input_keep_prob=1.0, output_keep_prob=keep_prob)
	mlstm_cell.append(lstm_cell)
mlstm_cell = tf.contrib.rnn.MultiRNNCell(mlstm_cell,state_is_tuple=True)

# **Step3: Initiate state with zero
init_state = mlstm_cell.zero_state(batch_size, dtype=tf.float32)

# **Step4: Calculate in timeStep
outputs = list()
state = init_state
with tf.variable_scope('RNN'):
    for timestep in range(timestep_size):
        if timestep > 0:
            tf.get_variable_scope().reuse_variables()
        # Variable "state" store the LSTM state
        (cell_output, state) = mlstm_cell(X[:, timestep, :], state)
        outputs.append(cell_output)
h_state = outputs[-1]

# h_state is the output of hidden layer
# Weight, Bias, Softmax to predict
W = tf.Variable(tf.truncated_normal([hidden_size, class_num], stddev=0.1), dtype=tf.float32)
bias = tf.Variable(tf.constant(0.1,shape=[class_num]), dtype=tf.float32)
Y_pre = tf.nn.softmax(tf.matmul(h_state, W) + bias)


# Loss function and accuracy
cross_entropy = -tf.reduce_mean(y * tf.log(Y_pre))
train_op = tf.train.AdamOptimizer(lr).minimize(cross_entropy)

train_correct_prediction = tf.equal(tf.argmax(Y_pre,1), tf.argmax(Y_train,1))
train_accuracy = tf.reduce_mean(tf.cast(train_correct_prediction, "float"))

test_correct_prediction = tf.equal(tf.argmax(Y_pre,1), tf.argmax(Y_test,1))
test_accuracy = tf.reduce_mean(tf.cast(test_correct_prediction, "float"))

####################################################################

# Set up GPU demand
config = tf.ConfigProto()
config.gpu_options.per_process_gpu_memory_fraction = 0.5 # maximun alloc gpu50% of MEM
config.gpu_options.allow_growth = True

init = tf.global_variables_initializer()
out = tf.identity(Y_pre, name="output")
saver = tf.train.Saver()

print ("Start...")
with tf.Session(config=config) as sess:
	sess.run(init)
	for i in range(epoch_num):
	    batch = mnist.train.next_batch(_batch_size)
	    reshape_trainX = np.array(batch[0]).reshape(-1, 28, 28)
	    reshape_testX = np.array(mnist.test.images).reshape(-1, 28, 28)
	    if (i+1)%200 == 0:
	        accuracy = sess.run(train_accuracy, feed_dict={
	            X_train:batch[0], Y_train: batch[1], keep_prob: 1.0, batch_size: _batch_size, X: reshape_trainX})
	        print ("Iter%d, step %d, training accuracy %g" % ( mnist.train.epochs_completed, (i+1), accuracy))
	    sess.run(train_op, feed_dict={X_train: batch[0], Y_train: batch[1], keep_prob: 0.5, batch_size: _batch_size, X: reshape_trainX})

	# Testing data accuracy
	print ("test accuracy %g"% sess.run(test_accuracy, feed_dict={
	    X_test: mnist.test.images, Y_test: mnist.test.labels, keep_prob: 1.0, batch_size:mnist.test.images.shape[0], X: reshape_testX}))

	saver.save(sess, "model/rnn.ckpt")

	frozen_tensors = [out]
	out_tensors = [out]

	frozen_graphdef = tf.graph_util.convert_variables_to_constants(sess, sess.graph_def, list(map(canonical_name, frozen_tensors)))
	tf.train.write_graph(frozen_graphdef, "model",
                     'rnn.pb', as_text=False)
	tflite_model = tf.contrib.lite.toco_convert(frozen_graphdef, [X_train], out_tensors, allow_custom_ops=True)

	open("writer_model.tflite", "wb").write(tflite_model)