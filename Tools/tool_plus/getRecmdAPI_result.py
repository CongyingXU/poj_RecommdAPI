#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Wed Sep 27 11:12:34 2017

@author: Congying.Xu

API推荐的最终结果汇总
1、API description   Scores # [(key,value)]
2、similar report    
3、Srcfile similar ecores main(issuekey , weights):   SimilarScores#列表类型，【 （key，value） 】
"""
"""
没没没没import没
import getAPIdscpScors
import getAPISrcfileScores
import getSimilarityScores2Reports
"""
import xlwt
 

#优化测试版
def getFinal_Result(APIdscpScors_dict , APISimilarReportsScores_dict , APISrcfileScores_dict , weights):
    Final_Result={}
    for key in  APIdscpScors_dict:#issuekey
        result_dict={}
        for key0 in APIdscpScors_dict[key]:#对应的API
        
            APIdscpScors = APIdscpScors_dict[key][key0] 
            #APISimilarReportsScores
            try:
                APISrcfileScores = APISrcfileScores_dict[key][key0]
            except KeyError:
                APISrcfileScores = 0.0
                #print key,key0
                #############################################################
            if APISimilarReportsScores_dict[key].has_key(key0):
                APISimilarReportsScores = APISimilarReportsScores_dict[key][key0]
                result_dict[key0] = weights[0]*APIdscpScors + weights[1]*APISimilarReportsScores + weights[2]* APISrcfileScores
            else:
                try:
                    result_dict[key0] = weights[0]*APIdscpScors + weights[2]* APISrcfileScores
                except TypeError:     
                    result_dict[key0] = 0.0
                    print key,key0
                    
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
    sheet1.write(0,3,"Hit@1: " + bytes(evaluate_result[2]) )
    sheet1.write(0,4,"Hit@5: " + bytes(evaluate_result[3]) )
    sheet1.write(0,5,"Hit@10: "+ bytes(evaluate_result[4]) )
    
    sheet1.write(1,0,"issuekey".decode('utf-8'))
    sheet1.write(1,1,"Ranked_API_Recommandation".decode('utf-8'))
    i=0
    for key in Final_Result:
        sheet1.write(i+2,0,key)#"issuekey"
        for j in range(len(Final_Result[key])):
            sheet1.write(i+2,j+1,Final_Result[key][j])
            if j>14:
                break#只存前15个
        i = i+1

    f.save('Output/Ranked_API_Recommandation_result9.xls')      

    
    

"""
if __name__=='__main__':
    num_list = range(4,1004)  #全部 
    #     APISimilarReportsScores_dict[newReportIssueKey] = API_scores_list  列表类型，【 （key，value）
    APISimilarReportsScores_dict  = getSimilarityScores2Reports.main_API()
    APIdscpScors_dict = getAPIdscpScors.main()
    APISrcfileScores_dict = getAPISrcfileScores.main(weights)#weights：自然语言  与程序语言之间的权重关系，决定相似度分数
    getFinal_Result(APIdscpScors_dict , APISimilarReportsScores_dict , APISrcfileScores_dict , weights)
"""