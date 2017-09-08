#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Wed Aug  9 14:56:42 2017

@author: Congying.Xu
"""

"""
数据结构
Result={issurKey :[ [ 修改的类名, ...] , [使用的API]  ]       
        .........
        .........
                    }

[使用的API]：（用一个字符串存储，中间 ； 号分割开）
"""

import xlrd
import xlwt
import csv
import re
import collections
import os

def getSrcInfo():
    Src_info_file_dir='Output/repo_SrcfileInfo.xls'
    workbook = xlrd.open_workbook(Src_info_file_dir,'r')
    sheet1 = workbook.sheet_by_name('sheet1')
    
    #格式：{key classdir: value variableInfo}
    #variableInfo:[ '变量类型，变量名' , '变量类型，变量名' ,  ,  ]
    ##variableInfo:{ '变量名' : '变量类型'   }
    SrcInfo_dict={}

    
    for i in range(1,sheet1.nrows):
        
        classdir=sheet1.cell(i,0).value.encode('utf-8')
        variableInfo_dict={}
        variableInfo=[]
        variableInfo = sheet1.cell(i,5).value.encode('utf-8').split(';')
        for v in variableInfo:
            index=v.find(',')
            variableInfo_dict[ v[index+1:] ]=v[:index]
        
        SrcInfo_dict[classdir] = variableInfo_dict
        #print i
    return  SrcInfo_dict  

SrcInfo_dict = getSrcInfo()

#汇总出所有的第三方API
def getAll_3partAPI():
    dir = '/Users/apple/Documents/API推荐项目/APIdoc'
    All_3partAPI_set=set()
    for dirpath,dirname,filename in os.walk(dir):
        for each_file in filename:
            if each_file.endswith(".xls"):
                #if 'test' not in dirpath:
                    tmp_path=os.path.join(dirpath,each_file)
                    workbook = xlrd.open_workbook(tmp_path,'r')
                    sheet = workbook.sheet_by_name('sheet1')
                    for i in range(1,sheet.nrows):
                        API_short = sheet.cell(i,0).value+'.'+sheet.cell(i,1).value
                        API_short = API_short.split('.')[-2] +'.'+ API_short.split('.')[-1]
                        All_3partAPI_set.add( API_short )
                       
    #print  All_3partAPI_set.__len__()
    #print  All_3partAPI_set.union()                
    return All_3partAPI_set

All_3partAPI_set = getAll_3partAPI()

def getfixedfile():
    workbook = xlrd.open_workbook(r'Input/Hbase.xlsx')
    sheet = workbook.sheet_by_name('general_report')
    
    Result=collections.OrderedDict()
    for j in range(4,1004):
        issuekey=sheet.cell(j,1).value.encode('utf-8')
        
        #print issuekey
        
        class_result=[]
        x=0
        while 1:
            filename='Input/hbase-attachments/'+issuekey+'_'+str(x)+'.patch'
            x = x + 1
            try:
                #获取patch中设计的源代码文件
                
                with open(filename ,'r') as f:
                    list_of_all_the_lines = f.readlines( )
                    for line in list_of_all_the_lines:#文本文件时
                        if line[:3]=='+++':#提取所有修改的类
                            class_name = line.split(' ')[1].strip('\n').strip('b/') 
                            class_name = 'hbase-1-master/'+class_name
                            if not class_name in class_result and class_name.endswith(".java"): 
                                class_result.append(class_name)
                                
            except IOError:
                break
        
        #print j
        #Result.update[issuekey] = [class_result,API_result]
        Result[issuekey] = [class_result]    #格式：key:[[ .java, , ,]  ]
        
    return Result

        
def getAPIFromSrcfile(pre_result_dict,SrcInfo_dict,file_name):
    Result_list=[]#(类名，方法名，【参数】)
    for key in pre_result_dict:
        try:
            if SrcInfo_dict[file_name].has_key(pre_result_dict[key][0]):
                Result_list.append(( SrcInfo_dict[file_name][pre_result_dict[key][0]] , 
                                    pre_result_dict[key][1] , 
                                    pre_result_dict[key][2]))
        except KeyError: 
            #print file_name,'KeyError'
            pass
    
    return Result_list
    
def getUsedAPI(fixedfile_result):
    workbook = xlrd.open_workbook(r'Input/Hbase.xlsx')
    sheet = workbook.sheet_by_name('general_report')
    
    Result=collections.OrderedDict()
    for j in range(4,1004):
        issuekey=sheet.cell(j,1).value.encode('utf-8')
        
        #print issuekey
        
        class_result=[]
        API_result=[ ]
        add_line =''
        Add_line={}
        x=0
        while 1:
            filename='Input/hbase-attachments/'+issuekey+'_'+str(x)+'.patch'
            x = x + 1
            
            if len(fixedfile_result[issuekey][0])==0:#无fixed file时
                #print issuekey,'None .java'
                break
            
            try:
                #获取patch中设计的源代码文件
                with open(filename ,'r') as f:
                    list_of_all_the_lines = f.readlines( )
                    for line in list_of_all_the_lines:#文本文件时
                        if line[:3]=='+++':#提取所有修改的类
                            class_name = line.split(' ')[1].strip('\n').strip('b/') 
                            class_name = 'hbase-1-master/'+class_name
                            if not class_name in class_result and class_name.endswith(".java"): 
                                class_result.append(class_name)
                                Add_line[class_name]=''
                                
                                
                            if class_name.endswith(".java"):    
                                Add_line[class_name] = Add_line[class_name] + add_line
                                add_line=''
                        elif line[:2]=='+ ':#提取+的内容
                            add_line = add_line + line.strip('\n').strip(' ')       
            except IOError:
                break
        #print Add_line.__len__()
        #print j
        for key in Add_line:
            API_result = API_result + getAPI( Add_line[key], key , issuekey)
        #Result.update[issuekey] = [class_result,API_result]
        Result[issuekey] = [API_result]
        
    return Result    

"""
只考虑最简单的情况：A.a(b1 ,b2 )这种，参数中 如含有字符串，当作空处理

返回值为列表形式
"""
def getAPI(add_line,file_name,issuekey):
    list0=add_line.split('+')
    listt=[]
    for s in list0:
        s=s.strip(' ')
        if len(s)<5 or s[0]=='*'or s[0]=='@' or s[:2]=='//' or s[:2]=='if'or s[-1]=='{' :
            pass
        else:
            listt.append(s)
    list0=listt
    
    string=''
    for string0 in list0:
        string = string +string0.strip(' ')
        
    list1=string.split(';')
    
    list2=[]
    for str0 in list1:
        if str0.find('new')>-1:
            str1 = str0[ str0.find('new')+3 :]
        elif str0.find('return')>-1:
            str1 = str0[ str0.find('return')+6 :]
        else:
            str1 = str0
            
        list2 = list2 + str1.split('=')
        
    """
    得到粗糙的划分，如下
    sf.getReader().getMaxTimestamp()'
    out.println("RegionServer status for "hrs.getServerName()out.println("\\n\\nVersion Info:")'
    Long.parseLong(fileName.substring(0, fileName.length()
    """
    
    pre_result_dict ={}
    i=0
    #p1='[a-zA-Z0-9_]+\.[a-zA-Z0-9_]+\([,\'a-zA-Z0-9_\"\s\)]+'
    p1='[a-zA-Z0-9_]+\.[a-zA-Z0-9_]+\(.+\)'
    pa1=re.compile(p1)
    for line in list2:
        API_lines = pa1.findall(line)
        if len(API_lines)>0:
            for API_line in API_lines: #API_line为字符串形式,满足条件的 含有method的语句
                index0 = API_line.find('.')
                index1 = API_line.find('(')
                index2 = API_line.find(')')
                
                odj_name = API_line[:index0]
                mtd_name = API_line[index0+1:index1]
                para = []
                
                
                #参数为空时，para = ['']  表示
                #参数为字符串时,或有其他方法嵌套时
                if API_line.find('\'')>-1 or API_line.find('\"')>-1 or API_line[index1+1].find('(')>-1:
                    para = ['']
                elif API_line[-1]==')':
                    para = API_line[index1+1:-1].split(',')
                #有一个问题： sf.getReader(hjj).getMaxTimestamp()'    参数hjj，提取不出来
                
                pre_result_dict[API_line[:index1] ]= (API_line[:index0] ,API_line[index0+1:index1] ,para )
                

    middle_APIresult_list=[]
    middle_APIresult_list = getAPIFromSrcfile(pre_result_dict,SrcInfo_dict,file_name)
    
    Used_API=[]#[ (类名 , 方法名,【参数】 ) .]
    for ele in middle_APIresult_list:
        if ele[0]+ '.' + ele[1] in All_3partAPI_set:
            Used_API.append( (ele[0], ele[1] , ele[2] ) )
            print (ele[0], ele[1] , ele[2] )
        
    return Used_API
    
def write():
    pass
 
def main():
    fixedfile_result = getfixedfile()
    Used_API = getUsedAPI(fixedfile_result)
    #print Used_API
    
    """
    with open("Input/Attachments_PatchInfo.csv","w") as csvfile:
        writer = csv.writer(csvfile)
        for k,v in Result.items():
            writer.writerow([k]+v[0])
        
      
    
    import csv

    #python2可以用file替代open
    with open("test.csv","w") as csvfile: 
    writer = csv.writer(csvfile)

    #先写入columns_name
    writer.writerow(["index","a_name","b_name"])
    #写入多行用writerows
    writer.writerows([[0,1,3],[1,2,3],[2,3,4]])
    """

main()