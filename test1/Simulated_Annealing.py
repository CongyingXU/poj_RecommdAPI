#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Thu Aug 24 20:47:39 2017

@author: Congying.Xu
"""
import evaluate 
#import getFeatureLocation_result
import getSimilarityScores2Reports
from time import time
import getRecmdAPI_result
import getAPIdscpScors 
import getAPISrcfileScores
import xlrd

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

    APISrcfileScores_dict = getAPISrcfileScores.main(weights[:2])#weights：自然语言  与程序语言之间的权重关系，决定相似度分数
    Result_dict = getRecmdAPI_result.getFinal_Result(APIdscpScors_dict , APISimilarReportsScores_dict , APISrcfileScores_dict , weights[2:])

    Aimresult=getAimList()
    MAP = evaluate.main_All(Aimresult,Result_dict)
    return MAP

#为了减少I/O，定向制作
      
def getAimList():
    workbook = xlrd.open_workbook(r'Input/issuekeys_UsedAPI.xls')
    sheet = workbook.sheet_by_name('sheet1')
    Aimresult={}
    for j in range(1,sheet.nrows):
        issuekey=sheet.cell(j,0).value
        if sheet.cell(j,1).value=='':
            Aimresult[issuekey] = []
        else:
            Aimresult[issuekey] = sheet.cell(j,1).value.split(';')
    return Aimresult
print time()
weights = [0.0, 0.0, 0.2, 0.2, 0.2]#前两个用于Src相似度计算时，后三个用于  API 推荐结果的汇总
APISimilarReportsScores_dict  = getSimilarityScores2Reports.main_API()
APIdscpScors_dict = getAPIdscpScors.main()


solutionnew =weights 
valuenow = getevaluate(solutionnew)#目标函数解
print valuenow


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

