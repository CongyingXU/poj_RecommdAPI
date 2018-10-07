#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Wed Aug  9 09:11:35 2017

@author: Congying.Xu
I/O表格3s
注意点！
！！！！！！！！！！！
主要的时间花费在  斯坦福分词器上，以下内容是针对一个new report
如果是多个时：做微调
对old report信息，只需提取一次 文本与处理一次！！！！；之后便用来与new report 做计算即可。大的for，一定只需要一次。

"""

#import computeSimilarity
import xlrd  
import math
import csv

from aip import AipNlp

""" 你的 APPID AK SK """
APP_ID = '10493871'
API_KEY = '97AMli67i7a5GlWeFuPRNR91'
SECRET_KEY = 'yMaiiCKUB8G4bImZC263wRYUTiVrun1y '

client = AipNlp(APP_ID, API_KEY, SECRET_KEY)
model_dict = {'model':'CRNN'}

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

    """
def main():

    begin = time()
    
    newReportSummary='Address some alerts raised by lgtm.com'
    newReportDescription='lgtm.com has identified a number of issues in the code, see https://lgtm.com/projects/g/apache/hbase/alerts/ This ticket is to address some of those issues, such as resources not being closed; and results of integer multiplication being cast to long (with potential for overflow).'
    newReportSummaryAndDescription = newReportSummary + newReportDescription
    newReportComponent=['']#列表类型，表格中是以’，‘号分割的字符串;若没有，则为【‘’】
    newReportReporter='Malcolm Taylor'
    newReportPriority='Major'
    newReportIssueKey='HBASE-18434'
    newReportLinkedissue=['']  #列表类型，表格中是以’，‘号分割的字符串;若没有，则为【‘’】
    """
def getnewreportInfo():
    oldReports_dir='Input/Hive.xlsx'
    workbook = xlrd.open_workbook(oldReports_dir,'r')
    sheet1 = workbook.sheet_by_name('general_report')
    #print sheet1.cell(6,28).value.encode('utf-8')
    All_newreportinfo=[]
    
    data = range(904,1004)# + range(904,1004)
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
    oldReports_dir='Input/Hive.xlsx'
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
        """
        if OldReportComponentList[0]=='' or OldReportComponentList[0]=='':
            result0=0    
        else:
            count = 0
            for x in newReportComponent:
                if x in OldReportComponentList:
                    count=count+1
            
            result0=count/(math.sqrt(len(newReportComponent))*math.sqrt(len(OldReportComponentList)))
        Comp_result.append(result0)#
        #print time()
        """
        
        str0 = ''
        str0=str0 + sheet1.cell(i,8).value.encode('utf-8')   #[ “ ”]
        oldReportReporter=str0
        OldReportsReporter.append(oldReportReporter)###
        """
        if newReportReporter == oldReportReporter:
            Rpoter_result.append(1)    
        else:
            Rpoter_result.append(0)
        #print time()
        """
        
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
        
        """
        result0 = 1.0/(1+math.fabs(new - old) )  
        Prio_result.append(result0)
        #print time()
        """
        
        str0 = ''
        str0=str0 + sheet1.cell(i,26).value.encode('utf-8')   
        oldReportLinked_issues=str0.split(',')
        OldReportsLinkedissue.append(oldReportLinked_issues)###
        
        oldReportIssueKey=sheet1.cell(i,1).value.encode('utf-8') 
        OldReportsIssueKey.append(oldReportIssueKey)###
        """
        if newReportLinkedissue[0]=='' or  oldReportLinked_issues[0]=='':
            Llediss_result.append(0)
        elif newReportIssueKey in oldReportLinked_issues or  oldReportIssueKey in newReportLinkedissue:
            Llediss_result.append(1)
        else:
            Llediss_result.append(0)
        """
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
 
def getOldreportsSimilarScores(newreportinfo,oldreportsInfo,issuekey_similar_dict):
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
    
    
    if issuekey_similar_dict.has_key(newReportIssueKey):
        SD2SD_result = issuekey_similar_dict [ newReportIssueKey ] 
    else:
        print newReportIssueKey,'no key'
        
    D2D_result = len( SD2SD_result)*[0]
    S2S_result = len( SD2SD_result)*[0]
    
    #特殊处理一下，作为归一化处理
    Max_socre  = SD2SD_result[0]
    for score in SD2SD_result:
        if Max_socre < score and score < 1 :#里面有一个是其本身。最大值为一
            Max_socre = score
            
    if Max_socre> 0:
        for i in range(len(SD2SD_result)):
            SD2SD_result[i] = SD2SD_result[i] / Max_socre
        
    Comp_result=[]
    for OldReportComponentList in OldReportsComponent:
        if OldReportComponentList[0]=='' or OldReportComponentList[0]=='':
                result0=0    
        else:
                count = 0
                for x in newReportComponent:
                    if x in OldReportComponentList:
                        count=count+1
                
                result0=count/(math.sqrt(len(newReportComponent))*math.sqrt(len(OldReportComponentList)))
        Comp_result.append(result0)
        
    Rpoter_result=[]
    for oldReportReporter in OldReportsReporter:
        if newReportReporter == oldReportReporter:
            Rpoter_result.append(1)    
        else:
            Rpoter_result.append(0)
    
    Prio_result=[]
    for old in OldReportsPriority:
        result0 = 1.0/(1+math.fabs(new - old) )   #new 表示新的report中的priority
        Prio_result.append(result0)
    
    Llediss_result=[]
    for i in range(len(OldReportsLinkedissue)):
    #for oldReportLinked_issues in OldReportsLinkedissue:
        oldReportLinked_issues = OldReportsLinkedissue[i]
        oldReportIssueKey = OldReportsIssueKey[i]
        if newReportLinkedissue[0]=='' or  oldReportLinked_issues[0]=='':
            Llediss_result.append(0)
        elif newReportIssueKey in oldReportLinked_issues or  oldReportIssueKey in newReportLinkedissue:
            Llediss_result.append(1)
        else:
            Llediss_result.append(0)
    
    #print time()

    """
    print len(D2D_result)
    print len(S2S_result)
    print len(SD2SD_result)
    print len(Comp_result)
    print len(Rpoter_result)
    print len(Prio_result)
    print len(Llediss_result)
    """
    
    if len(D2D_result) > len(Llediss_result):
        D2D_result.pop(-1)   #因为最后项  为new report
    if len(S2S_result) > len(Llediss_result):
        S2S_result.pop(-1)   #因为最后项  为new report
    if len(SD2SD_result) > len(Llediss_result):
        SD2SD_result.pop(-1)   #因为最后项  为new report
    
    all_result =[]
    all_result.append(D2D_result)
    all_result.append(S2S_result)
    all_result.append(SD2SD_result)
    all_result.append(Comp_result)
    all_result.append(Rpoter_result)
    all_result.append(Prio_result)
    all_result.append(OldReportsIssueKey)
    all_result.append(Llediss_result)
    all_result.append(newReportIssueKey)
    
    return all_result


def getPathinfo():
    #根据排名靠前的report 找到对应的  .java文件
    issuekey_file_num = {}  #用于存放 issuekey及其对应 修复文件的个数
    issuekey_file_list = {} #存放  issuekey，及其对应 修复文件   0个时，不放入其中
    with open("Input/Hive_Attachments_PatchInfo.csv","r") as csvfile:
        reader = csv.reader(csvfile)
        #这里不需要readlines
        #先确保topk 的report中都是有 附件的
        for i,rows in enumerate(reader):
             issuekey_file_num[rows[0]] = len(rows) - 1
             if len(rows) > 1:
                 issuekey_file_list[rows[0]] = rows[1:]
    return issuekey_file_num,issuekey_file_list

def getFinalResultsbyWeights(All_result,weights,issuekey_file_num,issuekey_file_list):#这里已经当作  多个计算处理了
    Result_dict={}
    #权重
    a=weights
    for all_result in All_result:
        result_dict={}
        D2D_result = all_result[0]
        S2S_result = all_result[1]
        SD2SD_result = all_result[2]
        Comp_result = all_result[3]
        Rpoter_result = all_result[4]
        Prio_result = all_result[5]
        OldIssueKey = all_result[6]
        Llediss_result = all_result[7]
        newReportIssueKey = all_result[8]
        
        Final_SimlarScors2Reports=[]
        for i in range(len(Llediss_result)):
            score = a[0]*S2S_result[i] + a[1]*D2D_result[i] + a[2]*SD2SD_result[i] + a[3]*Rpoter_result[i]+ a[4]*Comp_result[i]+ a[5]*Prio_result[i] + a[6]*Llediss_result[i]
            Final_SimlarScors2Reports.append( score )
            result_dict[ OldIssueKey[i] ]=score
        result_list = sorted(result_dict.iteritems(), key = lambda asd:asd[1], reverse = True)
        
        #列表类型，【 （key，value） 】
        """
        #根据排名靠前的report 找到对应的  .java文件
        issuekey_file_num = {}  #用于存放 issuekey及其对应 修复文件的个数
        issuekey_file_list = {} #存放  issuekey，及其对应 修复文件   0个时，不放入其中
        with open("Input/Attachments_PatchInfo.csv","r") as csvfile:
            reader = csv.reader(csvfile)
            #这里不需要readlines
            #先确保topk 的report中都是有 附件的
            for i,rows in enumerate(reader):
                 issuekey_file_num[rows[0]] = len(rows) - 1
                 if len(rows) > 1:
                     issuekey_file_list[rows[0]] = rows[1:]
        """
        k = 15
        num = 0 
        topk_result_report = []
        for i in range(len(result_list)):
            if num >= k:
                break
            elif issuekey_file_num[result_list[i][0]] > 0:
                if result_list[i][0] == newReportIssueKey:#去掉自己
                    continue  
                topk_result_report.append(result_list[i][0])
                num = num + 1
                
        file_num_dict = {}
        for key in issuekey_file_list:
            if key in topk_result_report:
                for file0 in issuekey_file_list[key]:
                    if file_num_dict.has_key(file0):
                        file_num_dict[file0] = file_num_dict[file0] +1
                    else:
                        file_num_dict[file0] = 1
            
        file_scores_dict ={}###为该词的结果
        for key in file_num_dict:
            file_scores_dict[key] = file_num_dict[key]/float(k)
            
        #file_scores_list = sorted(file_scores_dict.iteritems(), key = lambda asd:asd[1], reverse = True)#列表类型，【 （key，value） 】
        
        #Result_dict[newReportIssueKey] = file_scores_list
        Result_dict[newReportIssueKey] = file_scores_dict
        
    return Result_dict
    #print result_dict    
    #print time()
    
    
    #数据写入
    """
    f = xlwt.Workbook() #创建工作簿
    sheet1 = f.add_sheet(u'sheet1',cell_overwrite_ok=True) #创建sheet
    
    workbook = xlrd.open_workbook(r'Input/Hbase.xlsx')
    sheet = workbook.sheet_by_name('general_report')
    #print sheet1.cell(6,28).value.encode('utf-8')
    
    sheet1.write(0,0,'IssuesKey'.encode('utf-8'))
    sheet1.write(0,1,'D2D_result'.encode('utf-8'))
    sheet1.write(0,2,'S2S_result'.encode('utf-8'))
    sheet1.write(0,3,'SD2SD_result'.encode('utf-8'))
    sheet1.write(0,4,'Comp_result'.encode('utf-8'))
    sheet1.write(0,5,'Rpoter_result'.encode('utf-8'))
    sheet1.write(0,6,'Prio_result'.encode('utf-8'))
    sheet1.write(0,7,'Llediss_result'.encode('utf-8'))
    sheet1.write(0,8,'Final_SimlarScors2Reports'.encode('utf-8'))
    
    for i in range(len(Final_SimlarScors2Reports)):
        
        sheet1.write(i+1,0,sheet.cell(i+4,1).value.encode('utf-8'))#字符串   #表格的第一行开始写。第一列，第二列。。。。
        sheet1.write(i+1,1,D2D_result[i])
        sheet1.write(i+1,2,S2S_result[i])
        sheet1.write(i+1,3,SD2SD_result[i])
        sheet1.write(i+1,4,Comp_result[i])
        sheet1.write(i+1,5,Rpoter_result[i])
        sheet1.write(i+1,6,Prio_result[i])
        sheet1.write(i+1,7,Llediss_result[i])
        sheet1.write(i+1,8,Final_SimlarScors2Reports[i])
        
    f.save('Output/ReportsSimilartest0.xls')#保存文件
    #问题！！！不能追加，会把原来的文件覆盖掉！！！
    """
    #end = time()
    #print "Total procesing time: %d seconds" % (end - begin)
    

#IssueKey 找到对应使用过的API
def getIssueKey_UsedAPIinfo():
    issuekey_UsedAPI_num = {}  #用于存放 issuekey及其对应 修复文件的个数
    issuekey_UsedAPI_list = {} #存放  issuekey，及其对应 修复文件   0个时，不放入其中
    
    workbook = xlrd.open_workbook(r'Input/Hive_issuekeys_UsedAPI.xls')
    sheet = workbook.sheet_by_name('sheet1')

    for j in range(1,sheet.nrows):
        issuekey=sheet.cell(j,0).value
        if sheet.cell(j,1).value=='':
            issuekey_UsedAPI_num[issuekey] = 0
        else:
            issuekey_UsedAPI_num[issuekey] = len( sheet.cell(j,1).value.split(';') )
            issuekey_UsedAPI_list[issuekey] = sheet.cell(j,1).value.split(';')
    return issuekey_UsedAPI_num,issuekey_UsedAPI_list
    
#得到最终  issuekey——API的分数
def getFinalAPIResultsbyWeights(All_result,weights,issuekey_UsedAPI_num,issuekey_UsedAPI_list):#这里已经当作  多个计算处理了
    Result_dict={}
    #权重
    a=weights
    for all_result in All_result:
        result_dict={}
        D2D_result = all_result[0]
        S2S_result = all_result[1]
        SD2SD_result = all_result[2]
        Comp_result = all_result[3]
        Rpoter_result = all_result[4]
        Prio_result = all_result[5]
        OldIssueKey = all_result[6]
        Llediss_result = all_result[7]
        newReportIssueKey = all_result[8]
        #a=[0.38 , 0.29 , 0.36 , 0.20 , 0.23 , 0.60 , 0.5]
        Final_SimlarScors2Reports=[]
        for i in range(len(Llediss_result)):
            score = a[0]*S2S_result[i] + a[1]*D2D_result[i] + a[2]*SD2SD_result[i] + a[3]*Rpoter_result[i]+ a[4]*Comp_result[i]+ a[5]*Prio_result[i] + a[6]*Llediss_result[i]
            Final_SimlarScors2Reports.append( score )
            result_dict[ OldIssueKey[i] ]=score
        result_list = sorted(result_dict.iteritems(), key = lambda asd:asd[1], reverse = True)
        #列表类型，【 （key，value） 】
        
        """
        #根据排名靠前的report 找到使用的API
        issuekey_UsedAPI_num = {}  #用于存放 issuekey及其对应 修复文件的个数
        issuekey_UsedAPI_list = {} #存放  issuekey，及其对应 修复文件   0个时，不放入其中
        """
        k = 15
        num = 0 
        topk_result_report = []
        for i in range(len(result_list)):
            if num >= k:
                break
            elif issuekey_UsedAPI_num[result_list[i][0]] > 0:
                if result_list[i][0] == newReportIssueKey:#去掉自己
                    continue  
                topk_result_report.append(result_list[i][0])
                num = num + 1
                
        API_num_dict = {}
        for key in issuekey_UsedAPI_list:
            if key in topk_result_report:
                for file0 in issuekey_UsedAPI_list[key]:
                    if API_num_dict.has_key(file0):
                        API_num_dict[file0] = API_num_dict[file0] +1
                    else:
                        API_num_dict[file0] = 1
            
        API_scores_dict ={}###为该词的结果
        for key in API_num_dict:
            API_scores_dict[key] = API_num_dict[key]/float(k)
        
        #数据归一化处理    
        Max_socre  = 0.0
        for key in API_scores_dict:
            if Max_socre < API_scores_dict[key] and API_scores_dict[key] < 1 :#里面有一个是其本身。最大值为一
                Max_socre = API_scores_dict[key]
                
        if Max_socre != 0:
            for key in API_scores_dict:
                API_scores_dict[key] = API_scores_dict[key] / Max_socre
        
        #API_scores_list = sorted(API_scores_dict.iteritems(), key = lambda asd:asd[1], reverse = True)#列表类型，【 （key，value） 】
        
        #Result_dict[newReportIssueKey] = API_scores_list
        Result_dict[newReportIssueKey] = API_scores_dict
        
    return Result_dict

    
    
def getAll_Info():#用于调参数
    All_newreportinfo = getnewreportInfo()
    oldreportsInfo = getOldreportsInfo()
    All_result=[]
    for newreportinfo in All_newreportinfo:
        all_result = getOldreportsSimilarScores(newreportinfo,oldreportsInfo,issuekey_similar_dict)
        All_result.append(all_result)
    issuekey_file_num,issuekey_file_list = getPathinfo()
    
    return All_result,issuekey_file_num,issuekey_file_list
 


def main(weights):
    
    #begin = time()
    #weights = [0.38 , 0.29 , 0.36 , 0.20 , 0.23 , 0.60 , 0.5]
    All_newreportinfo = getnewreportInfo()
    oldreportsInfo = getOldreportsInfo()
    All_result=[]
    for newreportinfo in All_newreportinfo:
        all_result = getOldreportsSimilarScores(newreportinfo,oldreportsInfo, issuekey_similar_dict)
        All_result.append(all_result)
    issuekey_file_num,issuekey_file_list = getPathinfo()
    #All_result = [all_result] 
    Result_dict = getFinalResultsbyWeights(All_result,weights,issuekey_file_num,issuekey_file_list)
    return Result_dict
    
    #end = time()
    #print "Total procesing time: %d seconds" % (end - begin)

def get_issuekey_similar_scores():
    issuekey_similar_dict = {}
    
    with open("Input/Hive_similarReport_SD2SD_Scores.csv","r") as csvfile:
            reader = csv.reader(csvfile)
            #这里不需要readlines
            #print reader
            for i,rows in enumerate(reader):
                scores = rows[1:1000+1]
                for j in range(len(scores)):
                    scores[j] = float(scores[j])
                issuekey_similar_dict[ rows[0] ] = scores
            
    return issuekey_similar_dict
issuekey_similar_dict = get_issuekey_similar_scores()

def main_API(weights):
    
    #begin = time()
    # = [0.9, 0.7000000000000001, 1.3877787807814457e-16, 1.3877787807814457e-16, 0.9, 0.40000000000000013, 0.6000000000000001, 1.3877787807814457e-16, 0.40000000000000013, 1, 1, 0.5000000000000001, 0.20000000000000015, 1, 1, 1, 1] 
    #weights = [1, 1, 0.10000000000000014, 1, 0.7000000000000001, 0, 0, 0.9, 0.9, 0.10000000000000014, 0.10000000000000014, 1.0]


    #weights = [0.20000000000000015, 0.5000000000000001, 0, 0, 1, 0.10000000000000014, 0.10000000000000014, 0, 0.4, 1, 1, 1]
    
    All_newreportinfo = getnewreportInfo()
    oldreportsInfo = getOldreportsInfo()
    All_result=[]
    for newreportinfo in All_newreportinfo:
        all_result = getOldreportsSimilarScores(newreportinfo,oldreportsInfo,issuekey_similar_dict)
        All_result.append(all_result)
    issuekey_UsedAPI_num,issuekey_UsedAPI_list = getIssueKey_UsedAPIinfo()
    #All_result = [all_result] 
    Result_dict = getFinalAPIResultsbyWeights(All_result,weights,issuekey_UsedAPI_num,issuekey_UsedAPI_list)#这里已经当作  多个计算处理了
    return Result_dict
    

if __name__ == '__main__':   
    main_API()

    
