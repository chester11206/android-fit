# -*- coding:utf-8 -*-
import tensorflow as tf
from tensorflow.contrib import rnn
from tensorflow.examples.tutorials.mnist import input_data
#import pyrebase
import pandas as pd
import numpy as np
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
import os

# os.environ["CUDA_VISIBLE_DEVICES"] = '0' #use GPU with ID=0
# os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

cred = credentials.Certificate('firebase-adminsdk.json')
firebase_admin.initialize_app(cred, {
    'databaseURL' : 'https://total-cascade-210406.firebaseio.com'
})

configfb = {"apiKey": "AIzaSyBkvh_XeJKp1v5XiRSiQEOsfiG5tfV7d9Y",
"authDomain": "total-cascade-210406.firebaseapp.com",
"databaseURL": "https://total-cascade-210406.firebaseio.com",  
"storageBucket": "total-cascade-210406.appspot.com"
}

lr = 1e-3
# 在训练和测试的时候，我们想用不同的 batch_size.所以采用占位符的方式
batch_size = tf.placeholder(tf.int32, [], name='batch_size')  # 注意类型必须为 tf.int32
# 在 1.0 版本以后请使用 ：
# keep_prob = tf.placeholder(tf.float32, [])
# batch_size = tf.placeholder(tf.int32, [])

# 每个时刻的输入特征是28维的，就是每个时刻输入一行，一行有 28 个像素
input_size = 28
# 时序持续长度为28，即每做一次预测，需要先输入28行
timestep_size = 28
# 每个隐含层的节点数
hidden_size = 256
# LSTM layer 的层数
layer_num = 2
# 最后输出分类类别数量，如果是回归预测的话应该是 1
class_num = 10

_batch_size = 128

epoch_num = 2000

train_p = 0.8

def connect_firebase():
# add a way to encrypt those, I'm a starter myself and don't know how
    # username = "chester11206@gmail.com"
    # password = "abcd1234"

    # firebase = pyrebase.initialize_app(configfb)
    # auth = firebase.auth() 

    # user = auth.sign_in_with_email_and_password(username, password)

    # #user['idToken']
    # # At pyrebase's git the author said the token expires every 1 hour, so it's needed to refresh it
    # user = auth.refresh(user['refreshToken'])

    # #set database
    # db = firebase.database()
    timestep_size = 450
    root = db.reference()

    valuesX = root.child('SensorDataSet').get()

    dataX = pd.DataFrame(valuesX)
    npdataX = np.array(dataX.values).T

    valuesY = root.child('GroundTruth').get()
    dataY = pd.DataFrame(valuesY)
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


def canonical_name(x):
  return x.name.split(":")[0]

trainX, trainY, testX, testY = connect_firebase()

def cond(batch_size):
	print (batch_size)
	if batch_size == _batch_size:
		return True
	else:
		return False

# 首先导入数据，看一下数据的形式
mnist = input_data.read_data_sets('MNIST_data', one_hot=True)
print (mnist.train.images.shape)

_X = tf.placeholder(tf.float32, [_batch_size, input_size*timestep_size], name="imput_train_x")
y = tf.placeholder(tf.float32, [_batch_size, class_num], name="imput_train_y")


x_test = tf.placeholder(tf.float32,[None, input_size*timestep_size])
y_test = tf.placeholder(tf.float32,[None, class_num], name='input_test_y')
keep_prob = tf.placeholder(tf.float32, [])
print (_X.shape)

# 把784个点的字符信息还原成 28 * 28 的图片
# 下面几个步骤是实现 RNN / LSTM 的关键
####################################################################
# **步骤1：RNN 的输入shape = (batch_size, timestep_size, input_size)
#p = cond(batch_size)
p = tf.placeholder(tf.bool, [])
#p = tf.Variable(cond(batch_size), dtype=tf.bool)
print ("b")

# X_train = tf.reshape(_X, [-1, 28, 28])
# X_test = tf.reshape(x_test, [-1, 28, 28])
X = tf.placeholder(tf.float32, [None, 28, 28])
#X = tf.where(p, tf.reshape(_X, [-1, 28, 28]), tf.reshape(x_test, [-1, 28, 28]))
print ("a")

# **步骤2：定义一层 LSTM_cell，只需要说明 hidden_size, 它会自动匹配输入的 X 的维度
#lstm_cell = rnn.BasicLSTMCell(num_units=hidden_size, forget_bias=1.0, state_is_tuple=True)

# **步骤3：添加 dropout layer, 一般只设置 output_keep_prob
#lstm_cell = rnn.DropoutWrapper(cell=lstm_cell, input_keep_prob=1.0, output_keep_prob=keep_prob)

# **步骤4：调用 MultiRNNCell 来实现多层 LSTM
#mlstm_cell = rnn.MultiRNNCell([lstm_cell] * layer_num, state_is_tuple=True)

mlstm_cell = []
for i in range(layer_num):
	lstm_cell = rnn.BasicLSTMCell(num_units=hidden_size, forget_bias=1.0, state_is_tuple=True)
	lstm_cell = rnn.DropoutWrapper(cell=lstm_cell, input_keep_prob=1.0, output_keep_prob=keep_prob)
	mlstm_cell.append(lstm_cell)
mlstm_cell = tf.contrib.rnn.MultiRNNCell(mlstm_cell,state_is_tuple=True)

# **步骤5：用全零来初始化state
init_state = mlstm_cell.zero_state(batch_size, dtype=tf.float32)

# **步骤6：方法一，调用 dynamic_rnn() 来让我们构建好的网络运行起来
# ** 当 time_major==False 时， outputs.shape = [batch_size, timestep_size, hidden_size] 
# ** 所以，可以取 h_state = outputs[:, -1, :] 作为最后输出
# ** state.shape = [layer_num, 2, batch_size, hidden_size], 
# ** 或者，可以取 h_state = state[-1][1] 作为最后输出
# ** 最后输出维度是 [batch_size, hidden_size]
# outputs, state = tf.nn.dynamic_rnn(mlstm_cell, inputs=X, initial_state=init_state, time_major=False)
# h_state = outputs[:, -1, :]  # 或者 h_state = state[-1][1]

# *************** 为了更好的理解 LSTM 工作原理，我们把上面 步骤6 中的函数自己来实现 ***************
# 通过查看文档你会发现， RNNCell 都提供了一个 __call__()函数（见最后附），我们可以用它来展开实现LSTM按时间步迭代。
# **步骤6：方法二，按时间步展开计算
outputs = list()
state = init_state
with tf.variable_scope('RNN'):
    for timestep in range(timestep_size):
        if timestep > 0:
            tf.get_variable_scope().reuse_variables()
        # 这里的state保存了每一层 LSTM 的状态
        (cell_output, state) = mlstm_cell(X[:, timestep, :], state)
        outputs.append(cell_output)
h_state = outputs[-1]

# 上面 LSTM 部分的输出会是一个 [hidden_size] 的tensor，我们要分类的话，还需要接一个 softmax 层
# 首先定义 softmax 的连接权重矩阵和偏置
# out_W = tf.placeholder(tf.float32, [hidden_size, class_num], name='out_Weights')
# out_bias = tf.placeholder(tf.float32, [class_num], name='out_bias')
# 开始训练和测试
W = tf.Variable(tf.truncated_normal([hidden_size, class_num], stddev=0.1), dtype=tf.float32)
bias = tf.Variable(tf.constant(0.1,shape=[class_num]), dtype=tf.float32)
y_pre = tf.nn.softmax(tf.matmul(h_state, W) + bias)


# 损失和评估函数
cross_entropy = -tf.reduce_mean(y * tf.log(y_pre))
train_op = tf.train.AdamOptimizer(lr).minimize(cross_entropy)

train_correct_prediction = tf.equal(tf.argmax(y_pre,1), tf.argmax(y,1))
train_accuracy = tf.reduce_mean(tf.cast(train_correct_prediction, "float"))

test_correct_prediction = tf.equal(tf.argmax(y_pre,1), tf.argmax(y_test,1))
test_accuracy = tf.reduce_mean(tf.cast(test_correct_prediction, "float"))
#print (mnist.train.next_batch(_batch_size)[0])

# 设置 GPU 按需增长
config = tf.ConfigProto()
config.gpu_options.per_process_gpu_memory_fraction = 0.3 # maximun alloc gpu50% of MEM
config.gpu_options.allow_growth = True
#sess = tf.Session(config=config)
init = tf.global_variables_initializer()
out = tf.identity(y_pre, name="output")

saver = tf.train.Saver()

print ("start")
with tf.Session(config=config) as sess:
	sess.run(init)
	for i in range(epoch_num):
	    batch = mnist.train.next_batch(_batch_size)
	    reshape_trainX = np.array(batch[0]).reshape(-1, 28, 28)
	    reshape_testX = np.array(mnist.test.images).reshape(-1, 28, 28)
	    if (i+1)%200 == 0:
	        accuracy = sess.run(train_accuracy, feed_dict={
	            _X:batch[0], y: batch[1], keep_prob: 1.0, batch_size: _batch_size, X: reshape_trainX})
	        # 已经迭代完成的 epoch 数: mnist.train.epochs_completed
	        print ("Iter%d, step %d, training accuracy %g" % ( mnist.train.epochs_completed, (i+1), accuracy))
	    sess.run(train_op, feed_dict={_X: batch[0], y: batch[1], keep_prob: 0.5, batch_size: _batch_size, X: reshape_trainX})

	# 计算测试数据的准确率
	print ("test accuracy %g"% sess.run(test_accuracy, feed_dict={
	    x_test: mnist.test.images, y_test: mnist.test.labels, keep_prob: 1.0, batch_size:mnist.test.images.shape[0], X: reshape_testX}))

	saver.save(sess, "model/rnn.ckpt")
	#tf.train.write_graph("/model/rnn.ckpt.data", "/model/rnn.ckpt.meta", "/model/rnn.ckpt.index")

	frozen_tensors = [out]
	# print ([out])
	out_tensors = [out]

	frozen_graphdef = tf.graph_util.convert_variables_to_constants(sess, sess.graph_def, list(map(canonical_name, frozen_tensors)))
	tf.train.write_graph(frozen_graphdef, "model",
                     'rnn.pb', as_text=False)
	tflite_model = tf.contrib.lite.toco_convert(frozen_graphdef, [_X], out_tensors, allow_custom_ops=True)

	open("writer_model.tflite", "wb").write(tflite_model)