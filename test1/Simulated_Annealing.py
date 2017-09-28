#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Thu Aug 24 20:47:39 2017

@author: Congying.Xu
"""
import numpy as np
import getStrucCmptScors
import test0
import csv
import evaluate 
import getFeatureLocation_result
import getSimilarityScores2Reports
from time import time

begin = time()
print begin
#相关参数初始化
def initpara():
    alpha = 0.99
    t = (1,100)
    markovlen = 1   #每次迭代的次数

    return alpha,t,markovlen

#待训练的数据集


def getevaluate(weights):

    Similarreports_result = getSimilarityScores2Reports.getFinalResultsbyWeights(All_result,weights[2:9],issuekey_file_num,issuekey_file_list)
    Structure_result = getStrucCmptScors.getFinal_result(All_result0 , weights[9:])
    Result_dict = getFeatureLocation_result.getFinal_Result(Similarreports_result , Structure_result , weights[:2])    
    
    #MAP = evaluate.main(Aimresult,Result_dict)用于训练
    #return MAP
    evaluate_result  = evaluate.main_All(Aimresult,Result_dict)#用于测试集
    getFeatureLocation_result.write(Result_dict,evaluate_result)
    return evaluate_result

#为了减少I/O，定向制作
      
def getAimList():
    #准备设计成 字典，以issuekey 作为键   ，  其aimresulr  为值
    Aimresult={}
    with open("Input/Attachments_PatchInfo.csv","r") as csvfile:
        reader = csv.reader(csvfile)
        #这里不需要readlines
        for i,rows in enumerate(reader):
            #if i <20 :
                aimresult = rows[1:]
                Aimresult[rows[0]] = aimresult
            #else:
             #   break
    return Aimresult

weights = [1, 0.7000000000000001, 1.0, 0.10000000000000014, 0.40000000000000013, 1.3877787807814457e-16, 0.10000000000000014, 1.3877787807814457e-16, 1, 1, 0.40000000000000013, 0.10000000000000014, 0.20000000000000015, 1, 1, 1, 1]

#weights = [1, 0.7000000000000001, 0.8, 1.3877787807814457e-16, 1.0, 0.40000000000000013, 1, 0.8, 1, 1, 0.6000000000000001, 0.10000000000000014, 0.5000000000000001, 1, 1, 1, 1]
Aimresult=getAimList()
All_result,issuekey_file_num,issuekey_file_list = getSimilarityScores2Reports.getAll_Info()
All_result0=getStrucCmptScors.getall_result()

print time()
solutionnew =weights 
valuenew = getevaluate(solutionnew)#目标函数解
print valuenew
"""
solutioncurrent = solutionnew
valuecurrent = valuenew

solutionbest = solutionnew
valuebest = valuenew

alpha,t2,markovlen = initpara()
t = t2[1]

valuebest_result = [] #记录迭代过程中的最优解
solutionbest_result = []
while t > t2[0]:
    for i in np.arange(markovlen):
        
        solutionnew[0] = np.random.rand()  #[0,1)
        solutionnew[1] = np.random.rand()
        #solutionnew[2] = np.random.rand()
        #solutionnew[3] = np.random.rand()
        #solutionnew[4] = np.random.rand()
        #solutionnew[5] = np.random.rand()
        #solutionnew[6] = np.random.rand()
        #solutionnew[7] = np.random.rand()
        #solutionnew[8] = np.random.rand()
        #solutionnew[9] = np.random.rand()
        #solutionnew[10] = np.random.rand()
        #solutionnew[11] = np.random.rand()
        #solutionnew[12] = np.random.rand()
        #solutionnew[13] = np.random.rand()
        #solutionnew[14] = np.random.rand()
        #solutionnew[15] = np.random.rand()
        #solutionnew[16] = np.random.rand()

        valuenew = getevaluate(solutionnew)
       

        if valuenew > valuecurrent: #接受该解
            #更新solutioncurrent 和solutionbest
            valuecurrent = valuenew
            solutioncurrent = solutionnew
            #print valuenew
            if valuenew > valuebest:
                valuebest = valuenew
                solutionbest = solutionnew
        else:#按一定的概率接受该解
            #print valuecurrent,valuenew
            if np.random.rand() < np.exp((valuenew - valuecurrent)/t):
                valuecurrent = valuenew
                solutioncurrent = solutionnew
            else:
                solutionnew = solutioncurrent

    t = alpha*t
    valuebest_result.append(valuebest)
    solutionbest_result.append(solutionbest)
    print t #程序运行时间较长，打印t来监视程序进展速度
    print time()

#print valuebest_result
#print solutionbest_result

print valuebest
print solutionbest
end = time()
print begin -end
"""

