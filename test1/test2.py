#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Fri Sep 15 09:37:53 2017

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
import getRecmdAPIresult
import getAPIdscpScors 
import getAPISrcfileScores
import xlrd

begin = time()
print begin

#功能简化型调参数
def getevaluate(weights):

    APISrcfileScores_dict = getAPISrcfileScores.main(weights[:2])#weights：自然语言  与程序语言之间的权重关系，决定相似度分数
    Result_dict = getRecmdAPIresult.getFinal_Result(APIdscpScors_dict , APISimilarReportsScores_dict , APISrcfileScores_dict , weights[2:])

    Aimresult=getAimList()
    MAP = evaluate.main(Aimresult,Result_dict)
    return MAP

#为了减少I/O，定向制作
"""
#为针对  csv文件单独写的 ,用于特征定位调参数   
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


weights = [1,  0.39589087254670696, 1, 0.29, 0.36, 0.2, 0.23, 0.6, 0.7383606484712233, 1, 0.4735409434116893, 0.10212499280443976, 0.4363396810754724, 0.5774016678038669, 0.4166697755914751, 0.18319637300523295, 0.14007257039425824]

All_result,issuekey_file_num,issuekey_file_list = getSimilarityScores2Reports.getAll_Info()
All_result0=getStrucCmptScors.getall_result()
Similarreports_result = getSimilarityScores2Reports.getFinalResultsbyWeights(All_result,weights[2:9],issuekey_file_num,issuekey_file_list)
Structure_result = getStrucCmptScors.getFinal_result(All_result0 , weights[9:])
"""
def getAimList():
    workbook = xlrd.open_workbook(r'Input/issuekeys_UsedAPI.xlsx')
    sheet = workbook.sheet_by_name('sheet1')
    Aimresult={}
    for j in range(1,sheet.nrows):
        issuekey=sheet.cell(j,0).value
        if sheet.cell(j,1).value=='':
            Aimresult[issuekey] = 0
        else:
            Aimresult[issuekey] = sheet.cell(j,1).value.split(';')
    return Aimresult

weights = [1,1,  1,1,1]#前两个用于Src相似度计算时，后三个用于  API 推荐结果的汇总
APISimilarReportsScores_dict  = getSimilarityScores2Reports.main_API()
APIdscpScors_dict = getAPIdscpScors.main()

print time()
solutionnew =weights 
valuenow = getevaluate(solutionnew)#目标函数解
print valuenow

best = 0.0
best_i = 0.0
for index in [1] + range(3,9) + range(10,17):
    i=0.1
    while i<10: 
        valuenew = getevaluate(solutionnew)#目标函数解
        if valuenew> valuenow:
            valuenow = valuenew
            best = valuenew
            best_i = i
        i = i +0.1
        solutionnew[index] = i
    solutionnew[index]  = best_i

print valuenow
print solutionnew





