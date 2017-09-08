#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Fri Aug 25 20:48:14 2017

@author: Congying.Xu
"""

import numpy
import getStrucCmptScors
from time import time
import getFeatureLocation_result


kong_num =0

#aimList：表示已知的答案列表
#resultLIst：表示工具计算的结果列表
def getMAPandMRR(aimList,resultList):
    if len(aimList) == 0:
        #global kong_num
        #print kong_num
        kong_num = kong_num +1
        return(0 ,0)
    
    result_list = []
    
    for file0 in aimList:
        if not file0 in resultList:
            result_list.append(0)
        else:
            result_list.append(resultList.index(file0)+1)
        
    result_list0 = numpy.sort(numpy.array(result_list)) #内容便为 已排好序的结果
    MAP_result = 0.0
    MRR_result = 0.0
    per_num = 1 #用于表示前面已有的
    flag = 0
    for i in range(len(result_list0)):
        if result_list0[i] == 0 :
             pass
             #MAP_result =  MAP_result + 0
        else:
             MAP_result =  MAP_result + per_num / float(result_list0[i])
             if flag == 0:
                 MRR_result = 1 / float(result_list0[i])
                 flag=1
             per_num = per_num + 1
    MAP_result =  MAP_result / len(aimList) 
     
    return (MAP_result ,MRR_result)


def getAimList():
    #准备设计成 字典，以issuekey 作为键   ，  其aimresulr  为值
    Aimresult={}
    import csv
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


"""
def main():
    Aimresult = getAimList()
    Result_dict = getFeatureLocation_result.main()
    MAP=0.0
    MRR=0.0
    Result=[]
    for key in Result_dict:
        t=getMAPandMRR(Aimresult[key],Result_dict[key])
        Result.append( t )
        MAP = MAP + t[0]
        MRR = MRR + t[1]
    #global kong_num
    MAP =  MAP/(len( ResultList) - kong_num)
    print MAP
"""


#参数训练版   
def main(Aimresult,Result_dict):    
    MAP=0.0
    MRR=0.0
    Result=[]
    
    for key in Result_dict:
        #kong_num =0
        t=getMAPandMRR(Aimresult[key],Result_dict[key])
        Result.append( t )
        MAP = MAP + t[0]
        MRR = MRR + t[1]
    #global kong_num
    MAP =  MAP/(len( Result)-kong_num )
    #print MAP
    return MAP
    
if __name__ == '__main__':   
    Result_dict = getFeatureLocation_result.main()
    print main(getAimList(), Result_dict)

"""
def getresult(data , weights):
    #转为实验设计
    All_result =[]
    for result in data:
        all_result={}
        for i in range(len(result[0])):
            all_result0= weights[0]*result[1][i] +weights[1]*result[2][i] +weights[2]*result[3][i] +weights[3]*result[4][i]
            + weights[4]*result[5][i] +weights[5]*result[6][i] +weights[6]*result[7][i] +weights[7]*result[8][i] 
            all_result[result[0][i]] = all_result0
        all_result = sorted(all_result.iteritems(), key = lambda asd:asd[1], reverse = True)
        All_result.append(all_result)
    return All_result
"""