#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Sun Nov 26 12:57:43 2017

@author: Congying.Xu

爬取issue-patch文件
"""

import requests
from bs4 import BeautifulSoup
import bs4
import os
import csv 

#请求对应url，并将内容返回
def getHTMLText(url):
    try:
        r = requests.get(url, timeout=30)
        r.raise_for_status()
        r.encoding = r.apparent_encoding
        return r.text
    except:
        return ""

#得到iusseid对应的attachments信息，以列表存储
def fillattachList(alist, html ,issuekey):
    soup = BeautifulSoup(html, "html.parser")
    if soup.find('ol',attrs={'id':"file_attachments",'class':"item-attachments"}) == None:#对应iueesid无attacments
        with open('Noneattachments.txt','a') as f:#将iusseid信息记录入文件
            f.write(issuekey+'\n')
            f.close()
            
        return
    
    for li in soup.find('ol',attrs={'id':"file_attachments",'class':"item-attachments"}).children:#找到对应的标签
        if isinstance(li, bs4.element.Tag):
            div = li('div')
            a=div[0]('a')
            alist.append(a[0].attrs['href'])#attachents写入列表
            #print (a[0].attrs['href'])
 
#将每个attachment吸入文件    
def writeattach(alist,issuekey):
    os.chdir('/Users/apple/Documents/API/MULAPI+/HadoopHDFS/HadoopHDFS-attachments')
    for i in range(len(alist)):
        #if alist[i].split('.')[-1] != 'patch' and alist[i].split('.')[-1] !='txt':
        #    continue
        url = 'https://issues.apache.org'+alist[i]
        html = getHTMLText(url)
        num=str(i)
        file=issuekey+'_'+num+'.patch' 
        try:
            with open(file,'w') as f:
                f.writelines(html)
                f.close()
        except TypeError:
            with open('TypeError.txt','a') as f:#将TypeError异常写入文件记录，继续执行
                f.write(alist[i]+'\n')
                f.close()
            continue
        
     
def main():
    #打开文件，从iusse表中读取iusseid信息
    with open('/Users/apple/Documents/API/MULAPI+/HadoopHDFS/HadoopHDFS.csv','rb') as csvfile:
        reader = csv.reader(csvfile)
        column = [row[1] for row in reader]
        
    #依据iusseid关键字列表，循环爬取每个  iusseid信息（attachments）    
    for i in range(4,len(column)-1): 
        #print i                        
        uinfo = []
        url = 'https://issues.apache.org/jira/browse/'+column[i]+'?attachmentSortBy=fileName#attachmentmodule'
        html = getHTMLText(url)
        fillattachList(uinfo, html,column[i])
        writeattach(uinfo,column[i])


main()

