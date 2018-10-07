#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Fri Dec  8 19:03:48 2017

@author: Congying.Xu
MULAPI+的部分，加入了对所推荐的API参数的考虑
"""

import xlrd
import csv
import time

from aip import AipNlp

""" 你的 APPID AK SK """
APP_ID = '10493871'
API_KEY = '97AMli67i7a5GlWeFuPRNR91'
SECRET_KEY = 'yMaiiCKUB8G4bImZC263wRYUTiVrun1y '
client = AipNlp(APP_ID, API_KEY, SECRET_KEY)

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
            if j>6:#目前提取6个相关的源文件
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
            #RelatedSrcfileinfo_dict[class_dir] = [sheet1.cell(row,1).value.split(' ') + sheet1.cell(row,2).value.split(' ') +sheet1.cell(row,3).value.split(' '),
            #                       sheet1.cell(row,6).value.split(' ') ]
            RelatedSrcfileinfo_dict[class_dir] = sheet1.cell(row,5).value.split(';')   #变量信息
            
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
             sheet2.cell(i,5).value.split(' '),
             #sheet2.cell(i,6).value# 形式参数信息     ‘参数类型 空格 参数名称’
             )
        allAPI_info_list.append(API)
    
    APIinfo_list = [] 
    parainfo_list = []
    for API in allAPI_info_list:#API是元组 形式如上
        APIinfo_list.append( API[2]+ API[3]+ API[4]+ API[5] )
        #APIinfo_list.append( API[2]+ API[3]+ API[4]+ API[5] )
        parainfo_list.append( API[4] )
    return allAPI_info_list,APIinfo_list,parainfo_list

def computeSimilarScores( RelatedSrcfileinfo_dict , parainfo_list ):
    #text0_list:类名 方法名 参数名    因为相似度计算是 列表存的words
    #text1_list:注释
    #text3_list：变量信息，【  ‘变量类型，变量名’  ，  ，  】
    text3_list = []
    for key in RelatedSrcfileinfo_dict:
        text3_list = text3_list + RelatedSrcfileinfo_dict[key] 
         
    para_result = 0 
    max_para_result = 0
    variable_type = ''
    variable_name = ''
    para_type = ''
    para_name = ''
    print '变量个数',len(text3_list)
    
    for ele in text3_list:# 依次对比每一组变量信息
        
        if len(ele.split(',')) == 2:#即有至少一个变量信息
            variable_type = ele.split(',')[0]
            variable_name = ele.split(',')[1]
        else:
            continue
        
        num = 0
        for para in parainfo_list:
            print para
            if len(para)<2:#api 无参数
                continue
            
            parainfo = para
            i =0 
            while(2*i + 1<=len(parainfo)/2):#API 的形式参数  不止一个
                para_type = parainfo[2*i]
                para_name = parainfo[2*i + 1]
                
                print para_name, variable_name
                reponse = client.wordSimEmbedding(para_name, variable_name)
                print reponse
                if reponse.has_key('score'):
                    para_result = reponse['score']*0.5
                    print num,reponse['score']
                else:
                    para_result = 0
                    
                if para_type == variable_type:#参数部分分数
                    para_result = 0.5 + para_result
                 
                if max_para_result < para_result:#记录最高分
                    max_para_result = para_result
                    
                i = i+1
                
            num = num +1
            #print num

    return max_para_result


#这样零散得放置便于调参数


sheet0,sheet1,issuekeyOrder_dict = Info_IO()
allAPI_info_list,APIinfo_list,parainfo_list = get_allAPI_info()
workbook = xlrd.open_workbook(r'Input/HadoopCommon.xlsx')
sheet = workbook.sheet_by_name('general_report')

###########################################################
#调节范围
num_list = range(4,5)

text3_result_dict={}
paraType_result_dict={}
paraSimiar_result_dict={}

print 1
for i in num_list:
    issuekey = sheet.cell(i,1).value.encode('utf-8') 
    RelatedSrcfile_list = getRelatedSrcfile(issuekey ,sheet0,issuekeyOrder_dict)
    print 2
    RelatedSrcfileinfo_dict = getRelatedSrcfile_info(RelatedSrcfile_list ,sheet1 )
    print 3
    para_result = computeSimilarScores( RelatedSrcfileinfo_dict , parainfo_list )
    text3_result_dict[issuekey] = para_result
    print issuekey,i
    
    
#因为fixedfile_result中，很多超过256个  所以用CSV存储
with open("Input/Hadoop_API_Src_Scores3.csv","w") as csvfile:
    writer = csv.writer(csvfile)
    for k,v in text3_result_dict.items():
        writer.writerow([k]+[v])



        



#if __name__=='__main__':
#    print main(issuekey , weights)
