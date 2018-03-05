#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Thu Feb 22 15:39:03 2018

@author: Congying.Xu


用于实现  version history component
"""
from multiprocessing import Pool
import xlrd
import csv
import math
e =  math.e 
K = 15  #by default

Project_name  = 'HadoopHDFS'
#返回 issuekey对应的日期  
#{ issuekey:date }
def getIssue2Date_Info():
    oldReports_dir='Input/' + Project_name +'.xlsx'
    workbook = xlrd.open_workbook(oldReports_dir,'r')
    sheet1 = workbook.sheet_by_name('general_report')

    issue_ResolvedDate_dict = {}
    issue_CreatedDate_dict = {}
    for i in range(4,1004):#表格中 4～1004行记录了有用的信息
        issuekey = sheet1.cell(i,1).value
        ResolvedDate = sheet1.cell(i,13).value
        if ResolvedDate=='':
            issue_ResolvedDate_dict[issuekey] = float(sheet1.cell(i,12).value)
        else:
            issue_ResolvedDate_dict[issuekey] = float(ResolvedDate)
        
        CreatedDate = sheet1.cell(i,10).value
        issue_CreatedDate_dict[issuekey] = float(CreatedDate)
    return issue_ResolvedDate_dict, issue_CreatedDate_dict, sheet1 


#找到 K 天之内的issuekeys, 输入newissuekey,输出 [ oldissekeys in K ]
def getVersionHis_score(newissuekey):
    
    #findIssuekeys_K
    Issuekeys_K_info_dict = {}  #{issuek ; 日期差值}
    
    newissuekey_date = float(issue_CreatedDate_dict[newissuekey])
    Issuekeys_K_info_dict = {k:newissuekey_date - v for k, v in issue_date_dict.iteritems() if newissuekey_date - v <=15 and newissuekey_date - v>=0  }
    
    
     
    #computing score of file           
    file_scores_dict = {}
    for key in Issuekeys_K_info_dict:
        if key!= newissuekey:  #去掉自己
            if issuekey_file_list.has_key(key):
                for file0 in issuekey_file_list[key]:
                    
                    tc = Issuekeys_K_info_dict[key]
                    score =1/( 1+ e**(12*( 1- (K - tc)/K )) )
                    if file_scores_dict.has_key(file0):
                        file_scores_dict[file0] = file_scores_dict[file0] + score
                    else:
                        file_scores_dict[file0] = score
   

    #print newissuekey
    return {newissuekey:file_scores_dict}
    
    
def getPathinfo():
    #根据排名靠前的report 找到对应的  .java文件
    issuekey_file_num = {}  #用于存放 issuekey及其对应 修复文件的个数
    issuekey_file_list = {} #存放  issuekey，及其对应 修复文件   0个时，不放入其中
    with open("Input/" + Project_name +"_Attachments_PatchInfo.csv","r") as csvfile:
        reader = csv.reader(csvfile)
        #这里不需要readlines
        #先确保topk 的report中都是有 附件的
        for i,rows in enumerate(reader):
             issuekey_file_num[rows[0]] = len(rows) - 1
             if len(rows) > 1:
                 issuekey_file_list[rows[0]] = rows[1:]
    return issuekey_file_num,issuekey_file_list     
    
issue_date_dict = {}
issue_date_dict, issue_CreatedDate_dict,sheet1  = getIssue2Date_Info()
issuekey_file_num,issuekey_file_list = getPathinfo()

def init():
    pass

from multiprocessing import freeze_support
freeze_support()

def main():
    
    #getVersionHis_score('HADOOP-14557')
    
    Result_dict = {}
    Result_list = []
    #pool = Pool(10)
    All_newissuekey = []
    for i in range(4,1004):#+(904,1004):
        All_newissuekey.append( sheet1.cell(i,1).value )
        Result_list.append( getVersionHis_score(sheet1.cell(i,1).value) )
    """
    Result_dict = {}
    
    pool = Pool(10)
    All_newissuekey = []
    for i in range(4,14):
        All_newissuekey.append( sheet1.cell(i,1).value )
    
    Result_list = pool.map(getVersionHis_score,All_newissuekey)   
    pool.close() # 关闭进程池，表示不能在往进程池中添加进程
    pool.join() # 等待进程池中的所有进程执行完毕，必须在close()之后调用
    """
    for ele in Result_list:
        Result_dict[ ele.keys()[0] ] = ele.values()[0]

    return Result_dict
   

if __name__=='__main__':
    main()