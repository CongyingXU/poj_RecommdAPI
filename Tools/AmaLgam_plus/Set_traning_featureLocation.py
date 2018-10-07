#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Sat Nov 18 16:12:53 2017

@author: Congying.Xu
"""

print 'start'
import evaluate
import getSimilarityScores2Reports
import getStrucCmptScors
import getVersionHisScors
import getReporterInfoScors
import csv
import getFeatureLocation_result

print 'Importing is done!'
getSimilarityScores2Reports.init()
getVersionHisScors.init()
getReporterInfoScors.init()
getStrucCmptScors.init()
print 'Initing is done!'




from multiprocessing import freeze_support


#功能简化型调参数
def getevaluate(weights):

    Result_dict = getFeatureLocation_result.getFinal_Result(VersionHis_result, Similarreports_result , Structure_result ,Reporter_result, weights)
    MAP = evaluate.main(Aimresult,Result_dict)
    return MAP

#为了减少I/O，定向制作

#为针对  csv文件单独写的 ,用于特征定位调参数   
def getAimList():
    #准备设计成 字典，以issuekey 作为键   ，  其aimresulr  为值
    Aimresult={}
    with open("Input/HadoopCommon_Attachments_PatchInfo.csv","r") as csvfile:
        reader = csv.reader(csvfile)
        #这里不需要readlines
        for i,rows in enumerate(reader):
            #if i <20 :
                aimresult = rows[1:]
                Aimresult[rows[0]] = aimresult
            #else:
             #   break
    return Aimresult

#c=1
weights = [1, 1, 1, 1]

#weights = [1,  0.39589087254670696,       0, 0, 1, 0.4, 0.6, 0, 0.4, 1, 
#           1 ,1]
#0 1 调餐  两个角度feature location的结果
#2 3 为0     4为1  5 6 7 调参数   8为1    
#后二默认不调参
Aimresult=getAimList()
print '5'
#All_result,issuekey_file_num,issuekey_file_list = getSimilarityScores2Reports.getAll_Info()
Similarreports_result = getSimilarityScores2Reports.main()
print '6'
Structure_result = getStrucCmptScors.main()
print '7'
VersionHis_result = getVersionHisScors.main()
print '8'
Reporter_result = getReporterInfoScors.main()


solutionnow =weights 
valuenow = getevaluate(solutionnow)#目标函数解
print valuenow

best = 0.0
for count in range(3):
#for index in [1] + range(3,9) + range(10,17):
    #for index in range(len(solutionnow)):
    for index in range(len(solutionnow)):
        i=1.0
        best_i = solutionnow[index]
        while i>0.005:
            
            solutionnow[index] = i
            #solutionnow  = [0.3*c, 0.2*0.7*c, 0.8*0.7*c, 1-c]
            valuenew = getevaluate(solutionnow)#目标函数解
            #print valuenew
            #print solutionnow
            if valuenew> valuenow:
                valuenow = valuenew
                best = valuenow
                best_i = i
            i = i - 0.1
           
        
        solutionnow[index] =best_i
        #solutionnow  = [0.3*c, 0.2*0.7*c, 0.8*0.7*c, 1-c]
        print index
    
    print count
    print valuenow
    print solutionnow

print valuenow
print solutionnow


#写结果
Result_dict = getFeatureLocation_result.getFinal_Result(VersionHis_result, Similarreports_result , Structure_result ,Reporter_result,solutionnow)
evaluate_result = evaluate.main_All(Aimresult,Result_dict)
getFeatureLocation_result.write(Result_dict, evaluate_result)
print evaluate_result




