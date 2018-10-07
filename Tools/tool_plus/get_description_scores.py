#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Tue Dec 12 11:36:38 2017

@author: Congying.Xu
"""
from multiprocessing import Pool
import time
import csv
import xlrd
from aip import AipNlp

""" 你的 APPID AK SK """
APP_ID = '10493871'
API_KEY = '97AMli67i7a5GlWeFuPRNR91'
SECRET_KEY = 'yMaiiCKUB8G4bImZC263wRYUTiVrun1y '

client = AipNlp(APP_ID, API_KEY, SECRET_KEY)
model_dict = {'model':'CNN'}


def getAPI_Info():
    
    allAPI_info_dir = 'Input/allAPI_info0.xls'
    workbook = xlrd.open_workbook(allAPI_info_dir,'r')
    sheet2 = workbook.sheet_by_name('sheet1')
    
    allAPI_info_list=[]
    for i in range(sheet2.nrows):
        API=(sheet2.cell(i,0).value,#原
             sheet2.cell(i,1).value,#原
             sheet2.cell(i,2).value,#进过文本预处理的地方，将单词分开
             sheet2.cell(i,3).value,
             sheet2.cell(i,4).value,
             sheet2.cell(i,5).value)
        allAPI_info_list.append(API)
    All_3partAPIinfo_list = allAPI_info_list
    return All_3partAPIinfo_list
    
def computeSimilarityScors(newRportSD):      #,Src_info_file_dir):
    issuekey = newRportSD.split('#key#')[0]
    newRportSD = newRportSD.split('#key#')[1]
    
    
    if issuekey_UsedAPI_list.has_key(issuekey):
        pass
    else:
        return {issuekey:len(all_APIdescription)*[0]}
    
    if len(newRportSD) > 510:
        newRportSD = newRportSD[:510]
    
    
    scores = []
    num = 0
    for APIdescription in all_APIdescription:
        num = num +1
        #print newRportSD
        #print APIdescription
        reponse = {}
        try:
            newRportSD = newRportSD.encode('utf-8').strip()
            APIdescription = APIdescription.encode('utf-8').strip()
            
            newRportSD = newRportSD.encode('gbk', 'ignore')
            APIdescription = APIdescription.encode('gbk', 'ignore')
            
            
        
            reponse=client.simnet(newRportSD,APIdescription,model_dict)
        except UnicodeDecodeError:
            score = 0
            print 'UnicodeDecodeError',num
            
        
        
        if reponse.has_key('score'):
            score = reponse['score']
            #print score,'d'
        else:
            score= 0
            #print reponse,'d'
            #print newRportSD
            #print APIdescription
            
        scores.append(score)
        
    return {issuekey:scores}   #直接用字典，这)# + ran样便于后续的关键字查找

def main():#即issuekey的行号【4:1004】 是全部
    
        
    workbook = xlrd.open_workbook(r'Input/HadoopCommon.xlsx')
    sheet = workbook.sheet_by_name('general_report')
    SD_list = []
    for i in num_list:
        issuekey = sheet.cell(i,1).value.encode('utf-8')
        newReportSummary=sheet.cell(i,2).value.encode('utf-8')
        newReportDescription=sheet.cell(i,28).value.encode('utf-8')
        SD = issuekey + '#key#' + newReportSummary  + newReportDescription
        SD_list.append(SD)
        
        
    print len(SD_list)

    
    pool = Pool(2)
    Scores = pool.map(computeSimilarityScors,SD_list)   
    pool.close() # 关闭进程池，表示不能在往进程池中添加进程
    pool.join() # 等待进程池中的所有进程执行完毕，必须在close()之后调用
    
    return Scores

def write(Scores):
    API_list = ['issuekey\API']
    for i  in range(len(All_3partAPIinfo_list)):
            API = All_3partAPIinfo_list[i][0]+ '.' + All_3partAPIinfo_list[i][1]
            API_list.append(API)
            
    print len(API_list)
    print len(Scores)
    
            
    with open("Input/Hadoop_description_Scores.csv","w") as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(API_list) 
        for score in Scores:
            writer.writerow( score.keys() + score[score.keys()[0]] )
        #writer.writerows(All_SD_result)

#IssueKey 找到对应使用过的API
def getIssueKey_UsedAPIinfo():
    issuekey_UsedAPI_num = {}  #用于存放 issuekey及其对应 修复文件的个数
    issuekey_UsedAPI_list = {} #存放  issuekey，及其对应 修复文件   0个时，不放入其中
    
    workbook = xlrd.open_workbook(r'Input/Hadoop_issuekeys_UsedAPI.xls')
    sheet = workbook.sheet_by_name('sheet1')

    for j in range(1,sheet.nrows):
        issuekey=sheet.cell(j,0).value
        if sheet.cell(j,1).value=='':
            issuekey_UsedAPI_num[issuekey] = 0
        else:
            issuekey_UsedAPI_num[issuekey] = len( sheet.cell(j,1).value.split(';') )
            issuekey_UsedAPI_list[issuekey] = sheet.cell(j,1).value.split(';')
    return issuekey_UsedAPI_num,issuekey_UsedAPI_list

if __name__=='__main__':
    begin = time.time()
    num_list = range(4,14)# +range(904,1004)
    All_3partAPIinfo_list = getAPI_Info()
    all_APIdescription = []
    for ele in All_3partAPIinfo_list:
        all_APIdescription.append(ele[5])
    
    issuekey_UsedAPI_num,issuekey_UsedAPI_list =  getIssueKey_UsedAPIinfo()
    
    Scores = main()
    write(Scores)
    
    print time.time() - begin
    