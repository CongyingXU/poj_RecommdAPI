#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Mon Sep 11 14:15:43 2017

@author: Congying.Xu
"""

"""
将特征定位到的  .java源文件中的信息抽取出来
与API的一些信息  类名、方法名、参数名 APIdescription 进行 相似度计算
1、源文件的类名 方法名 变量名
2、注释信息
分两大块处理，因为文字量悬殊太大
"""
import xlrd
import csv
import getStrucCmptScors
#其中  text1以及 basic_texts都是经过tokenize_stopwords_stemmer(texts)处理的
#此输入为tokenize_stopwords_stemmer(texts):的输出
#def Half_computeSimilarity(text1, all_reports_tokens):
#text1:[“。。。”]  basic_texts:【 [“  ” , "  " , "  " ] , 
#                                 ......
#                                [ "  ","  ","  " ] 】
def Info_IO():
    ##########################################
    #注意文件名   序号的修改
    dir0 = 'Output/Hadoop_FeaturnLocation_result.xls' 
    workbook0 = xlrd.open_workbook(dir0,'r')
    sheet0 = workbook0.sheet_by_name('sheet1')
    issuekeyOrder_dict = {}
    for i in range(sheet0.nrows):
        issuekeyOrder_dict[sheet0.cell(i,0).value] = i
    

    Srcfileinfo_dir = 'Output/Hadoop_repo_SrcfileInfo.xls'
    workbook1 = xlrd.open_workbook(Srcfileinfo_dir,'r')
    sheet1 = workbook1.sheet_by_name('sheet1')
    
    return sheet0,sheet1,issuekeyOrder_dict

def getRelatedSrcfile(issuekey ,sheet0,issuekeyOrder_dict):
    #从feation location中，得到结果
    RelatedSrcfile_list= []
    row = issuekeyOrder_dict[issuekey]#找到  issuekey对应的行号
    j=1
    while 1:
        try:
            RelatedSrcfile_list.append( sheet0.cell(row,j).value )
            j = j+1
            ############################
            ############################
            if j>11:#目前提取10个相关的源文件
                break
        except IOError:
            break
          
    return RelatedSrcfile_list
    #[ 源文件名, , , ]


def getRelatedSrcfile_info(RelatedSrcfile_list ,sheet1 ):
    RelatedSrcfileinfo_dict={}
    for row in range(1,sheet1.nrows):
        class_dir = sheet1.cell(row,0).value
        if class_dir in RelatedSrcfile_list:
            RelatedSrcfileinfo_dict[class_dir] = [sheet1.cell(row,1).value.split(' ') + sheet1.cell(row,2).value.split(' ') +sheet1.cell(row,3).value.split(' '),
                                   sheet1.cell(row,6).value.split(' ') ]
    return RelatedSrcfileinfo_dict
    #{'文件名':【类名等，注释信息】}

def get_allAPI_info():
    allAPI_info_dir = 'Input/allAPI_info.xls'
    workbook = xlrd.open_workbook(allAPI_info_dir,'r')
    sheet2 = workbook.sheet_by_name('sheet1')
    
    allAPI_info_list=[]
    for i in range(1,sheet2.nrows):
        API=(sheet2.cell(i,0).value,#原
             sheet2.cell(i,1).value,#原
             sheet2.cell(i,2).value.split(' '),#进过文本预处理的地方，将单词分开
             sheet2.cell(i,3).value.split(' '),
             sheet2.cell(i,4).value.split(' '),
             sheet2.cell(i,5).value.split(' '))
        allAPI_info_list.append(API)
    
    APIinfo_list = []
    for API in allAPI_info_list:#API是元组 形式如上
        APIinfo_list.append( API[2]+ API[3]+ API[4]+ API[5] )
        #APIinfo_list.append( API[2]+ API[3]+ API[4]+ API[5] )
    return allAPI_info_list,APIinfo_list

def computeSimilarScores( RelatedSrcfileinfo_dict , allAPI_info_list ):
    #text0_list:类名 方法名 参数名    因为相似度计算是 列表存的words
    #text1_list:注释
    text0_list = []
    text1_list = []
    for key in RelatedSrcfileinfo_dict:
        text0_list = text0_list + RelatedSrcfileinfo_dict[key][0] 
        text1_list = text1_list + RelatedSrcfileinfo_dict[key][1] 
    
   
    text0_result = getStrucCmptScors.Half_computeSimilarity(text0_list,APIinfo_list)
    text1_result = getStrucCmptScors.Half_computeSimilarity(text1_list,APIinfo_list)
    
    return text0_result,text1_result
"""
def computeFinal_SimilarScores(weights):
    SimilarScores_dict = {}
    for i in range(len(APIinfo_list)):
        API = APIinfo_list[i][0]  + '.' + APIinfo_list[i][1]
        score = text0_result[i]*weights[0] + text1_result[i]*weights[1]
        if SimilarScores_dict.has_key(API) and  SimilarScores_dict[API] >= score:
           pass
        else:
           SimilarScores_dict[API] = score
          
    SimilarScores = sorted(SimilarScores_dict.iteritems(), key = lambda asd:asd[1], reverse = True)
    return SimilarScores#列表类型，【 （key，value） 】
    #不使用列表的原因，后期直接根据  键找分数
    
"""
def computeFinal_SimilarScores(weights ,issuekey):
    SimilarScores_dict = {}
    text0_result = text0_result_dict[ issuekey]
    text1_result = text1_result_dict[ issuekey]
    
    """
    #将结果归一化处理一下
    Max_socre  = text0_result[0]
    for score in text0_result:
        if Max_socre < score and score < 1 :#里面有一个是其本身。最大值为一
            Max_socre = score
    for i in range(len(text0_result)):
        text0_result[i] = text0_result[i] / Max_socre
        
    Max_socre  = text1_result[0]
    for score in text1_result:
        if Max_socre < score and score < 1 :#里面有一个是其本身。最大值为一
            Max_socre = score
    for i in range(len(text1_result)):
        text1_result[i] = text1_result[i] / Max_socre
    """    
        
    
    
    for i in range(len(allAPI_info_list)):
        API = allAPI_info_list[i][0]  + '.' + allAPI_info_list[i][1]
        try:
            score = text0_result[i]*weights[0] + text1_result[i]*weights[1]
        except IndexError:
            break
        if SimilarScores_dict.has_key(API) and  SimilarScores_dict[API] >= score:
           pass
        else:
           SimilarScores_dict[API] = score
       
    """
    #数据归一化处理
    Max_socre  = 0.0
    for key in SimilarScores_dict:
        if Max_socre < SimilarScores_dict[key] and SimilarScores_dict[key] < 1 :#里面有一个是其本身。最大值为一
            Max_socre = score
    for i in range(len(text1_result)):
         SimilarScores_dict[key] = SimilarScores_dict[key] / Max_socre
    """
          
    #SimilarScores = sorted(SimilarScores_dict.iteritems(), key = lambda asd:asd[1], reverse = True)
    return SimilarScores_dict#列表类型，【 （key，value） 】


#这样零散得放置便于调参数
sheet0,sheet1,issuekeyOrder_dict = Info_IO()
allAPI_info_list,APIinfo_list = get_allAPI_info()

"""
workbook = xlrd.open_workbook(r'Input/Hbase.xlsx')
sheet = workbook.sheet_by_name('general_report')
###########################################################
#调节范围
num_list = range(4,404) + range(504,1004)

text0_result_dict={}
text1_result_dict={}
for i in num_list:
    issuekey = sheet.cell(i,1).value.encode('utf-8') 
    RelatedSrcfile_list = getRelatedSrcfile(issuekey ,sheet0,issuekeyOrder_dict)
    RelatedSrcfileinfo_dict = getRelatedSrcfile_info(RelatedSrcfile_list ,sheet1 )
    text0_result,text1_result = computeSimilarScores( RelatedSrcfileinfo_dict , allAPI_info_list )
    text0_result_dict[issuekey] = text0_result
    text1_result_dict[issuekey] = text1_result
    print issuekey,i
"""

#省省省省
num_list = range(4,1004)# + range(404,1004)
text0_result_dict={}
text1_result_dict={}
with open("Input/Hadoop_API_Src_Scores0.csv","r") as csvfile:
        reader = csv.reader(csvfile)
        #这里不需要readlines
        for i,rows in enumerate(reader):
            if i+4 in num_list:
                scores = rows[1:7366+1]
                for j in range(len(scores)):
                    scores[j] = float(scores[j])
                try:
                    text0_result_dict[rows[0]] = scores
                except IndexError:
                    pass
                
with open("Input/Hadoop_API_Src_Scores1.csv","r") as csvfile:
        reader = csv.reader(csvfile)
        #这里不需要readlines
        for i,rows in enumerate(reader):
            if i+4 in num_list:
                scores = rows[1:7366+1]
                for j in range(len(scores)):
                    scores[j] = float(scores[j])
                try:    
                    text1_result_dict[rows[0]] = scores
                except IndexError:
                    pass
def main( weights):
    
    #默认情况下，weights = [0.5 , 0.5]
    weights = [0.5, 0.5] 
    ALL_SimilarScores_dict = {}
    for key in text0_result_dict:
        issuekey = key
        ALL_SimilarScores_dict[issuekey] = computeFinal_SimilarScores(weights , issuekey)
    #SimilarScores_dict = computeFinal_SimilarScores(weights)
    #ALL_SimilarScores_dict[issuekey] =  SimilarScores_dict
    return ALL_SimilarScores_dict

"""
def main(num_list , weights):
    workbook = xlrd.open_workbook(r'Input/Hbase.xlsx')
    sheet = workbook.sheet_by_name('general_report')
    ###########################################################
    #调节范围
    SimilarScores_dict={}
    for i in num_list:
        issuekey = sheet.cell(i,1).value.encode('utf-8')
        RelatedSrcfile_list = getRelatedSrcfile(issuekey ,sheet0)
        RelatedSrcfileinfo_dict = getRelatedSrcfile_info(RelatedSrcfile_list ,sheet1 )
        SimilarScores = computeSimilarScores( RelatedSrcfileinfo_dict , allAPI_info_list ,weights )
        SimilarScores_dict[issuekey] = SimilarScores
    return SimilarScores_dict
"""


#if __name__=='__main__':
#    print main(issuekey , weights)
