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

poj = 'Axis2'
IssueKey_str = 'AXIS2-'

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
    
    all_className=[]
    all_methodName=[]
    all_variableName=[]
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
        all_className.append(className)
        
        methodName=[]
        methodName = sheet1.cell(i,2).value.encode('utf-8').split(' ')
        all_methodName.append(methodName)

        variableName=[]
        variableName = sheet1.cell(i,3).value.encode('utf-8').split(' ')
        all_variableName.append(variableName)
        
        
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
    newRportS= computeSimilarity.tokenize_stopwords_stemmer( [newReportSummary] )
    newRportD= computeSimilarity.tokenize_stopwords_stemmer( [newReportDescription] )
    #print 11
    
    all_classNameS_result = Half_computeSimilarity(newRportS,all_className)
    all_methodNameS_result = Half_computeSimilarity(newRportS,all_methodName)
    #print 22
    all_variableNameS_result = Half_computeSimilarity(newRportS,all_variableName)
    all_commentsS_result = Half_computeSimilarity(newRportS,all_comments)
    #print 33
        
    all_classNameD_result = Half_computeSimilarity(newRportD,all_className)
    all_methodNameD_result = Half_computeSimilarity(newRportD,all_methodName)
    #print 44
    all_variableNameD_result = Half_computeSimilarity(newRportD,all_variableName)
    all_commentsD_result = Half_computeSimilarity(newRportD,all_comments)
    
    #print 55
    """
    #版本一：数据无文本预处理
    #print sheet1.cell(6,28).value.encode('utf-8')    
    for i in range(1,sheet1.nrows):
        
        all_classdir.append(sheet1.cell(i,0).value.encode('utf-8'))
        
        className=[]
        className.append(sheet1.cell(i,1).value.encode('utf-8'))
        all_className.append(className)
        
        methodName=[]
        methodName.append(sheet1.cell(i,2).value.encode('utf-8'))
        all_methodName.append(methodName)

        variableName=[]
        variableName.append(sheet1.cell(i,3).value.encode('utf-8'))
        all_variableName.append(variableName)
        
        
        comments_str=''
        j=2
        while 1 :
            try:#因为不确定  注释占了几格
                comments_str= comments_str + sheet2.cell(i,j).value.encode('utf-8')
                j=j+1
            except IndexError:
                break
        comments=[]
        comments.append(comments_str)
        all_comments.append(comments)
    
    newRportS=[newReportSummary]
    newRportD=[newReportDescription]
    
    all_classNameS_result = computeSimilarity.all_computeSimilarity(newRportS,all_className)
    all_methodNameS_result = computeSimilarity.all_computeSimilarity(newRportS,all_methodName)
    all_variableNameS_result = computeSimilarity.all_computeSimilarity(newRportS,all_variableName)
    all_commentsS_result = computeSimilarity.all_computeSimilarity(newRportS,all_comments)
        
    all_classNameD_result = computeSimilarity.all_computeSimilarity(newRportD,all_className)
    all_methodNameD_result = computeSimilarity.all_computeSimilarity(newRportD,all_methodName)
    all_variableNameD_result = computeSimilarity.all_computeSimilarity(newRportD,all_variableName)
    all_commentsD_result = computeSimilarity.all_computeSimilarity(newRportD,all_comments)
    """
    
    """
        result =  all_classNameS_result +  all_classNameD_result
        + all_methodNameS_result +  all_methodNameD_result
        + all_variableNameS_result +  all_variableNameD_result
        + all_commentsS_result +  all_commentsD_result
    """

    result=[]
    result.append(all_classdir)
    result.append(all_classNameS_result)
    result.append(all_classNameD_result)
    result.append(all_methodNameS_result)
    result.append(all_methodNameD_result)
    result.append(all_variableNameS_result)
    result.append(all_variableNameD_result)
    result.append(all_commentsS_result)
    result.append(all_commentsD_result)
    
    return result

def writeResult(result,result_file_dir,weights):
    all_result={}
    f = xlwt.Workbook() #创建工作簿
    sheet1 = f.add_sheet(u'sheet1',cell_overwrite_ok=True) #创建sheet
    
    sheet1.write(0,0,'classdir'.encode('utf-8'))
    sheet1.write(0,1,'classNameS_result'.encode('utf-8'))
    sheet1.write(0,2,'classNameD_result'.encode('utf-8'))
    sheet1.write(0,3,'methodNameS_result'.encode('utf-8'))
    sheet1.write(0,4,'methodNameD_result'.encode('utf-8'))
    sheet1.write(0,5,'variableNameS_result'.encode('utf-8'))
    sheet1.write(0,6,'variableNameD_result'.encode('utf-8'))
    sheet1.write(0,7,'commentsS_result'.encode('utf-8'))
    sheet1.write(0,8,'commentsD_result'.encode('utf-8'))
    sheet1.write(0,9,'all_result'.encode('utf-8'))
    
    for i in range(len(result[0])):
        
        all_result0= weights[0]*result[1][i] +weights[1]*result[2][i] +weights[2]*result[3][i] +weights[3]*result[4][i]
        + weights[4]*result[5][i] +weights[5]*result[6][i] +weights[6]*result[7][i] +weights[7]*result[8][i] 
        all_result[result[0][i]] = all_result0
        sheet1.write(i+1,0,result[0][i])#字符串   #表格的第一行开始写。第一列，第二列。。。。
        sheet1.write(i+1,1,result[1][i])
        sheet1.write(i+1,2,result[2][i])
        sheet1.write(i+1,3,result[3][i])
        sheet1.write(i+1,4,result[4][i])
        sheet1.write(i+1,5,result[5][i])
        sheet1.write(i+1,6,result[6][i])
        sheet1.write(i+1,7,result[7][i])
        sheet1.write(i+1,8,result[8][i])
        sheet1.write(i+1,9,all_result0)
        
    f.save(result_file_dir)#保存文件
    #问题！！！不能追加，会把原来的文件覆盖掉！！
    all_result = sorted(all_result.iteritems(), key = lambda asd:asd[1], reverse = True)
    return all_result#列表类型，【 （key，value） 】
      
def getAimList():
    #准备设计成 字典，以issuekey 作为键   ，  其aimresulr  为值
    Aimresult={}
    import csv
    with open('Input/'+ poj+'Attachments_PatchInfo.csv',"r") as csvfile:
        reader = csv.reader(csvfile)
        #这里不需要readlines
        for i,rows in enumerate(reader):
            #if i <20 :
                aimresult = rows[1:]
                Aimresult[rows[0]] = aimresult
            #else:
             #   break
    return Aimresult


    #做整体计算
def getall_result():
    #begin = time()
    
    workbook = xlrd.open_workbook(r'/Users/apple/Desktop/open_issuekey/'+ poj+'.xlsx')
    sheet = workbook.sheet_by_name(poj)
    #print sheet1.cell(6,28).value.encode('utf-8')
    
    
    #定向制作
    Src_info_file_dir='Output/'+ poj+'_repo_SrcfileInfo.xls'
    workbook = xlrd.open_workbook(Src_info_file_dir,'r')
    sheet1 = workbook.sheet_by_name('sheet1')
    #因为数据量小，所以代码没有优化，
    #正式使用前，一定会要  优化！！！！
    ######################################
    All_result={}
    data = range(1,10)# + range(204,1004)
    for i in data:
        newReportissueKey=sheet.cell(i,1).value.encode('utf-8')
        
        if newReportissueKey.find(IssueKey_str)>-1:
            pass
        else:
            continue
        newReportSummary=sheet.cell(i,0).value.encode('utf-8')
        newReportDescription=sheet.cell(i,31).value.encode('utf-8')
        
        #Src_info_file_dir='Output/repo_SrcfileInfo.xls'
        result= computeSimilarityScors(newReportSummary,newReportDescription,    sheet1)     #Src_info_file_dir)
        #result_file_dir='Output/StructComponentScrs.xls'
        #weights=[1.0 ,1.0 ,1.0 ,1.0 ,1.0 ,1.0 ,1.0 ,1.0 ]
        #all_result = writeResult(result,result_file_dir,weights)#排好序
        All_result[newReportissueKey] = result
    
    #end = time()
    #print "Total procesing time: %d seconds" % (end - begin)
    return All_result

def getFinal_result(All_result , weights):
    #转为实验设计
    Final_result = {}
    for key in All_result:
        result = All_result[key]
        all_result={}
        for i in range(len(result[0])):
            all_result0= weights[0]*result[1][i] +weights[1]*result[2][i] +weights[2]*result[3][i] +weights[3]*result[4][i]
            + weights[4]*result[5][i] +weights[5]*result[6][i] +weights[6]*result[7][i] +weights[7]*result[8][i] 
            all_result[result[0][i]] = all_result0
        #all_result = sorted(all_result.iteritems(), key = lambda asd:asd[1], reverse = True)
        Final_result[key]= all_result
    return Final_result #{ issuekey:[(.java , 分数) 。。。。。  ] }



def main( weights):
    All_result=getall_result()
    #weights = [0.5881762643232233, 0.4735409434116893, 0.10212499280443976, 0.4363396810754724, 0.5774016678038669, 0.4166697755914751, 0.18319637300523295, 0.14007257039425824]
    Final_result = getFinal_result(All_result , weights)
    return Final_result
"""
#单个计算时
def main():
    begin = time()
    
    workbook = xlrd.open_workbook(r'Input/Hbase.xlsx')
    sheet = workbook.sheet_by_name('general_report')
    #print sheet1.cell(6,28).value.encode('utf-8')
    newReportSummary=sheet.cell(844,2).value.encode('utf-8')
    newReportDescription=sheet.cell(844,28).value.encode('utf-8')
    
    Src_info_file_dir='Output/SrcInfo2.xls'
    result= computeSimilarityScors(newReportSummary,newReportDescription,Src_info_file_dir)
    result_file_dir='Output/StructComponentScrs.xls'
    weights=[1.0 ,1.0 ,1.0 ,1.0 ,1.0 ,1.0 ,1.0 ,1.0 ]
    all_result = writeResult(result,result_file_dir,weights)#排好序
    
    
    end = time()
    print "Total procesing time: %d seconds" % (end - begin)
    return all_result
"""

"""
if __name__=='__main__':
    main()
"""