#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Tue Dec 12 10:25:17 2017

@author: Congying.Xu
"""


#import computeSimilarity
import xlrd  
import math
import csv
import time
from multiprocessing import Pool

from aip import AipNlp

""" 你的 APPID AK SK """
APP_ID = '10493871'
API_KEY = '97AMli67i7a5GlWeFuPRNR91'
SECRET_KEY = 'yMaiiCKUB8G4bImZC263wRYUTiVrun1y '

client = AipNlp(APP_ID, API_KEY, SECRET_KEY)
model_dict = {'model':'CNN'}

class switch(object):
    def __init__(self, value):
        self.value = value
        self.fall = False

    def __iter__(self):
        """Return the match method once, then stop"""
        yield self.match
        raise StopIteration
    
    def match(self, *args):
        """Indicate whether or not to enter a case suite"""
        if self.fall or not args:
            return True
        elif self.value in args: # changed for v1.5, see below
            self.fall = True
            return True
        else:
            return False

    
def getnewreportInfo():
    oldReports_dir='Input/HadoopCommon.xlsx'
    workbook = xlrd.open_workbook(oldReports_dir,'r')
    sheet1 = workbook.sheet_by_name('general_report')
    #print sheet1.cell(6,28).value.encode('utf-8')
    All_newreportinfo=[]
    
    data = range(4,6)# + range(204,1004)
    for i in data:
        print i
        newReportSummary=sheet1.cell(i,2).value
        newReportDescription=sheet1.cell(i,28).value
        newReportSummaryAndDescription = newReportSummary + ' ' +newReportDescription
        newReportComponent=sheet1.cell(i,16).value.split(',')
        newReportReporter=sheet1.cell(i,8).value
        newReportPriority=sheet1.cell(i,5).value.encode('utf-8')
        newReportIssueKey=sheet1.cell(i,1).value.encode('utf-8')
        newReportLinkedissue=sheet1.cell(i,26).value.encode('utf-8').split(',')
        
        
        #print time()-begin,11
        
        workbook = xlrd.open_workbook(oldReports_dir,'r')
        sheet1 = workbook.sheet_by_name('general_report')
        
        new=0
        for case in switch (newReportPriority):
                if case('Blocker'):
                    new = 1
                    break
                if case('Critical'):
                    new = 2
                    break
                if case('Major'):
                    new = 3
                    break
                if case('Minor'):
                    new = 4
                    break
                if case('Trivial'):
                    new = 5
            
        #Llediss_result=[] 
        newreportinfo=[]
        newreportinfo.append(newReportDescription)
        newreportinfo.append(newReportSummary)
        newreportinfo.append(newReportSummaryAndDescription)
        newreportinfo.append(newReportComponent)
        newreportinfo.append(newReportReporter)
        newreportinfo.append(new)
        newreportinfo.append(newReportIssueKey)
        newreportinfo.append(newReportLinkedissue)
        All_newreportinfo.append( newreportinfo )
    return  All_newreportinfo
       
def getOldreportsInfo():
    oldReports_dir='Input/HadoopCommon.xlsx'
    workbook = xlrd.open_workbook(oldReports_dir,'r')
    sheet1 = workbook.sheet_by_name('general_report')
    
    OldReportsDescription=[]
    OldReportsSummary=[]
    OldReportsSummaryAndDescription=[]
    OldReportsComponent = []    #每个项为列表类型
    OldReportsReporter = []
    OldReportsPriority = []     
    OldReportsIssueKey = []     #每个项为列表类型
    OldReportsLinkedissue = []  #每个项为列表类型
    #得到所有文档的分词结果为：【 [" "] ，... ，[" "] 】
    #提取old report信息，并文本与处理
    for i in range(4,1004):####下面的字典中，也要改范围
        texts1=sheet1.cell(i,28).value
        OldReportsDescription.append(texts1)#【 [" "] ，... ，[" "] 】
        #print time()

        texts2=sheet1.cell(i,2).value
        OldReportsSummary.append(texts2)#【 [" "] ，... ，[" "] 】
        #print time()
        #texts0=[]
        #texts0.append(sheet1.cell(i,2).value.encode('utf-8')
        #+sheet1.cell(i,28).value.encode('utf-8'))   #[ “ ”] 
        #texts0=Tokenize_stopwords_stemmer(texts0)
        OldReportsSummaryAndDescription.append(texts1+texts2)#【 [" "] ，... ，[" "] 】
        #print time()
        
        
        #result0=0
        #texts0=[]
        str0 = ''
        str0=str0 + sheet1.cell(i,16).value.encode('utf-8')   #[ “ ”]
        OldReportComponentList=str0.split(',')
        OldReportsComponent.append(OldReportComponentList)###
        #OldReportsComponentList.append(texts0)
        
        
        str0 = ''
        str0=str0 + sheet1.cell(i,8).value.encode('utf-8')   #[ “ ”]
        oldReportReporter=str0
        OldReportsReporter.append(oldReportReporter)###
       
        
        str0 = ''
        str0=str0 + sheet1.cell(i,5).value.encode('utf-8')   
        oldReportPriority=str0
        old=0
        for case in switch(oldReportPriority):
            if case('Blocker'):
                old = 1
                break
            if case('Critical'):
                old = 2
                break
            if case('Major'):
                old = 3
                break
            if case('Minor'):
                old = 4
                break
            if case('Trivial'):
                old = 5
        OldReportsPriority.append(old)###
        
       
        
        str0 = ''
        str0=str0 + sheet1.cell(i,26).value.encode('utf-8')   
        oldReportLinked_issues=str0.split(',')
        OldReportsLinkedissue.append(oldReportLinked_issues)###
        
        oldReportIssueKey=sheet1.cell(i,1).value.encode('utf-8') 
        OldReportsIssueKey.append(oldReportIssueKey)###
        
        print i
    oldreportsInfo=[]
    oldreportsInfo.append(OldReportsDescription)
    oldreportsInfo.append(OldReportsSummary)
    oldreportsInfo.append(OldReportsSummaryAndDescription)
    oldreportsInfo.append(OldReportsComponent)
    oldreportsInfo.append(OldReportsReporter)
    oldreportsInfo.append(OldReportsPriority)
    oldreportsInfo.append(OldReportsIssueKey)
    oldreportsInfo.append(OldReportsLinkedissue)
    
    return oldreportsInfo
 
def getOldreportsSimilarScores(newreportinfo):
    newReportDescription=newreportinfo[0]
    newReportSummary=newreportinfo[1]
    newReportSummaryAndDescription = newreportinfo[2]
    newReportComponent=newreportinfo[3]
    newReportReporter=newreportinfo[4]
    newReportPriority=newreportinfo[5]  #特别注意，这里传进来的就直接是数值了
    new = newReportPriority
    newReportIssueKey=newreportinfo[6]
    newReportLinkedissue=newreportinfo[7]
    
    OldReportsDescription=oldreportsInfo[0]
    OldReportsSummary=oldreportsInfo[1]
    OldReportsSummaryAndDescription=oldreportsInfo[2]
    OldReportsComponent = oldreportsInfo[3]    #每个项为列表类型
    OldReportsReporter = oldreportsInfo[4]
    OldReportsPriority = oldreportsInfo[5]
    OldReportsIssueKey = oldreportsInfo[6]     #每个项为列表类型
    OldReportsLinkedissue = oldreportsInfo[7]  #每个项为列表类型
    #若newReportText不在OldReports中，则自动添加到最后
    
    
    
    SD2SD_result = []
    if len(newReportSummaryAndDescription) > 510:
            newReportSummaryAndDescription = newReportSummaryAndDescription[:510]
    num = 5
    for oldReportsSummaryAndDescription in OldReportsSummaryAndDescription:
        num = num +1
        if len(oldReportsSummaryAndDescription) > 510:
            oldReportsSummaryAndDescription = oldReportsSummaryAndDescription[:510]
        
        reponse = {}
        try:
            
            newReportSummaryAndDescription = newReportSummaryAndDescription.encode('utf-8').strip()
            oldReportsSummaryAndDescription = oldReportsSummaryAndDescription.encode('utf-8').strip()
            
            newReportSummaryAndDescription = newReportSummaryAndDescription.encode('gbk', 'ignore')
            oldReportsSummaryAndDescription = oldReportsSummaryAndDescription.encode('gbk', 'ignore')
        
            reponse=client.simnet(newReportSummaryAndDescription,oldReportsSummaryAndDescription,model_dict)
       
        
        except UnicodeDecodeError:
            score = 0
            
            print 'UnicodeDecodeError',num
            #print oldReportsSummaryAndDescription
        
        if reponse.has_key('score'):
                score = reponse['score']
                #print score
                #print newReportSummaryAndDescription,oldReportsSummaryAndDescription
        else:
                score= 0
                #print reponse,'s'
                
        SD2SD_result.append(score)
        
   
    
    Prio_result=[]
    for old in OldReportsPriority:
        result0 = 1.0/(1+math.fabs(new - old) )   #new 表示新的report中的priority
        Prio_result.append(result0)
    
    if len(SD2SD_result) > len(Prio_result):
        SD2SD_result.pop(-1)   #因为最后项  为new report
    
    return {newReportIssueKey:SD2SD_result}
    


All_newreportinfo = getnewreportInfo()
oldreportsInfo = getOldreportsInfo()


def main():
    
    pool = Pool(4)
    All_SD_result = pool.map(getOldreportsSimilarScores,All_newreportinfo)
    pool.close() # 关闭进程池，表示不能在往进程池中添加进程
    pool.join() # 等待进程池中的所有进程执行完毕，必须在close()之后调用
    print len(All_SD_result[0])
    return All_SD_result



def write(All_SD_result):
    with open("Input/Hadoop_similarReport_SD2SD_Scores.csv","w") as csvfile:
        writer = csv.writer(csvfile)
        #writer.writerows(All_SD_result)
        for SD_result in All_SD_result:
            writer.writerow( SD_result.keys() + SD_result[SD_result.keys()[0]] )
        
    
if __name__ == '__main__': 
    
    All_newreportinfo = getnewreportInfo()
    oldreportsInfo = getOldreportsInfo()
    begin = time.time()
    All_SD_result = main()
    write(All_SD_result)
    
    print time.time() - begin

    
