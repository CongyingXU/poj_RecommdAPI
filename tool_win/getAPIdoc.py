#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Sat Aug 12 14:33:35 2017

@author: Congying.Xu
"""

"""
数据结构
all_info { 类名：[【修饰，方法名，描述，参数】,...]， ... }

参数为空时：用   ‘（）’表示；有参时：‘（类型 名称）’
方法介绍为空时：用  ''表示
"""
import requests
from bs4 import BeautifulSoup
import bs4
import os
import xlwt
import csv 

#请求对应url，并将内容返回
def getHTMLText(url):
    try:
        r = requests.get(url, timeout=10)
        r.raise_for_status()
        r.encoding = r.apparent_encoding
        return r.text
    except:
        return ""

#得到iusseid对应的attachments信息，以列表存储
#all_info={ 类名：【空】 ...}
#数据结构  { 类名：[【修饰,方法名，描述，参数】,...]， ... }#无参数时，用''表示
def fillattachList(all_info,html,class_name):
    soup = BeautifulSoup(html, "html.parser")
    """
    #try:
    #if len(soup.find('a',attrs={'name':"method_summary"}).parent('table'))>0:
        if len(soup.find('a',attrs={'name':"method.summary"}).parent('table'))>0:#lang 中是   .
            table = soup.find('a',attrs={'name':"method.summary"}).parent('table')[0]
            for tr in table('tr'):#列表形式存储
                if tr.attrs.has_key('class') and (tr.attrs['class'][0] == 'altColor' or tr.attrs['class'][0] == 'rowColor'):
                    method_info = []
                    for td in tr('td'):
                        
                        if td.attrs['class'][0] == 'colFirst':
                            code = td('code')[0]
                            return_type = ''
                            for content in code.contents:
                                try:
                                    return_type =return_type + content.string
                                except TypeError:
                                    pass
                            method_info.append(return_type)
                        elif td.attrs['class'][0] == 'colLast':
                           
                            method_name=td('code')[0]('span')[0].string
                            
                            
                            method_description=''
                            if len(td('div'))>0:#是否有简介
                                method_description0=''
                                for content in td('div')[0].contents:
                                    if isinstance(content,bs4.element.Tag):
                                        if content.string != None: 
                                            method_description0 = method_description0 + content.string
                                    else:
                                        method_description0 = method_description0 + content
                                
                                for str0 in method_description0.split('\n'):
                                    method_description = method_description + str0.strip(' ')
                                    
                            else:
                                method_description=''
                        
                            #提取参数信息，一些格式的特殊处理
                            para0 = ''
                            if td('code')[0].contents[1] == '(':
                                for content in td('code')[0].contents[1:]:
                                    if isinstance(content,bs4.element.Tag):
                                        para0 = para0 + content.string
                                    else:
                                        para0 = para0 + content
                            else:
                                para0 = td('code')[0].contents[1]
                    
                            para=''
                            for str0 in para0.split('\n'):
                                para = para + str0.strip(' ')
                            
                            method_info.append(method_name)
                            method_info.append(method_description)
                            method_info.append(para)
        
                    all_info[class_name].append(method_info)  
    #except AttributeError:
    #    pass
    """
    #try:
    if soup.find('a',attrs={'name':"method_summary"}) !=None:
        #if len(soup.find('a',attrs={'name':"method.summary"}).parent('table'))>0:#lang 中是   .
            table = soup.find('a',attrs={'name':"method_summary"}).next.next.next
            for tr in table('tr'):#列表形式存储
                #print tr
                if tr.attrs.has_key('bgcolor') and tr.attrs.has_key('class') and tr.attrs['class']==['TableRowColor'] and tr.attrs['bgcolor'] == 'white' :
                #if  tr.attrs['bgcolor'] == 'white' :
                    method_info = []
                   
                    td0 = tr.contents[1]        
                    return_type = ''
                    for code  in td0('code'):
                        for content in code.contents:
                            try:
                                if content.string == '\n':
                                    continue
                                else:
                                    return_type =return_type + content.string
                            except TypeError:
                                continue
                    method_info.append(return_type)
                    
                    td1 = tr.contents[3] 
                    #method_name=td('code')[0]('span')[0].string
                    method_name=td1('b')[0].string
                            
                    method_description=''
                    if len(td1('br'))>0:#是否有简介
                        method_description0=''
                        for content in td1('br')[0].contents:
                            if isinstance(content,bs4.element.Tag):
                                if content.string != None: 
                                
                                    method_description0 = method_description0 + content.string
                            else:
                                method_description0 = method_description0 + content
                                
                        for str0 in method_description0.split('\n'):
                            method_description = method_description + str0.strip(' ')
                                    
                    else:
                        method_description=''
                        
                            #提取参数信息，一些格式的特殊处理
                    para0 = ''
                    if td1('code')[0].contents[1] == '(':
                        for content in td1('code')[0].contents[1:]:
                            if isinstance(content,bs4.element.Tag):
                                para0 = para0 + content.string
                            else:
                                para0 = para0 + content
                    else:
                        para0 = td1('code')[0].contents[1]
                    
                    para=''
                    for str0 in para0.split('\n'):
                        para = para + str0.strip(' ')
                            
                    method_info.append(method_name)
                    method_info.append(method_description)
                    method_info.append(para)
        
                    all_info[class_name].append(method_info)  
    #except AttributeError:
    #    pass
    
    """
    if soup.find('a',attrs={'name':"method_summary"}) == None:#对应iueesid无attacments
        with open('Noneattachments.txt','a') as f:#将iusseid信息记录入文件
            f.write(issuekey+'\n')soup.find('a',attrs={'name':"method_summary"}).parent('table')
            f.close()
            
        return
    """
    """
    if len(soup.find('a',attrs={'name':"method_summary"}).parent('table'))>0:
        table = soup.find('a',attrs={'name':"method_summary"}).parent('table')[0]
        for td in table('td'):
            if td.attrs['class'][0] == 'colLast':
               
                method_name=td('code')[0]('strong')[0].string
                
                
                method_description=''
                if len(td('div'))>0:#是否有简介
                    method_description0=''
                    for content in td('div')[0].contents:
                        if isinstance(content,bs4.element.Tag):
                            if content.string != None: 
                                method_description0 = method_description0 + content.string
                        else:
                            method_description0 = method_description0 + content
                    
                    for str0 in method_description0.split('\n'):
                        method_description = method_description + str0.strip(' ')
                        
                else:
                    method_description=''
            
                #提取参数信息，一些格式的特殊处理
                para0 = ''
                if td('code')[0].contents[1] == '(':
                    for content in td('code')[0].contents[1:]:
                        if isinstance(content,bs4.element.Tag):
                            para0 = para0 + content.string
                        else:
                            para0 = para0 + content
                else:
                    para0 = td('code')[0].contents[1]
        
                para=''
                for str0 in para0.split('\n'):
                    para = para + str0.strip(' ')
                
                method_info=[method_name,method_description,para]
                all_info[class_name].append(method_info)
     """
 
#将每个attachment吸入文件    
def writeattach(all_info):
    #os.chdir('/Users/apple/Documents/API推荐项目/APIdoc')
    
    f = xlwt.Workbook() #创建工作簿
    sheet1 = f.add_sheet(u'sheet1',cell_overwrite_ok=True) #创建sheet
    sheet1.write(0,0,"class_name".decode('utf-8'))
    sheet1.write(0,1,"method_name".decode('utf-8'))
    sheet1.write(0,2,"para_info".decode('utf-8'))
    sheet1.write(0,3,"method_description".decode('utf-8'))
    sheet1.write(0,4,"modifier_type".decode('utf-8'))
    
    #all_info { 类名：[【 修饰，方法名，描述，参数】,...]， ... }
    
    i=1
    for key in all_info:
        for method in all_info[key]:
            sheet1.write(i,0,key)
            sheet1.write(i,1,method[1])#方法名
            sheet1.write(i,2,method[3])#参数信息
            sheet1.write(i,3,method[2])#描述
            sheet1.write(i,4,method[0])#返回类型
            i = i + 1
             
    f.save('/Users/apple/Documents/API推荐项目/APIdoc/commons-io_doc.xls')#保存文件    
        
     
def main():
    
    all_info={}
    url = 'http://commons.apache.org/proper/commons-io/javadocs/api-2.4/allclasses-frame.html'
    html = getHTMLText(url)
    #print html
    soup = BeautifulSoup(html, "html.parser")
    """
    url_list=[]
    class_name=[]
    for li in soup.find('ul').children:#找到对应的标签
        if isinstance(li, bs4.element.Tag):
            a=li('a')
            url_list.append(a[0].attrs['href'])
            all_info[a[0].string]=[]
            class_name.append(a[0].string)
    """
    
    url_list=[]
    class_name=[]
    font = soup.find('font',attrs={'class':"FrameItemFont"})
    for a in font('a'):
            url_list.append(a.attrs['href'])
            all_info[a.string]=[]
            class_name.append(a.string)
            
    #print all_info
    #print url_list
    #https://www.slf4j.org/api/org/slf4j/impl/Log4jLoggerAdapter.html
  #https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/helpers/AbsoluteTimeDateFormat.html
    #http://commons.apache.org/proper/commons-io/javadocs/api-2.4/org/apache/commons/io/input/AutoCloseInputStream.html
    i=0
    #依据iusseid关键字列表，循环爬取每个  iusseid信息（attachments）    
    for url0 in url_list: 
        #rint class_name[i]
        print url0
        url = 'http://commons.apache.org/proper/commons-io/javadocs/api-2.4/'+url0
        html = getHTMLText(url)
        fillattachList(all_info,html,class_name[i])
        i=i+1
        
    writeattach(all_info)
    
    
    
    #writeattach(all_info)
   

main()