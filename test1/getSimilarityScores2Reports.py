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
import xlwt  
import math
from nltk.tokenize import StanfordTokenizer
from nltk.corpus import stopwords#停词
import re
import nltk
from time import time
import csv
import collections

#############################################################################
#############################################################################
def Tokenize_stopwords_stemmer(texts):
    #print time()
    #用斯坦福的分词采用这一段，用普通分词时不用这个
    #tokenize
    Str_texts=texts[0]
    #tokenizer = StanfordTokenizer(path_to_jar=r"/Users/apple/Documents/tools/stanford-parser-full-2015-04-20/stanford-parser.jar")
    tokenizer = StanfordTokenizer(path_to_jar=r"stanford-parser.jar")
    texts_tokenized=tokenizer.tokenize(Str_texts)#输入必须是字符串
    #print time()
    p2=r'.+[-_\./"].+'
    pa2=re.compile(p2)
    texts_filtered=[]
    for document in  texts_tokenized:
        if document in pa2.findall(document):
            if document.find('_')>-1 :
                texts_filtered = texts_filtered + document.split('_')
            elif document.find('-')>-1:
                texts_filtered = texts_filtered + document.split('-')
            elif document.find('.')>-1:
                texts_filtered = texts_filtered + document.split('.')
        else:
            texts_filtered.append(document)
    #print time()
    p1=r'[-@<#$%^&*].+'
    pa1=re.compile(p1) 
    p3=r'.+">'
    pa3=re.compile(p3)
    english_stopwords = stopwords.words('english')#得到停词
    english_punctuations = [',', '.', ':', ';', '?', '(', ')', '[', ']', '&', '!', '*', '@', '#', '$', '%','\n'
                            ,'1','2','3','4','5','6','7','8','9','0','<','>','/','\"','\'','{','}','!','~','`'
                            ,'$','^','/*','*/','/**','**/','**','-','_','+','=',r'-?-',r'@?']#得到标点
    texts_filtered0=[]
    for document in texts_filtered:
        if  document in pa1.findall(document) or document in pa3.findall(document) or document == '' or document == "''" or document == "``" or document in english_stopwords or  document in english_punctuations:
            pass
        else:
            texts_filtered0.append(document)
    #print time()
            
    porter = nltk.PorterStemmer()
    texts_Stemmered=[porter.stem(t) for t in texts_filtered0]#列表类型
    #print time()
        
    return texts_Stemmered #返回一个列表


#统计关键词及个数
def CountKey(text_words):
    try:
        #统计格式 格式<Key:Value> <属性:出现个数>
        i = 0
        table = {}
        
        #字典插入与赋值
        for word in text_words:
            if word!="" and table.has_key(word):      #如果存在次数加1
                num = table[word]
                table[word] = num + 1
            elif word!="":                            #否则初值为1
                table[word] = 1
        i = i + 1

        #键值从大到小排序 函数原型：sorted(dic,value,reverse)
        dic = sorted(table.iteritems(), key = lambda asd:asd[1], reverse = True)
        #print dic
        return dic
        
    except Exception,e:    
        print 'Error:',e
    finally:
        pass
        #print 'END\n\n'

#其中  text1以及 basic_texts都是经过tokenize_stopwords_stemmer(texts)处理的
#此输入为tokenize_stopwords_stemmer(texts):的输出
#def Half_computeSimilarity(text1, basic_texts):#计算数据集中所有文本相似度
def Half_computeSimilarity(text1, all_reports_tokens):
    #text1:[“。。。”]  basic_texts:【 [“  ” , "  " , "  " ] , 
    #                                 ......
    #                                [ "  ","  ","  " ] 】
    
    #检查text1，是否在数据集中
    if text1 in all_reports_tokens:
        pass
    else:
        all_reports_tokens.append(text1)      
    
    word_dict={}
    for text in all_reports_tokens:
        w=[]#用于记录  已有的词，防止 一个文本中的多个词都被计数
        for word in text:
            if word in w:
                continue
            if word_dict.has_key(word):
                word_dict[word] = word_dict[word] + 1
            else:
                word_dict[word]=1
            w.append(word)
            
    result=[] 
    dic1 = CountKey(text1)
    
    for i in range(len(all_reports_tokens)):
    #计算文档2-互动的关键词及个数
        dic2 = CountKey(all_reports_tokens[i])
        
        x=0.0
        x1=0.0
        x2=0.0
        
        t1={}
        for i in range(len(dic1)):
            x1 = x1 + dic1[i][1]/float(word_dict[dic1[i][0]])*dic1[i][1]/float(word_dict[dic1[i][0]])
            t1[dic1[i][0]]=dic1[i][1]
        for i in range(len(dic2)):    
            x2 = x2 + dic2[i][1]/float(word_dict[dic2[i][0]])*dic2[i][1]/ float(word_dict[dic2[i][0]])
            if t1.has_key(dic2[i][0]):
                x = x + dic2[i][1]*t1[dic2[i][0]]/ float(word_dict[dic2[i][0]] * word_dict[dic2[i][0]])
        try:
            result0 = float(x) / ( math.sqrt(x1) * math.sqrt(x2) )
        except ZeroDivisionError:
            result0=0.0
        #合并两篇文章的关键词及相似度计算
        result.append( result0 )
            
    return result
##############################################################################
##############################################################################


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
    oldReports_dir='Input/Hbase.xlsx'
    workbook = xlrd.open_workbook(oldReports_dir,'r')
    sheet1 = workbook.sheet_by_name('general_report')
    #print sheet1.cell(6,28).value.encode('utf-8')
    All_newreportinfo=[]
    
    data = range(904,1004)# + range(204,1004)
    for i in data:
        print i
        newReportSummary=sheet1.cell(i,2).value.encode('utf-8')
        newReportDescription=sheet1.cell(i,28).value.encode('utf-8')
        newReportSummaryAndDescription = newReportSummary + ' ' +newReportDescription
        newReportComponent=sheet1.cell(i,16).value.encode('utf-8').split(',')
        newReportReporter=sheet1.cell(i,8).value.encode('utf-8')
        newReportPriority=sheet1.cell(i,5).value.encode('utf-8')
        newReportIssueKey=sheet1.cell(i,1).value.encode('utf-8')
        newReportLinkedissue=sheet1.cell(i,26).value.encode('utf-8').split(',')
        
        
        #print time()-begin,11
        
        workbook = xlrd.open_workbook(oldReports_dir,'r')
        sheet1 = workbook.sheet_by_name('general_report')
        #print sheet1.cell(6,28).value.encode('utf-8')
        #print time()-begin,11
        newReportDescriptionText=[]
        newReportDescriptionText.append(newReportDescription)
        newReportDescriptionText_tokens=Tokenize_stopwords_stemmer(newReportDescriptionText)
        #print time()-begin,11
        newReportSummaryText=[]
        newReportSummaryText.append(newReportSummary)
        newReportSummaryText_tokens=Tokenize_stopwords_stemmer(newReportSummaryText)
        #print time()-begin,11
        newReportSummaryAndDescriptionText_tokens = newReportSummaryText_tokens+newReportDescriptionText_tokens
      
        
        """
        print time()-begin,22
        OldReportsDescription_tokens=[]
        OldReportsSummary_tokens=[]
        OldReportsSummaryAndDescription_tokens=[]
        OldReportsComponent = []    #每个项为列表类型
        OldReportsReporter = []
        OldReportsPriority = []     
        OldReportsIssueKey = []     #每个项为列表类型
        OldReportsLinkedissue = []  #每个项为列表类型
        
        Comp_result=[]
        Rpoter_result=[]
        Prio_result=[]
        """
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
        newreportinfo.append(newReportDescriptionText_tokens)
        newreportinfo.append(newReportSummaryText_tokens)
        newreportinfo.append(newReportSummaryAndDescriptionText_tokens)
        newreportinfo.append(newReportComponent)
        newreportinfo.append(newReportReporter)
        newreportinfo.append(new)
        newreportinfo.append(newReportIssueKey)
        newreportinfo.append(newReportLinkedissue)
        All_newreportinfo.append( newreportinfo )
    return  All_newreportinfo
       
def getOldreportsInfo():
    oldReports_dir='Input/Hbase.xlsx'
    workbook = xlrd.open_workbook(oldReports_dir,'r')
    sheet1 = workbook.sheet_by_name('general_report')
    
    OldReportsDescription_tokens=[]
    OldReportsSummary_tokens=[]
    OldReportsSummaryAndDescription_tokens=[]
    OldReportsComponent = []    #每个项为列表类型
    OldReportsReporter = []
    OldReportsPriority = []     
    OldReportsIssueKey = []     #每个项为列表类型
    OldReportsLinkedissue = []  #每个项为列表类型
    #得到所有文档的分词结果为：【 [" "] ，... ，[" "] 】
    #提取old report信息，并文本与处理
    for i in range(4,1004):####下面的字典中，也要改范围
        texts0=[]
        texts0.append(sheet1.cell(i,28).value.encode('utf-8'))   #[ “ ”] 
        texts1=Tokenize_stopwords_stemmer(texts0)
        OldReportsDescription_tokens.append(texts1)#【 [" "] ，... ，[" "] 】
        #print time()
        texts0=[]
        texts0.append(sheet1.cell(i,2).value.encode('utf-8'))   #[ “ ”] 
        texts2=Tokenize_stopwords_stemmer(texts0)
        OldReportsSummary_tokens.append(texts2)#【 [" "] ，... ，[" "] 】
        #print time()
        #texts0=[]
        #texts0.append(sheet1.cell(i,2).value.encode('utf-8')
        #+sheet1.cell(i,28).value.encode('utf-8'))   #[ “ ”] 
        #texts0=Tokenize_stopwords_stemmer(texts0)
        OldReportsSummaryAndDescription_tokens.append(texts1+texts2)#【 [" "] ，... ，[" "] 】
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
    oldreportsInfo.append(OldReportsDescription_tokens)
    oldreportsInfo.append(OldReportsSummary_tokens)
    oldreportsInfo.append(OldReportsSummaryAndDescription_tokens)
    oldreportsInfo.append(OldReportsComponent)
    oldreportsInfo.append(OldReportsReporter)
    oldreportsInfo.append(OldReportsPriority)
    oldreportsInfo.append(OldReportsIssueKey)
    oldreportsInfo.append(OldReportsLinkedissue)
    
    return oldreportsInfo
 
def getOldreportsSimilarScores(newreportinfo,oldreportsInfo):
    newReportDescriptionText_tokens=newreportinfo[0]
    newReportSummaryText_tokens=newreportinfo[1]
    newReportSummaryAndDescriptionText_tokens = newreportinfo[2]
    newReportComponent=newreportinfo[3]
    newReportReporter=newreportinfo[4]
    newReportPriority=newreportinfo[5]  #特别注意，这里传进来的就直接是数值了
    new = newReportPriority
    newReportIssueKey=newreportinfo[6]
    newReportLinkedissue=newreportinfo[7]
    
    OldReportsDescription_tokens=oldreportsInfo[0]
    OldReportsSummary_tokens=oldreportsInfo[1]
    OldReportsSummaryAndDescription_tokens=oldreportsInfo[2]
    OldReportsComponent = oldreportsInfo[3]    #每个项为列表类型
    OldReportsReporter = oldreportsInfo[4]
    OldReportsPriority = oldreportsInfo[5]
    OldReportsIssueKey = oldreportsInfo[6]     #每个项为列表类型
    OldReportsLinkedissue = oldreportsInfo[7]  #每个项为列表类型
    #若newReportText不在OldReports中，则自动添加到最后
    D2D_result=Half_computeSimilarity(newReportDescriptionText_tokens, OldReportsDescription_tokens)
    S2S_result=Half_computeSimilarity(newReportSummaryText_tokens, OldReportsSummary_tokens)
    SD2SD_result=Half_computeSimilarity(newReportSummaryAndDescriptionText_tokens, OldReportsSummaryAndDescription_tokens)
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

    
    print len(D2D_result)
    print len(S2S_result)
    print len(SD2SD_result)
    print len(Comp_result)
    print len(Rpoter_result)
    print len(Prio_result)
    print len(Llediss_result)
    
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
    with open("Input/Attachments_PatchInfo.csv","r") as csvfile:
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
        #a=[0.38 , 0.29 , 0.36 , 0.20 , 0.23 , 0.60 , 0.5]
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
    
    workbook = xlrd.open_workbook(r'Input/issuekeys_UsedAPI.xlsx')
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
            
        API_scores_list = sorted(API_scores_dict.iteritems(), key = lambda asd:asd[1], reverse = True)#列表类型，【 （key，value） 】
        
        Result_dict[newReportIssueKey] = API_scores_list
        
    return Result_dict

    
    
def getAll_Info():#用于调参数
    All_newreportinfo = getnewreportInfo()
    oldreportsInfo = getOldreportsInfo()
    All_result=[]
    for newreportinfo in All_newreportinfo:
        all_result = getOldreportsSimilarScores(newreportinfo,oldreportsInfo)
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
        all_result = getOldreportsSimilarScores(newreportinfo,oldreportsInfo)
        All_result.append(all_result)
    issuekey_file_num,issuekey_file_list = getPathinfo()
    #All_result = [all_result] 
    Result_dict = getFinalResultsbyWeights(All_result,weights,issuekey_file_num,issuekey_file_list)
    return Result_dict
    
    #end = time()
    #print "Total procesing time: %d seconds" % (end - begin)
 
def main_API(weights):
    
    #begin = time()
    #weights = [0.38 , 0.29 , 0.36 , 0.20 , 0.23 , 0.60 , 0.5]
    All_newreportinfo = getnewreportInfo()
    oldreportsInfo = getOldreportsInfo()
    All_result=[]
    for newreportinfo in All_newreportinfo:
        all_result = getOldreportsSimilarScores(newreportinfo,oldreportsInfo)
        All_result.append(all_result)
    issuekey_UsedAPI_num,issuekey_UsedAPI_list = getIssueKey_UsedAPIinfo()()
    #All_result = [all_result] 
    Result_dict = getFinalAPIResultsbyWeights(All_result,weights,issuekey_UsedAPI_num,issuekey_UsedAPI_list)#这里已经当作  多个计算处理了
    return Result_dict
    
"""  
if __name__ == '__main__':   
    main()
"""
    
    
