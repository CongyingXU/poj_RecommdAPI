#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Sat Aug 12 14:33:35 2017

@author: Congying.Xu
"""

import computeSimilarity
import os
import xlrd
import xlwt
import math
from time import time

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

def getAPI_Info():
    dir = 'Input/APIdoc'
    All_3partAPIinfo_list=[]#格式：  【  （ class_name，method_name，para_info，method_description，modifier_type  ） ，  】
    for dirpath,dirname,filename in os.walk(dir):
        for each_file in filename:
            if each_file.endswith(".xls"):
                #if 'test' not in dirpath:
                    tmp_path=os.path.join(dirpath,each_file)
                    workbook = xlrd.open_workbook(tmp_path,'r')
                    sheet = workbook.sheet_by_name('sheet1')
                    for i in range(1,sheet.nrows):
                        API=(sheet.cell(i,0).value,
                             sheet.cell(i,1).value,
                             sheet.cell(i,2).value,
                             sheet.cell(i,3).value,
                             sheet.cell(i,4).value)
                        All_3partAPIinfo_list.append(API)
                       
    #print  All_3partAPI_set.__len__()
    #print  All_3partAPI_set.union()                
    return All_3partAPIinfo_list

def getAPI_Info_Txtprocessing():#进过文本预处理的API信息，（目前只对description进行预处理）
                                #二次升级  对class_name、method———name、para——name  也进行预处理，放到最后把
    """
    dir = 'Input/APIdoc'
    All_3partAPIinfo_list=[]#格式：  【  （ class_name，method_name，para_info，method_description，modifier_type  ） ，  】
    for dirpath,dirname,filename in os.walk(dir):
        for each_file in filename:
            if each_file.endswith(".xls"):
                #if 'test' not in dirpath:
                    tmp_path=os.path.join(dirpath,each_file)
                    workbook = xlrd.open_workbook(tmp_path,'r')
                    sheet = workbook.sheet_by_name('sheet1')
                    for i in range(1,sheet.nrows):
                        class_name =  sheet.cell(i,0).value
                        class_name_after_txtprces = computeSimilarity.tokenize_stopwords_stemmer([class_name])
                        methond_name =  sheet.cell(i,1).value
                        methond_name_after_txtprces = computeSimilarity.tokenize_stopwords_stemmer([methond_name])
                        para_name =  sheet.cell(i,2).value
                        para_name_after_txtprces = computeSimilarity.tokenize_stopwords_stemmer([para_name])
                        description =  sheet.cell(i,3).value
                        description_after_txtprces = computeSimilarity.tokenize_stopwords_stemmer([description])
            
                        API=(sheet.cell(i,0).value,#原
                             sheet.cell(i,1).value,#原
                             class_name_after_txtprces,
                             methond_name_after_txtprces,
                             para_name_after_txtprces,
                             description_after_txtprces,
                             sheet.cell(i,2).value,#原
                             sheet.cell(i,4).value)#原)
                        All_3partAPIinfo_list.append(API)
                       
    #print  All_3partAPI_set.__len__()
    #print  All_3partAPI_set.union()
    f = xlwt.Workbook() #创建工作簿
    sheet1 = f.add_sheet(u'sheet1',cell_overwrite_ok=True) #创建sheet
    #sheet2 = f.add_sheet(u'comments',cell_overwrite_ok=True) #创建sheet
    sheet1.write(0,0,"class_name_origin".decode('utf-8'))
    sheet1.write(0,1,"methond_name_origin".decode('utf-8'))
    sheet1.write(0,2,"class_name".decode('utf-8'))
    sheet1.write(0,3,"methond_name".decode('utf-8'))
    sheet1.write(0,4,"para_name".decode('utf-8'))
    sheet1.write(0,5,"description".decode('utf-8'))
    
    sheet1.write(0,6,"para_info_origin".decode('utf-8'))
    sheet1.write(0,7,"modifier_type_origin".decode('utf-8'))
         
    for i in range(len(All_3partAPIinfo_list)):
            sheet1.write(i+1,0,All_3partAPIinfo_list[i][0])
            sheet1.write(i+1,1,All_3partAPIinfo_list[i][1])
            
            class_name = ' '
            if len(All_3partAPIinfo_list[i][2]) >0:
                for word in All_3partAPIinfo_list[i][2]:
                    class_name = class_name + ' ' + word
            sheet1.write(i+1,2,class_name.strip(' '))
            
            methond_name = ' '
            if len(All_3partAPIinfo_list[i][3]) >0:
                for word in All_3partAPIinfo_list[i][3]:
                    methond_name = methond_name + ' ' + word
            sheet1.write(i+1,3,methond_name.strip(' '))
            
            para_name = ' '
            if len(All_3partAPIinfo_list[i][4]) >0:
                for word in All_3partAPIinfo_list[i][4]:
                    para_name = para_name + ' ' + word
            sheet1.write(i+1,4,para_name.strip(' '))

            description = ' '
            if len(All_3partAPIinfo_list[i][5]) >0:
                for word in All_3partAPIinfo_list[i][5]:
                    description = description + ' ' + word
            sheet1.write(i+1,5,description.strip(' '))
            
            sheet1.write(i+1,6,All_3partAPIinfo_list[i][6])
            sheet1.write(i+1,7,All_3partAPIinfo_list[i][7])
     
    f.save('Input/allAPI_info0.xls')
    """
    allAPI_info_dir = 'Input/allAPI_info.xls'
    workbook = xlrd.open_workbook(allAPI_info_dir,'r')
    sheet2 = workbook.sheet_by_name('sheet1')
    
    allAPI_info_list=[]
    for i in range(sheet2.nrows):
        API=(sheet2.cell(i,0).value,#原
             sheet2.cell(i,1).value,#原
             sheet2.cell(i,2).value.split(' '),#进过文本预处理的地方，将单词分开
             sheet2.cell(i,3).value.split(' '),
             sheet2.cell(i,4).value.split(' '),
             sheet2.cell(i,5).value.split(' '))
        allAPI_info_list.append(API)
    All_3partAPIinfo_list = allAPI_info_list
    return All_3partAPIinfo_list
    
def computeSimilarityScors(newReportSummary, newReportDescription , All_3partAPIinfo_list):      #,Src_info_file_dir):
    all_APIdescription = []
    for ele in All_3partAPIinfo_list:
        all_APIdescription.append(ele[5])
    #版本二，数据已经经过文本处理，节省时间
    newRportSD= computeSimilarity.tokenize_stopwords_stemmer( [newReportSummary,newReportDescription] )
    scores = Half_computeSimilarity(newRportSD,all_APIdescription)
    
    #print time(),44
    Scores={}
    for i  in range(len(All_3partAPIinfo_list)):
        API = All_3partAPIinfo_list[i][0]+ '.' + All_3partAPIinfo_list[i][1]
        Scores[ API ] = scores[i]
        
    """
    Scores = sorted(Scores.iteritems(), key = lambda asd:asd[1], reverse = True)
    return Scores # [(key,value)]
    """
    return Scores   #直接用字典，这样便于后续的关键字查找

num_list = range(104,1004)
def main():#即issuekey的行号【4:1004】 是全部

    workbook = xlrd.open_workbook(r'Input/Hbase.xlsx')
    sheet = workbook.sheet_by_name('general_report')
    #print sheet1.cell(6,28).value.encode('utf-8')
    Scores_dict={}
    All_3partAPIinfo_list = getAPI_Info_Txtprocessing()
    ###########################################################
    #调节范围
    for i in num_list:
        issuekey = sheet.cell(i,1).value.encode('utf-8')
        newReportSummary=sheet.cell(i,2).value.encode('utf-8')
        newReportDescription=sheet.cell(i,28).value.encode('utf-8')
        Scores = computeSimilarityScors(newReportSummary, newReportDescription, All_3partAPIinfo_list)
        Scores_dict[issuekey] = Scores
    return Scores_dict


#if __name__=='__main__':
#    print main()