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
    
    Aimresult=getAimList()
    MAP = evaluate.main(Aimresult,Result_dict)
    return MAP



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

All_result,issuekey_file_num,issuekey_file_list = getSimilarityScores2Reports.getAll_Info()

All_result0=getStrucCmptScors.getall_result()

solutionnew =weights = [0.7217656912869671,  0.39589087254670696, 0.38, 0.29, 0.36, 0.2, 0.23, 0.6, 0.7383606484712233, 0.5881762643232233, 0.4735409434116893, 0.10212499280443976, 0.4363396810754724, 0.5774016678038669, 0.4166697755914751, 0.18319637300523295, 0.14007257039425824]
valuenew = getevaluate(solutionnew)#目标函数解
print valuenew

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
        solutionnew[8] = np.random.rand()
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

#print valuebest_result
#print solutionbest_result

print valuebest
print solutionbest


