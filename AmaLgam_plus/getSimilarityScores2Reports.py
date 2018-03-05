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
#
import xlwt  
import math
from nltk.tokenize import StanfordTokenizer
from nltk.corpus import stopwords#停词
import re
import nltk
#from time import time
import csv
#import collections
from multiprocessing import Pool

Project_name  = 'HadoopHDFS'
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
    
    """
    #检查text1，是否在数据集中
    if text1 in all_reports_tokens:
        pass
    else:
        all_reports_tokens.append(text1)      
    """
    
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

    
def getnewreportInfo():
    oldReports_dir='Input/' + Project_name +'.xlsx'
    workbook = xlrd.open_workbook(oldReports_dir,'r')
    sheet1 = workbook.sheet_by_name('general_report')
    #print sheet1.cell(6,28).value.encode('utf-8')
    All_newreportinfo=[]
    
    data = range(4,1004)# + range(804,1004)
    for i in data:
        print i
        newReportSummary=sheet1.cell(i,2).value.encode('utf-8')
        newReportDescription=sheet1.cell(i,28).value.encode('utf-8')
        newReportSummaryAndDescription = newReportSummary + ' ' +newReportDescription
        newReportIssueKey=sheet1.cell(i,1).value.encode('utf-8')
        
        
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
      
        #Llediss_result=[] 
        newreportinfo=[]
        newreportinfo.append(newReportDescriptionText_tokens)
        newreportinfo.append(newReportSummaryText_tokens)
        newreportinfo.append(newReportSummaryAndDescriptionText_tokens)
        newreportinfo.append(newReportIssueKey)
        
        All_newreportinfo.append( newreportinfo )
    return  All_newreportinfo
       
def getOldreportsInfo():
    oldReports_dir='Input/' + Project_name +'.xlsx'
    workbook = xlrd.open_workbook(oldReports_dir,'r')
    sheet1 = workbook.sheet_by_name('general_report')
    
    OldReportsDescription_tokens=[]
    OldReportsSummary_tokens=[]
    OldReportsSummaryAndDescription_tokens=[]
    #OldReportsComponent = []    #每个项为列表类型
    #OldReportsReporter = []
    #OldReportsPriority = []     
    OldReportsIssueKey = []     #每个项为列表类型
    #OldReportsLinkedissue = []  #每个项为列表类型
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
        OldReportsSummaryAndDescription_tokens.append(texts1+texts2)#【 [" "] ，... ，[" "] 】
        #print time()
        
        
        oldReportIssueKey=sheet1.cell(i,1).value.encode('utf-8') 
        OldReportsIssueKey.append(oldReportIssueKey)###
      
        print i
    oldreportsInfo=[]
    oldreportsInfo.append(OldReportsDescription_tokens)
    oldreportsInfo.append(OldReportsSummary_tokens)
    oldreportsInfo.append(OldReportsSummaryAndDescription_tokens)
    oldreportsInfo.append(OldReportsIssueKey)
    
    return oldreportsInfo
 
def getOldreportsSimilarScores(newreportinfo):
    #newReportDescriptionText_tokens=newreportinfo[0]
    #newReportSummaryText_tokens=newreportinfo[1]
    newReportSummaryAndDescriptionText_tokens = newreportinfo[2]
    newReportIssueKey=newreportinfo[3]
    
    
    #OldReportsDescription_tokens=oldreportsInfo[0]
    #OldReportsSummary_tokens=oldreportsInfo[1]
    OldReportsSummaryAndDescription_tokens=oldreportsInfo[2]
    OldReportsIssueKey = oldreportsInfo[3]     #每个项为列表类型
    #若newReportText不在OldReports中，则自动添加到最后
    #D2D_result=Half_computeSimilarity(newReportDescriptionText_tokens, OldReportsDescription_tokens)
    #S2S_result=Half_computeSimilarity(newReportSummaryText_tokens, OldReportsSummary_tokens)
    SD2SD_result=Half_computeSimilarity(newReportSummaryAndDescriptionText_tokens, OldReportsSummaryAndDescription_tokens)
    
    #特殊处理一下，作为归一化处理
    Max_socre  = SD2SD_result[0]
    for score in SD2SD_result:
        if Max_socre < score and score < 1 :#里面有一个是其本身。最大值为一
            Max_socre = score
    
    for i in range(len(SD2SD_result)):
        SD2SD_result[i] = SD2SD_result[i] / Max_socre
        
    
    all_result =[]
    #all_result.append(D2D_result)
    #all_result.append(S2S_result)
    all_result.append(SD2SD_result)
    all_result.append(OldReportsIssueKey)
    all_result.append(newReportIssueKey)

    if len(SD2SD_result) == len(OldReportsIssueKey):
        result_dict = {}
        for i in range(len(OldReportsIssueKey)):
            
            result_dict[ OldReportsIssueKey[i] ]=SD2SD_result[i]
        result_list = sorted(result_dict.iteritems(), key = lambda asd:asd[1], reverse = True)
        

        k = len(result_list)  #因为没有个数的限制啊，本来k=15
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
                        file_num_dict[file0] = file_num_dict[file0] + result_dict[ key ]/issuekey_file_num[key]
                    else:
                        file_num_dict[file0] = result_dict[ key ]/issuekey_file_num[key]
        
        """    
        file_scores_dict ={}###为该词的结果
        for key in file_num_dict:
            file_scores_dict[key] = file_num_dict[key]/float(k)
         """   
        #Result_dict[newReportIssueKey] = file_scores_dict
    return {newReportIssueKey:file_num_dict}    #其中  file_num_dict 为file_scores_dict ，因为已被改过
 

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


    

All_newreportinfo = []
oldreportsInfo = []
issuekey_file_num = {}
issuekey_file_list = {}
Result_dict = {}

def init():
    global All_newreportinfo,oldreportsInfo,issuekey_file_num,issuekey_file_list 
    
    All_newreportinfo = getnewreportInfo()
    oldreportsInfo = getOldreportsInfo()
    issuekey_file_num,issuekey_file_list = getPathinfo()
    

from multiprocessing import freeze_support
from contextlib import closing
#freeze_support()

def main():
    
    Result_dict = {}
    Result_list = []
    #pool = Pool(10)
    #All_newissuekey = []
    for newreportinfo in All_newreportinfo:
        
        Result_list.append( getOldreportsSimilarScores(newreportinfo) )
  
    """
    with closing(Pool(processes=10)) as p:
        Result_list = p.map(getOldreportsSimilarScores,All_newreportinfo)   
    
    """
    """
    pool = Pool(10)
    Result_list = pool.map(getOldreportsSimilarScores,All_newreportinfo)   
    pool.close() # 关闭进程池，表示不能在往进程池中添加进程
    pool.join() # 等待进程池中的所有进程执行完毕，必须在close()之后调用
    """

    for ele in Result_list:
        Result_dict[ ele.keys()[0] ] = ele.values()[0]
    
    print 1
    return Result_dict
    
 

if __name__ == '__main__':
    freeze_support()
    main()

    
    
