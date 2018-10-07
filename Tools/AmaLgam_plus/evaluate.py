#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Fri Aug 25 20:48:14 2017

@author: Congying.Xu
实现各个评估指标
Aimresult  正确答案的字典存储
Result_dict  目前答案的字典存储

#aimList：表示已知的答案列表
#resultLIst：表示工具计算的结果列表
"""

import numpy

#aimList：表示已知的答案列表
#resultLIst：表示工具计算的结果列表
def getMAPandMRR(aimList,resultList):
    if len(aimList) == 0:
        return(0,0)
    
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
    with open("Input/CXF_Attachments_PatchInfo.csv","r") as csvfile:
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
    kong_num = 0
    for key in Result_dict:
        #kong_num =0
        t=getMAPandMRR(Aimresult[key],Result_dict[key])
        Result.append( t )
        MAP = MAP + t[0]
        MRR = MRR + t[1]
        if t[0] ==  0 :
            kong_num = kong_num + 1
    #global kong_num
    #print len( Result)
    #print kong_num
    #print MAP
    try:
        MAP =  MAP/(len( Result)- kong_num )
    except ZeroDivisionError:
        MAP = 0.0
    #print MAP
    return MAP



def get_all_evalution(aimList,resultList):#MAP  MPP @1   @5  @10 
    if len(aimList) == 0:
        return(0,0,0,0,0)
    
    result_list = []
    k1  = 0
    k5 = 0
    k10 = 0
    
    for file0 in aimList:
        if not file0 in resultList:
            result_list.append(0)
        else:
            result_list.append(resultList.index(file0)+1)
            
    result_list0 = numpy.sort(numpy.array(result_list)) #内容便为 已排好序的结果
    
    index_k = 0
    for i in range(len(result_list0)):
        if result_list0[i] == 0 :
             pass
             #MAP_result =  MAP_result + 0
        else:
            index_k = result_list0[i]
            break
    if index_k == 0 or index_k >10:
        k1 = 0
        k5 = 0
        k10 = 0    
    elif index_k == 1:
        k1 = 1
        k5 = 1
        k10 = 1
    elif index_k <=5 and index_k >1 :
        k1 = 0
        k5 = 1
        k10 = 1
    elif index_k <=10 and index_k >5:
        k1 = 0
        k5 = 0
        k10 = 1
        
        
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
     
    return (MAP_result ,MRR_result,k1,k5,k10)

#用于测试集   多个评估标准的结果
def main_All(Aimresult,Result_dict):    
    MAP=0.0
    MRR=0.0
    k1=0.0
    k5=0.0
    k10=0.0
    Result=[]
    kong_num = 0
    for key in Result_dict:
        #kong_num =0
        t=get_all_evalution(Aimresult[key],Result_dict[key])
        Result.append( t )
        MAP = MAP + t[0]
        MRR = MRR + t[1]
        k1 = k1 + t[2]
        k5 = k5 + t[3]
        k10 = k10 + t[4]
        if t[0] ==  0 :
            kong_num = kong_num + 1
    try:
        MAP =  MAP/(len( Result)- kong_num )
    except ZeroDivisionError:
        MAP = 0
    try:
        MRR =  MRR/(len( Result)- kong_num )
    except ZeroDivisionError:
        MRR = 0
    try:
        k1 =  k1/(len( Result)- kong_num )
    except ZeroDivisionError:
        k1 = 0
    try:
        k5 =  k5/(len( Result)- kong_num )
    except ZeroDivisionError:
        k5 = 0
    try:
        k10 =  k10/(len( Result)- kong_num )
    except ZeroDivisionError:
        k10 = 0
        
    #print MAP
    #print MRR
    #print k1
    #print k5
    #print k10
    return MAP,MRR,k1,k5,k10
    
"""    
if __name__ == '__main__':   
    Result_dict = getFeatureLocation_result.main()
    #print main(getAimList(), Result_dict)
    main(getAimList(), Result_dict)
"""
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