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


def getAPI(add_line):
    add_API=''
    
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
    i=0
    #p1='[a-zA-Z0-9_]+\.[a-zA-Z0-9_]+\([,\'a-zA-Z0-9_\"\s\)]+'
    p1='[a-zA-Z0-9_]+\.[a-zA-Z0-9_]+\(.+\)'
    pa1=re.compile(p1)
    for line in list2:
        result = pa1.findall(line)
        if len(result)>0:
            print result,len(result)
            i=i +1
 
def main():
    workbook = xlrd.open_workbook(r'Input/Hbase.xlsx')
    sheet = workbook.sheet_by_name('general_report')
    
    Result=collections.OrderedDict()
    for j in range(4,1004):
        issuekey=sheet.cell(j,1).value.encode('utf-8')
        
        print issuekey
        
        class_result=[]
        API_result=['']
        add_line =''
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
                        elif line[:2]=='+ ':#提取+的内容
                            add_line = add_line + line.strip('\n').strip(' ')       
            except IOError:
                break
        
            getAPI(add_line)
        print j
        #Result.update[issuekey] = [class_result,API_result]
        Result[issuekey] = [class_result]
    
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