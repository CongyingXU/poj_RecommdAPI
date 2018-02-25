#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Sat Aug 12 14:33:35 2017

@author: Congying.Xu
"""

import os
import xlrd
from aip import AipNlp
import csv

""" 你的 APPID AK SK """
APP_ID = '10493871'
API_KEY = '97AMli67i7a5GlWeFuPRNR91'
SECRET_KEY = 'yMaiiCKUB8G4bImZC263wRYUTiVrun1y '

client = AipNlp(APP_ID, API_KEY, SECRET_KEY)
model_dict = {'model':'CRNN'}


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
    allAPI_info_dir = 'Input/allAPI_info0.xls'
    workbook = xlrd.open_workbook(allAPI_info_dir,'r')
    sheet2 = workbook.sheet_by_name('sheet1')
    
    allAPI_info_list=[]
    for i in range(sheet2.nrows):
        API=(sheet2.cell(i,0).value,#原
             sheet2.cell(i,1).value,#原
             sheet2.cell(i,2).value,#进过文本预处理的地方，将单词分开
             sheet2.cell(i,3).value,
             sheet2.cell(i,4).value,
             sheet2.cell(i,5).value)
        allAPI_info_list.append(API)
    All_3partAPIinfo_list = allAPI_info_list
    return All_3partAPIinfo_list
    
def computeSimilarityScors(issuekey,issuekey_API_scores_dict):      
    if issuekey_API_scores_dict.has_key(issuekey) :
        return issuekey_API_scores_dict[issuekey]
    else:
        print issuekey


def get_issuekey_API_scores():
    issuekey_API_scores_dict = {}
    API_name = []
    with open("Input/CXF_description_Scores.csv","r") as csvfile:
            reader = csv.reader(csvfile)
            #这里不需要readlines
            print reader
            for i,rows in enumerate(reader):
                if i<1 :
                    API_name = rows[1:7366+1]
                else:
                    API_scores_dict = {}
                    scores = rows[1:7366+1]
                     
                    for j in range(len(scores)):
                        API_scores_dict[ API_name[j] ] = float(scores[j])
                    try:
                        issuekey_API_scores_dict[rows[0]] = API_scores_dict
                    except IndexError:
                        pass
                
    return issuekey_API_scores_dict
    
num_list = range(904,1004)# +range(904,1004)
def main():#即issuekey的行号【4:1004】 是全部
    issuekey_API_scores_dict = get_issuekey_API_scores()
    workbook = xlrd.open_workbook(r'Input/CXF.xlsx')
    sheet = workbook.sheet_by_name('general_report')
    
    Scores_dict={}
    for i in num_list:
        issuekey = sheet.cell(i,1).value.encode('utf-8')
        #newReportSummary=sheet.cell(i,2).value.encode('utf-8')
        #newReportDescription=sheet.cell(i,28).value.encode('utf-8')
        Scores = computeSimilarityScors(issuekey,issuekey_API_scores_dict)
        Scores_dict[issuekey] = Scores
    return Scores_dict


if __name__=='__main__':
    issuekey_API_scores_dict = get_issuekey_API_scores()
    main()
    print 1
    