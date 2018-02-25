#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Sun Nov 19 11:10:56 2017

@author: Congying.Xu
"""

print 'start'
import evaluate
print '1' 
import getSimilarityScores2Reports
print '2'
import getStrucCmptScors
print '5'
import csv
import getFeatureLocation_result
#功能简化型调参数
def getevaluate(weights):

    Similarreports_result = getSimilarityScores2Reports.getFinalResultsbyWeights(All_result,weights[2:9],issuekey_file_num,issuekey_file_list)
    Structure_result = getStrucCmptScors.getFinal_result(All_result0 , weights[9:])
   
    Result_dict = getFeatureLocation_result.getFinal_Result(Similarreports_result , Structure_result , weights[:2])
    Aimresult=getAimList()
    
    evaluate_result = evaluate.main_All(Aimresult,Result_dict)
    getFeatureLocation_result.write(Result_dict, evaluate_result)
    return evaluate_result

#为了减少I/O，定向制作

#为针对  csv文件单独写的 ,用于特征定位调参数   
def getAimList():
    #准备设计成 字典，以issuekey 作为键   ，  其aimresulr  为值
    Aimresult={}
    with open("Input/Hadoop_Attachments_PatchInfo.csv","r") as csvfile:
        reader = csv.reader(csvfile)
        #这里不需要readlines
        for i,rows in enumerate(reader):
            #if i <20 :
                aimresult = rows[1:]
                Aimresult[rows[0]] = aimresult
            #else:
             #   break
    return Aimresult

weights = [0.20000000000000015, 0.5000000000000001, 0, 0, 1, 0.10000000000000014, 0.10000000000000014, 0, 0.4, 1, 1, 1]
#[0.20000000000000015, 0.5000000000000001, 0, 0, 1, 0.10000000000000014, 0.10000000000000014, 0, 0.4, 1, 1, 1]
#weights = [1,  0.39589087254670696,       0, 0, 1, 0.4, 0.6, 0, 0.4, 1, 
#           1 ,1]
#0 1 调餐  两个角度feature location的结果  【0】structureScores；【1】similarReports
#2 3 为0     4为1  5 6 7 调参数   8为1    
#后二默认不调参


All_result,issuekey_file_num,issuekey_file_list = getSimilarityScores2Reports.getAll_Info()
All_result0=getStrucCmptScors.getall_result()
Similarreports_result = getSimilarityScores2Reports.getFinalResultsbyWeights(All_result,weights[2:9],issuekey_file_num,issuekey_file_list)
Structure_result = getStrucCmptScors.getFinal_result(All_result0 , weights[9:])


solutionnow =weights 
valuenow = getevaluate(solutionnow)#目标函数解
print valuenow

