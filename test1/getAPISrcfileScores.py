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
    dir0 = 'Output/FeaturnLocation_result.xls' 
    workbook0 = xlrd.open_workbook(dir0,'r')
    sheet0 = workbook0.sheet_by_name('sheet1')

    Srcfileinfo_dir = 'Output/repo_SrcfileInfo.xls'
    workbook1 = xlrd.open_workbook(Srcfileinfo_dir,'r')
    sheet1 = workbook1.sheet_by_name('sheet1')
    
    return sheet0,sheet1

def getRelatedSrcfile(issuekey ,sheet0):
    #从feation location中，得到结果
    RelatedSrcfile_list= []
    for i in range(2,sheet0.nrows):
        if sheet0.cell(i,0).value == issuekey:
            j=1
            while 1:
                try:
                    RelatedSrcfile_list.append( sheet0.cell(i,j).value )
                    j = j+1
                    ############################
                    ############################
                    if j>11:#目前提取10个相关的源文件
                        break
                except IOError:
                    break
            break
    return RelatedSrcfile_list
    #[ 源文件名, , , ]


def getRelatedSrcfile_info(RelatedSrcfile_list ,sheet1 ):
    RelatedSrcfileinfo_dict={}
    for row in (1,sheet1.nrows):
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
    for i in range(len(sheet2.nrows)):
        API=(sheet2.cell(i,0).value,#原
             sheet2.cell(i,1).value,#原
             sheet2.cell(i,2).value.split(' '),#进过文本预处理的地方，将单词分开
             sheet2.cell(i,3).value.split(' '),
             sheet2.cell(i,4).value.split(' '),
             sheet2.cell(i,5).value.split(' '))
    allAPI_info_list.append(API)
    return allAPI_info_list

def computeSimilarScores( RelatedSrcfileinfo_dict , allAPI_info_list ):
    #text0_list:类名 方法名 参数名    因为相似度计算是 列表存的words
    #text1_list:注释
    text0_list = []
    text1_list = []
    for key in RelatedSrcfileinfo_dict:
        text0_list = text0_list + RelatedSrcfileinfo_dict[key][0] 
        text1_list = text1_list + RelatedSrcfileinfo_dict[key][1] 
    
    APIinfo_list = []
    for API in allAPI_info_list:#API是元组 形式如上
        APIinfo_list.append( API[2]+ API[3]+ API[4]+ API[5] )
    text0_result = getStrucCmptScors.Half_computeSimilarity(text0_list,APIinfo_list)
    text1_result = getStrucCmptScors.Half_computeSimilarity(text1_list,APIinfo_list)
    
    return APIinfo_list,text0_result,text1_result
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
def computeFinal_SimilarScores(weights):
    SimilarScores_dict = {}
    for i in range(len(APIinfo_list)):
        API = APIinfo_list[i][0]  + '.' + APIinfo_list[i][1]
        score = text0_result[i]*weights[0] + text1_result[i]*weights[1]
        if SimilarScores_dict.has_key(API) and  SimilarScores_dict[API] >= score:
           pass
        else:
           SimilarScores_dict[API] = score
          
    #SimilarScores = sorted(SimilarScores_dict.iteritems(), key = lambda asd:asd[1], reverse = True)
    return SimilarScores_dict#列表类型，【 （key，value） 】


#这样零散得放置便于调参数


sheet0,sheet1 = Info_IO()
allAPI_info_list = get_allAPI_info()
workbook = xlrd.open_workbook(r'Input/Hbase.xlsx')
sheet = workbook.sheet_by_name('general_report')
###########################################################
#调节范围
num_list = range(104,1004)

for i in num_list:
    issuekey = sheet.cell(i,1).value.encode('utf-8') 
    RelatedSrcfile_list = getRelatedSrcfile(issuekey ,sheet0)
    RelatedSrcfileinfo_dict = getRelatedSrcfile_info(RelatedSrcfile_list ,sheet1 )

APIinfo_list,text0_result,text1_result = computeSimilarScores( RelatedSrcfileinfo_dict , allAPI_info_list )

def main( weights):
    ALL_SimilarScores_dict = {}
    SimilarScores_dict = computeFinal_SimilarScores(weights)
    ALL_SimilarScores_dict[issuekey] =  SimilarScores_dict
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
