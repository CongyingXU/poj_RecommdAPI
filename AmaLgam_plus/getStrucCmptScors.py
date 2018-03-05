#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Wed Aug 16 13:59:09 2017

@author: Congying.Xu
"""
import computeSimilarity
import xlrd
import xlwt
import math
from time import time


Project_name  = 'HadoopHDFS'
#############################################################################
#############################################################################
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

#######
#由于计算量大。所以  把参数略微改动，省去I/O的时间


def computeSimilarityScors(newReportSummary,newReportDescription        ,sheet1):      #,Src_info_file_dir):
   
    all_classdir=[]
    
    all_program_lanuage=[]
    all_comments=[]
    """
    workbook = xlrd.open_workbook(Src_info_file_dir,'r')
    sheet1 = workbook.sheet_by_name('sheet1')
    """
    #sheet2 = workbook.sheet_by_name('comments')
    #print 1
    #print sheet1.cell(6,28).value.encode('utf-8')    
    for i in range(1,sheet1.nrows):
        
        all_classdir.append(sheet1.cell(i,0).value.encode('utf-8'))
        className=[]
        className = sheet1.cell(i,1).value.encode('utf-8').split(' ')
        methodName=[]
        methodName = sheet1.cell(i,2).value.encode('utf-8').split(' ')
        variableName=[]
        variableName = sheet1.cell(i,3).value.encode('utf-8').split(' ')
        program_lanuage = className + methodName  + variableName
        all_program_lanuage.append(program_lanuage)
        
        comments_str=''
        j=6
        while 1 :
            try:#因为不确定  注释占了几格
                comments_str= comments_str + ' ' + sheet1.cell(i,j).value.encode('utf-8')
                j=j+1
            except IndexError:
                break
        comments_str = comments_str.strip(' ')
        comments=[]
        comments = comments_str.split(' ')
        all_comments.append(comments)
        #print i
        
   
    
    #版本二，数据已经经过文本处理，节省时间
    newRportSD= computeSimilarity.tokenize_stopwords_stemmer( [newReportSummary + newReportDescription] )
    #print 11
    
    all_program_language_result = Half_computeSimilarity(newRportSD,all_program_lanuage)
    all_commentsSD_result = Half_computeSimilarity(newRportSD,all_comments)
    #归一化处理
    Max_socre  = all_program_language_result[0]
    for score in all_program_language_result:
        if Max_socre < score and score < 1 :#里面有一个是其本身。最大值为一
            Max_socre = score
    if Max_socre != 0:
        for i in range(len(all_program_language_result)):
            all_program_language_result[i] = all_program_language_result[i] / Max_socre
    
    Max_socre  = all_commentsSD_result[0]
    for score in all_commentsSD_result:
        if Max_socre < score and score < 1 :#里面有一个是其本身。最大值为一
            Max_socre = score
    if Max_socre != 0:
        for i in range(len(all_commentsSD_result)):
            all_commentsSD_result[i] = all_commentsSD_result[i] / Max_socre
            
    result=[]
    result.append(all_classdir)
    result.append(all_program_language_result)
    result.append(all_commentsSD_result)
    
    return result

    #做整体计算
def getall_result():
    #begin = time()
    
    workbook = xlrd.open_workbook(r'Input/' + Project_name +'.xlsx')
    sheet = workbook.sheet_by_name('general_report')
    #print sheet1.cell(6,28).value.encode('utf-8')
    
    
    #定向制作
    Src_info_file_dir='Output/' + Project_name +'_repo_SrcfileInfo.xls'
    workbook = xlrd.open_workbook(Src_info_file_dir,'r')
    sheet1 = workbook.sheet_by_name('sheet1')
    #因为数据量小，所以代码没有优化，
    #正式使用前，一定会要  优化！！！！
    ######################################
    All_result={}
    data = range(4,1004)# + range(904,1004)
    for i in data:
        newReportissueKey=sheet.cell(i,1).value.encode('utf-8')
        newReportSummary=sheet.cell(i,2).value.encode('utf-8')
        newReportDescription=sheet.cell(i,28).value.encode('utf-8')
        
        #Src_info_file_dir='Output/repo_SrcfileInfo.xls'
        result= computeSimilarityScors(newReportSummary,newReportDescription,    sheet1)     #Src_info_file_dir)
        #result_file_dir='Output/StructComponentScrs.xls'
        #weights=[1.0 ,1.0 ,1.0 ,1.0 ,1.0 ,1.0 ,1.0 ,1.0 ]
        #all_result = writeResult(result,result_file_dir,weights)#排好序
        All_result[newReportissueKey] = result
    
    #end = time()
    #print "Total procesing time: %d seconds" % (end - begin)
    return All_result

def getFinal_result(All_result):
    weights = [1 , 1]
    #默认情况下，不需要调参数 weights = [0.5 ， 0.5 ]
    #转为实验设计
    Final_result = {}
    for key in All_result:
        result = All_result[key]
        all_result={}
        for i in range(len(result[0])):
            all_result0 = weights[0]*result[1][i] +weights[1]*result[2][i] 
            all_result[result[0][i]] = all_result0
        #all_result = sorted(all_result.iteritems(), key = lambda asd:asd[1], reverse = True)
        Final_result[key]= all_result
    return Final_result #{ issuekey:[(.java , 分数) 。。。。。  ] }

def init():
    pass

def main( ):
    begin = time()
    print begin
    All_result=getall_result()
    Final_result = getFinal_result(All_result )
    
    print  time() - begin
    return Final_result



if __name__=='__main__':
    main()
