#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Thu Feb 22 17:49:21 2018

@author: Congying.Xu

用以实现  Reporter information component
"""

import xlrd
import csv
from multiprocessing import Pool

Project_name = 'HadoopHDFS'

def getInput_info():
    oldReports_dir='Input/' + Project_name +'.xlsx'
    workbook = xlrd.open_workbook(oldReports_dir,'r')
    sheet1 = workbook.sheet_by_name('general_report')

    issue2reporter_dict = {}
    for i in range(4,1004):
        issuekey = sheet1.cell(i,1).value
        reporter = sheet1.cell(i,8).value
        issue2reporter_dict[issuekey] = reporter

    return  issue2reporter_dict,sheet1

def getAll_files():
    Src_info_file_dir='Output/' + Project_name +'_repo_SrcfileInfo.xls'
    workbook = xlrd.open_workbook(Src_info_file_dir,'r')
    sheet2 = workbook.sheet_by_name('sheet1')
    all_files_list = []
    
    for i in range(1,sheet2.nrows):
        all_files_list.append( sheet2.cell(i,0).value )
    return all_files_list

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

def getRepoter_score(newissuekey):
    newissue_repoter = issue2reporter_dict[ newissuekey ]
    issues_sameReporter_list = [k for k,v in issue2reporter_dict.iteritems() if v == newissue_repoter]
    all_package_name_set = set()
    
    file_list = []
    for issuekey in issues_sameReporter_list:
        if issuekey_file_list.has_key(issuekey): 
            file_list = file_list + issuekey_file_list [issuekey]
    
    for file0 in file_list:
        index0 =  file0.rfind('/')
        if index0 >0:
            package_dir = file0[:index0]
            all_package_name_set.add(package_dir)
            
     
    file_scores_dict ={}
    for file0 in all_files_list:
        index0 =  file0.rfind('/')
        if index0 >0:
            package_dir = file0[:index0]
            if package_dir in all_package_name_set:
                file_scores_dict[file0] = 1
            else:
                file_scores_dict[file0] = 0
    return {newissuekey:file_scores_dict}
        
issue2reporter_dict,sheet1 = getInput_info()  
all_files_list  = []
issuekey_file_num = {}
issuekey_file_list = {}


def init():
    global issue2reporter_dict,sheet1,all_files_list,issuekey_file_num,issuekey_file_list
    
    #issue2reporter_dict,sheet1 = getInput_info()  
    all_files_list = getAll_files()
    issuekey_file_num,issuekey_file_list = getPathinfo()

from multiprocessing import freeze_support
freeze_support()

def main():
    
    #getRepoter_score('HADOOP-14401')
   
    Result_dict = {}
    Result_list = []
    #pool = Pool(10)
    All_newissuekey = []
    for i in range(4,1004):#+(904,1004):
        All_newissuekey.append( sheet1.cell(i,1).value )
        Result_list.append( getRepoter_score(sheet1.cell(i,1).value) )
    """    
    Result_list = pool.map(getRepoter_score,All_newissuekey)   
    pool.close() # 关闭进程池，表示不能在往进程池中添加进程
    pool.join() # 等待进程池中的所有进程执行完毕，必须在close()之后调用
    """
    for ele in Result_list:
        Result_dict[ ele.keys()[0] ] = ele.values()[0]


    print 1
    return Result_dict 
   
if __name__=='__main__':
    main()