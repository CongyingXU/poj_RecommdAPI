#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Sat Nov 18 16:12:53 2017

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
    MAP = evaluate.main(Aimresult,Result_dict)
    return MAP

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

weights = [0.8, 0.39589087254670696, 0, 0, 1, 0.20000000000000015, 0.10000000000000014, 0, 0.4, 1, 1, 1]

#weights = [1,  0.39589087254670696,       0, 0, 1, 0.4, 0.6, 0, 0.4, 1, 
#           1 ,1]
#0 1 调餐  两个角度feature location的结果
#2 3 为0     4为1  5 6 7 调参数   8为1    
#后二默认不调参


All_result,issuekey_file_num,issuekey_file_list = getSimilarityScores2Reports.getAll_Info()
All_result0=getStrucCmptScors.getall_result()
Similarreports_result = getSimilarityScores2Reports.getFinalResultsbyWeights(All_result,weights[2:9],issuekey_file_num,issuekey_file_list)
Structure_result = getStrucCmptScors.getFinal_result(All_result0 , weights[9:])

"""
def getAimList():
    workbook = xlrd.open_workbook(r'Input/Hadoop_issuekeys_UsedAPI.xls')
    sheet = workbook.sheet_by_name('sheet1')
    Aimresult={}
    for j in range(1,sheet.nrows):
        issuekey=sheet.cell(j,0).value
        if sheet.cell(j,1).value=='':
            Aimresult[issuekey] = []
        else:
            Aimresult[issuekey] = sheet.cell(j,1).value.split(';')
    return Aimresult

weights = [1,1,  1,1,1]#前两个用于Src相似度计算时，后三个用于  API 推荐结果的汇总
APISimilarReportsScores_dict  = getSimilarityScores2Reports.main_API()
print '5'
APIdscpScors_dict = getAPIdscpScors.main()
APISrcfileScores_dict = getAPISrcfileScores.main(weights[:2])#weights：自然语言  与程序语言之间的权重关系，决定相似度分数

"""   

solutionnow =weights 
valuenow = getevaluate(solutionnow)#目标函数解
print valuenow

best = 0.0
for count in range(6):
#for index in [1] + range(3,9) + range(10,17):
    #for index in range(len(solutionnow)):
    for index in [0,1,   5,6,7]:
        i=1.0
        best_i = solutionnow[index]
        while i>0.005: 
            solutionnow[index] = i
            valuenew = getevaluate(solutionnow)#目标函数解
            if valuenew> valuenow:
                valuenow = valuenew
                best = valuenow
                best_i = i
            i = i - 0.1
           
        solutionnow[index]  = best_i
        print index
    
    print count
    print valuenow
    print solutionnow

print valuenow
print solutionnow






