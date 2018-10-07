#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Sun Aug 27 09:47:33 2017

@author: Congying.Xu

#功能定位的最终结果
"""
import getSimilarityScores2Reports
import getStrucCmptScors
import os
import xlwt
"""
def getFinal_Result(Similarreports_result , Structure_result , weights):
    Final_Result={}
    for key in  Structure_result:
        result_dict={}
        for ele in Structure_result[key]:
            flag = 0
            for ele0 in Similarreports_result[key]:
                if ele[0] == ele0[0]:        
                        result_dict[ele[0]] = weights[0]*ele[1]+weights[1]*ele0[1]
                        flag =1
                        break
            
            if flag==0:
                result_dict[ele[0]] = weights[0]*ele[1]
        result_list_ele = sorted(result_dict.iteritems(), key = lambda asd:asd[1], reverse = True)#列表类型，【 （key，value） 】
        result_list = []
        for ele in result_list_ele:
            result_list.append(ele[0])
        Final_Result[key]=result_list
    return Final_Result
"""
#优化测试版
def getFinal_Result(Similarreports_result , Structure_result , weights):
    Final_Result={}
    for key in  Structure_result:
        result_dict={}
        for key0 in Structure_result[key]:
            if Similarreports_result[key].has_key(key0):
                        result_dict[key0] = weights[0]*Structure_result[key][key0]+weights[1]*Similarreports_result[key][key0]
            else:
                result_dict[key0] = weights[0]*Structure_result[key][key0]
        result_list_ele = sorted(result_dict.iteritems(), key = lambda asd:asd[1], reverse = True)#列表类型，【 （key，value） 】
        result_list = []
        for ele in result_list_ele:
            result_list.append(ele[0])
        Final_Result[key]=result_list
    return Final_Result       

def write(Final_Result,evaluate_result):
    f = xlwt.Workbook() #创建工作簿
    sheet1 = f.add_sheet(u'sheet1',cell_overwrite_ok=True) #创建sheet
    #sheet2 = f.add_sheet(u'comments',cell_overwrite_ok=True) #创建sheet
    sheet1.write(0,0,"Evaluation_Result".decode('utf-8'))
    sheet1.write(0,1,"MAP: " + bytes(evaluate_result[0]) )
    sheet1.write(0,2,"MRR: " + bytes(evaluate_result[1])  )
    sheet1.write(0,3,"Recall-Rate@1: " + bytes(evaluate_result[2]) )
    sheet1.write(0,4,"Recall-Rate@5: " + bytes(evaluate_result[3]) )
    sheet1.write(0,5,"Recall-Rate@10: "+ bytes(evaluate_result[4]) )
    
    sheet1.write(1,0,"issuekey".decode('utf-8'))
    sheet1.write(1,1,"ralatedSrcfiles".decode('utf-8'))
    i=0
    for key in Final_Result:
        sheet1.write(i+2,0,key)#"issuekey"
        for j in range(len(Final_Result[key])):
            sheet1.write(i+2,j+1,Final_Result[key][j])
            if j>14:
                break#只存前15个
        i = i+1

    f.save('Output/FeaturnLocation_result9.xls')
            
        
def write_NoResult(Final_Result):
    f = xlwt.Workbook() #创建工作簿
    sheet1 = f.add_sheet(u'sheet1',cell_overwrite_ok=True) #创建sheet
    
    sheet1.write(1,0,"issuekey".decode('utf-8'))
    sheet1.write(1,1,"ralatedSrcfiles".decode('utf-8'))
    i=0
    for key in Final_Result:
        sheet1.write(i+2,0,key)#"issuekey"
        for j in range(len(Final_Result[key])):
            sheet1.write(i+2,j+1,Final_Result[key][j])
            if j>14:
                break#只存前15个
        i = i+1

    f.save('Output/Openissue_FeaturnLocation_result.xls')

def main():
    # 有问题
    #说明一下  weights  [0,1]:a,b   [2,8]:  getSimilarityScores2Reports中的7个参数  [9,17] :getStrucCmptScors中的8个参数
    weights = [0.4765080059930489, 0.08921775453867853, 0.38, 0.29, 0.36, 0.2, 0.23, 0.6, 0.7383606484712233, 0.5881762643232233, 0.4735409434116893, 0.10212499280443976, 0.4363396810754724, 0.5774016678038669, 0.4166697755914751, 0.18319637300523295, 0.14007257039425824]

    Similarreports_result = getSimilarityScores2Reports.main(weights[2:9])
    Structure_result = getStrucCmptScors.main(weights[9:])
    Final_Result = getFinal_Result(Similarreports_result , Structure_result , weights[0:2])
    write_NoResult(Final_Result)
    

if __name__=='__main__':
    main()
