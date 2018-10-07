#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Tue Nov 28 11:17:00 2017

@author: Congying.Xu
"""
import xlrd
import xlwt

j=0
f = xlwt.Workbook() #创建工作簿
sheet1 = f.add_sheet(u'sheet1',cell_overwrite_ok=True) #创建sheet
#sheet2 = f.add_sheet(u'comments',cell_overwrite_ok=True) #创建sheet

for num in range(10):
    dir = 'Output/FeaturnLocation_result'+ str(num)+ '.xls'
    workbook = xlrd.open_workbook(dir)
    sheet = workbook.sheet_by_name('sheet1')
    #17列，第一列为issuekey    
    for i in range(2,102):#行数
    
        for  k in range(17):#列数
            sheet1.write(j,k,sheet.cell(i,k).value.encode('utf-8'))
            
        j = j+1
    

f.save('Output/Hadoop_FeaturnLocation_result.xls')