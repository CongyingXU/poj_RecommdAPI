#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Sat Aug 12 14:33:35 2017

@author: Congying.Xu
"""

"""
数据结构设置：
project_dict= { "类的路径" : [【类名】，【方法名】，【( 变量类型, 变量名 ),...,( 变量类型, 变量名 )】，【注释】] , ... } 
"""




import computeSimilarity
import os
import javalang
import xlwt
from nltk.tokenize import StanfordTokenizer
import re
from nltk.corpus import stopwords#停词
import nltk


def Tokenize_stopwords_stemmer(texts):
    
    #用斯坦福的分词采用这一段，用普通分词时不用这个
    #tokenize
    Str_texts=texts[0]
    #tokenizer = StanfordTokenizer(path_to_jar=r"/Users/apple/Documents/tools/stanford-parser-full-2015-04-20/stanford-parser.jar")
    tokenizer = StanfordTokenizer(path_to_jar=r"stanford-parser.jar")
    texts_tokenized=tokenizer.tokenize(Str_texts)#输入必须是字符串
    
    p2=r'.+[-_\./"].+'
    pa2=re.compile(p2)
    texts_filtered=[]
    for document in  texts_tokenized:
        if document in pa2.findall(document):
            if document.find('_')>-1 :
                texts_filtered = texts_filtered + document.split('_')
            elif document.find('-')>-1:
                texts_filtered = texts_filtered + document.split('-')
            elif document.find('.')>-1:
                texts_filtered = texts_filtered + document.split('.')
        else:
            texts_filtered.append(document)
    
    
    
    p1=r'[-@<#$%^&*].+'
    pa1=re.compile(p1) 
    p3=r'.+">'
    pa3=re.compile(p3)
    english_stopwords = stopwords.words('english')#得到停词
    english_punctuations = [',', '.', ':', ';', '?', '(', ')', '[', ']', '&', '!', '*', '@', '#', '$', '%','\n'
                            ,'1','2','3','4','5','6','7','8','9','0','<','>','/','\"','\'','{','}','!','~','`'
                            ,'$','^','/*','*/','/**','**/','**','-','_','+','=',r'-?-',r'@?']#得到标点
    texts_filtered0=[]
    for document in texts_filtered:
        if  document in pa1.findall(document) or document in pa3.findall(document) or document == '' or document == "''" or document == "``" or document in english_stopwords or  document in english_punctuations:
            pass
        else:
            texts_filtered0.append(document)
    
            
    porter = nltk.PorterStemmer()
    texts_Stemmered=[porter.stem(t) for t in texts_filtered0]#列表类型
    
        
    return texts_Stemmered #返回一个列表

def extract_class(cls):
    '''
    将类的方法和变量分离
    return [ [类名]，【方法名】，【( 变量类型, 变量名 ),...,( 变量类型, 变量名 )】,[注释]]

    cls.__dict__={'body': [FieldDeclaration, MethodDeclaration, MethodDeclaration, MethodDeclaration], 'implements': [ReferenceType], 'modifiers': set([u'abstract', u'public']), 'name': u'Ab1', 'documentation': None, 'type_parameters': None, 'extends': None, 'annotations': []}
    FieldDeclaration
    {'modifiers': [u'private'], 'documentation': 'None', 'declarators': [{'dimensions': [], 'name': u'observers', 'initializer': {'body': 'None', 'postfix_operators': [], 'qualifier': 'None', 'selectors': [], 'prefix_operators': [],
    MethodDeclaration
    {'body': [ForStatement], 'modifiers': set([u'public']), 'name': u'notifyObservers', 'parameters': [], 'documentation': None, 'type_parameters': None, 'throws': None, 'return_type': None, 'annotations': [Annotation], '_position': (24, 9)}
    
    '''
    #  cls_dict={'field':[],'method':{}}
    cls_content=[]
    cls_name = [cls.name]
    cls_methods=[]
    cls_variables=[]#注意存入的是 元组
    cls_comments=''
    
    if cls.documentation != None:
        cls_comments = cls_comments + cls.documentation 
    
    for each_field in cls.fields: #成员属性添加
        if each_field.documentation !=None:#成员属性的注释添加
            cls_comments = cls_comments + each_field.documentation
        for each in each_field.declarators:#成员属性添加    
            if isinstance(each,javalang.tree.VariableDeclarator):    
                cls_variables.append( (each_field.type.name , each.name ) )  #append(  (变量类型,变量名)  )
    
    for each_method in cls.methods: #成员方法信息添加
        if each_method.documentation !=None:#成员方法的注释添加
            cls_comments = cls_comments + each_method.documentation
        cls_methods.append(each_method.name )#成员方法名添加
        
        if len(each_method.parameters) > 0:#成员方法入口参数添加
            for each_var in each_method.parameters:
                cls_variables.append( (each_var.type.name , each_var.name ) )
        if each_method.body!= None:
            for each in each_method.body:#成员方法添加    
                if isinstance(each,javalang.tree.LocalVariableDeclaration):
                    cls_variables.append( ( each.type.name , each.declarators[0].name ) )  #append(  (变量类型,变量名)  )
         
    
    cls_content.append(cls_name)
    cls_content.append(cls_methods)
    cls_content.append(cls_variables)
    cls_content.append( [cls_comments] )
    
    return cls_content
    
def get_class(dir):

    '''
    根据java语言字符串分析，找到一个文件里面的类
    with open('test.java', 'r') as f:
        get_class(f.read())

    param content java语句字符串

    return {"类的路径" : [【类名】，【方法名】，【( 变量类型, 变量名 ),...,( 变量类型, 变量名 )】，【注释】]}
    '''
    class_dict={}
    try:
        class_dict={}
        with open(dir, 'r') as tmp_f:
            content = tmp_f.read()
        
            tree=javalang.parse.parse(content)
            for each in tree.types:
                #if isinstance(each,javalang.tree.ClassDeclaration):
                    #对路径名进行美化处理，去掉  Input
                    String_dir = dir.split('Input/')
                    class_dict[String_dir[1]] = extract_class( each )  #以类路径名  作为Key  ，类的内容为 Value
    except javalang.parser.JavaSyntaxError:
        pass
    
    return class_dict


                        
def extract_file(dir):#经处理，类名统一以org文件开始
    project_dict={}
    for dirpath,dirname,filename in os.walk(dir):
        for each_file in filename:
            if each_file.endswith(".java"):
                #if 'test' not in dirpath:
                    tmp_path=os.path.join(dirpath,each_file)
                    project_dict.update( get_class(tmp_path) )

    return project_dict

import sys 
reload(sys) # Python2.5 初始化后会删除 sys.setdefaultencoding 这个方法，我们需要重新载入 
sys.setdefaultencoding('utf-8') 


repo_dir='Input/hadoop-common'

if __name__ == '__main__':
    project_dict=extract_file(repo_dir)
    #print  project_dict
    #dir='/Users/apple/Documents/hbase-1-master/hbase-client/src/main/java/org/apache/hadoop/hbase/ClusterStatus.java'
    #print get_class(dir)
    
    #project_dict={'路径':[['类名'], ['方法名','aasf','afads'],[('变量类型','变量名'),('ad','safew')],['afeaeefa']]}
    #存入文件
    #缺点，一次性的，会把之前的文案覆盖掉
    f = xlwt.Workbook() #创建工作簿
    sheet1 = f.add_sheet(u'sheet1',cell_overwrite_ok=True) #创建sheet
    #sheet2 = f.add_sheet(u'comments',cell_overwrite_ok=True) #创建sheet
    sheet1.write(0,0,"路径".decode('utf-8'))
    sheet1.write(0,1,"类名".decode('utf-8'))
    sheet1.write(0,2,"方法名".decode('utf-8'))
    #sheet1.write(0,4,"注释".decode('utf-8'))
    sheet1.write(0,3,"变量名".decode('utf-8'))
    sheet1.write(0,5,"变量信息".decode('utf-8'))
    sheet1.write(0,6,"注释信息".decode('utf-8'))
    """
    sheet2.write(0,0,"路径".decode('utf-8'))
    sheet2.write(0,1,"类名".decode('utf-8'))
    sheet2.write(0,2,"注释信息".decode('utf-8'))
    #sheet2.write(0,3,"变量名".decode('utf-8'))
   """
    
    """
    #版本1  原生态版
    #project_dict= { "类的路径" : [【类名】，【方法名】，【( 变量类型, 变量名 ),...,( 变量类型, 变量名 )】，【注释】] , ... } 
    
    i=1
    for key in project_dict:
        sheet1.write(i,0,key.decode('utf-8'))
        sheet2.write(i,0,key.decode('utf-8'))
        sheet1.write(i,1,project_dict[key][0][0].decode('utf-8'))
        sheet2.write(i,1,project_dict[key][0][0].decode('utf-8'))
        
        method_name=''
        for name in project_dict[key][1]:
            method_name = name + ' '+method_name
        sheet1.write(i,2,method_name.decode('utf-8'))
        
        variable_name=''
        variable_info=''
        j = 2
        for variable in project_dict[key][2]:
            variable_info = variable_info + variable[0] + ',' + variable[1] + ';'
            variable_name = variable[1] + ' '+variable_name
            j=j+1
        variable_info = variable_info.strip(';')   
        sheet1.write(i,3,variable_name.decode('utf-8'))
        sheet1.write(i,5,variable_info.decode('utf-8'))
        
        
        #存注释，在表格中  Exception: String longer than 32767 characters
        comments=project_dict[key][3][0]
        number = len(comments) / 30000 +1
        for column in range(number):    
            if column < number-1:
                sheet2.write(i,column+2,comments[column*30000 : (column+1)*30000 ].decode('utf-8'))
            else:
                sheet2.write(i,column+2,comments[column*30000 :].decode('utf-8'))
        i = i + 1
             
    f.save('/Users/apple/Documents/API推荐项目/hbase/SrcInfo.xls')#保存文件    
    """
    
    #版本2  文本预处理版
    #project_dict= { "类的路径" : [【类名】，【方法名】，【( 变量类型, 变量名 ),...,( 变量类型, 变量名 )】，【注释】] , ... } 
    
    i=1
    for key in project_dict:
        sheet1.write(i,0,key.decode('utf-8'))
        #sheet2.write(i,0,key.decode('utf-8'))
        
        #类名
        Class_name0=computeSimilarity.tokenize_stopwords_stemmer(project_dict[key][0])
        Class_name=''
        for word in Class_name0:
            Class_name = Class_name+' ' + word
        
        sheet1.write(i,1,Class_name.strip(' '))
        #sheet2.write(i,1,Class_name.strip(' '))
        
        #方法名
        method_name=''
        for name in project_dict[key][1]:
            method_name = name + ' '+method_name
        
        Method_name0=Tokenize_stopwords_stemmer([method_name])
        Method_name=''
        for word in Method_name0:
            Method_name = Method_name +' ' + word
            
        sheet1.write(i,2,Method_name.strip(' '))
        
        #变量名+变量信息
        variable_name=''
        variable_info=''
        j = 2
        for variable in project_dict[key][2]:
            
            Variable_name0=Tokenize_stopwords_stemmer([variable[1]])
            Variable_name=''
            for word in Variable_name0:
                Variable_name = Variable_name +' ' + word
            #variable_info = variable_info + variable[0] + ',' + Variable_name.strip(' ') + ';'
            variable_info = variable_info + variable[0] + ',' + variable[1].strip(' ') + ';'
            
            variable_name = variable[1] + ' '+variable_name
            j=j+1
        variable_info = variable_info.strip(';')
        
        Variable_name0=Tokenize_stopwords_stemmer([variable_name])
        Variable_name=''
        for word in Variable_name0:
            Variable_name = Variable_name +' ' + word
        
        sheet1.write(i,3,Variable_name.strip(' '))
        sheet1.write(i,5,variable_info)
        
        
        #存注释，在表格中  Exception: String longer than 32767 characters
        Comments0=Tokenize_stopwords_stemmer(project_dict[key][3])
        Comments=''
        for word in Comments0:
            Comments = Comments + ' ' + word
        
        Comments= Comments.strip(' ')
        
        #comments=project_dict[key][3][0]
        number = len(Comments) / 30000 +1
        for column in range(number):    
            if column < number-1:
                sheet1.write(i,column+6,Comments[column*30000 : (column+1)*30000 ].decode('utf-8'))
            else:
                sheet1.write(i,column+6,Comments[column*30000 :].decode('utf-8'))
        i = i + 1
        print key
        print i
             
    #f.save('Output/repo_SrcfileInfo.xls')#保存文件 
    f.save('/Users/apple/Documents/API推荐项目/HadoopCommon/repo_SrcfileInfo.xls')
    #cxf有问题，但时候好好看看