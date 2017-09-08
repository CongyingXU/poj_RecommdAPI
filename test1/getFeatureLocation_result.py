#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Sun Aug 27 09:47:33 2017

@author: Congying.Xu

#功能定位的最终结果
"""
import getSimilarityScores2Reports
import getStrucCmptScors

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
        




def main():
    # 有问题
    #说明一下  weights  [0,1]:a,b   [2,8]:  getSimilarityScores2Reports中的7个参数  [9,17] :getStrucCmptScors中的8个参数
    weights = [0.4765080059930489, 0.08921775453867853, 0.38, 0.29, 0.36, 0.2, 0.23, 0.6, 0.7383606484712233, 0.5881762643232233, 0.4735409434116893, 0.10212499280443976, 0.4363396810754724, 0.5774016678038669, 0.4166697755914751, 0.18319637300523295, 0.14007257039425824]

    Similarreports_result = getSimilarityScores2Reports.main(weights[2:9])
    Structure_result = getStrucCmptScors.main(weights[9:])
    Final_Result = getFinal_Result(Similarreports_result , Structure_result , weights[0:2])
    return Final_Result

if __name__=='__main__':
    main()
