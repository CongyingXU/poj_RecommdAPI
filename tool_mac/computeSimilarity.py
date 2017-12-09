#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Mon Aug  7 19:23:55 2017

@author: Congying.Xu
文本相似度计算的主要技术实现
"""

''' ------------------------------------------------------- 
    基本步骤：
        1.分别统计两个文档的关键词
        2.两篇文章的关键词合并成一个集合,相同的合并,不同的添加
        3.计算每篇文章对于这个集合的词的词频 TF-IDF算法计算权重
        4.生成两篇文章各自的词频向量
        5.计算两个向量的余弦相似度,值越大表示越相似     


        ~tokenize_stopwords_stemmer
         文本预处理，输入一个列表存放的的字符文本【“I am Chinese”】
                   输出一个列表存放的a bag of words  【" I" ，"am " , " Chinense"】
        ~CountKey
         计算某一文本的关键词，输入一个列表存放的a bag of words，【" I" ，"am " , " Chinense"】
                            输出(字典):表示该bag of words的关键词信息
        ～MergeKeys
         相似度计算，输入两个文本的关键词和一个数据集，输出两个文本的相似性分数
                  输入：dic1
                       dic2
                       数据集【[" I" ，"am " , " Chinense"],[" you " ，"are " , " Janpanese"] ...】
                       
    ------------------------------------------------------- '''

    # -*- coding: utf-8 -*-
import re 
import math
import nltk
from nltk.tokenize import StanfordTokenizer
from nltk.corpus import stopwords#停词
from nltk.tokenize import word_tokenize#分词
import xlrd
#import xlwt
import sys
reload(sys)
sys.setdefaultencoding('utf8')


def tokenize_stopwords_stemmer(texts):
#texts：列表存放的字符串    
    #用斯坦福的分词采用这一段，用普通分词时不用这个
    #tokenize
    Str_texts=texts[0]
    #tokenizer = StanfordTokenizer(path_to_jar=r"/Users/apple/Documents/tools/stanford-parser-full-2015-04-20/stanford-parser.jar")
    tokenizer = StanfordTokenizer(path_to_jar=r"stanford-parser.jar")
    texts_tokenized=tokenizer.tokenize(Str_texts)#输入必须是字符串
    
    p1=r'[-@<#$%^&*].+'
    pa1=re.compile(p1) 
    texts_filtered0 = [ document for document in  texts_tokenized  if not document in pa1.findall(document) ]
    
    p2=r'.+[-_\./].+'
    pa2=re.compile(p2)
    texts_filtered=[]
    for document in  texts_filtered0:
        if document in pa2.findall(document):
            if document.find('_')>-1 :
                texts_filtered = texts_filtered + document.split('_')
            elif document.find('-')>-1:
                texts_filtered = texts_filtered + document.split('-')
            elif document.find('.')>-1:
                texts_filtered = texts_filtered + document.split('.')
        else:
            texts_filtered.append(document)
    
    texts_filtered = [ document for document in  texts_filtered  if  document != '' and document != "''" and document != "``" ]
  
    #stopwords
    english_stopwords = stopwords.words('english')#得到停词
    texts_filtered_stopwords = [ document for document in texts_filtered if not document in english_stopwords]#
    
    english_punctuations = [',', '.', ':', ';', '?', '(', ')', '[', ']', '&', '!', '*', '@', '#', '$', '%','\n'
                            ,'1','2','3','4','5','6','7','8','9','0','<','>','/','\"','\'','{','}','!','~','`'
                            ,'$','^','/*','*/','/**','**/','**','-','_','+','=',r'-?-',r'@?']#得到标点
   
    texts_filtered = [ document for document in  texts_filtered_stopwords if not document in english_punctuations]#
    
            
    porter = nltk.PorterStemmer()
    texts_Stemmered=[porter.stem(t) for t in texts_filtered]#列表类型
    
        
    return texts_Stemmered #返回一个列表
    
    """
    #用普通的分词采用这一段，用斯坦福分词时不用这个
    #tokenize
    texts_tokenized = [[word.lower() for word in word_tokenize(document.decode('utf-8'))] for document in texts]
    
    #stopwords
    english_stopwords = stopwords.words('english')#得到停词
    texts_filtered_stopwords = [[word for word in document if not word in english_stopwords] for document in texts_tokenized]#去停词
    
    english_punctuations = [',', '.', ':', ';', '?', '(', ')', '[', ']', '&', '!', '*', '@', '#', '$', '%','\n'
                            ,'1','2','3','4','5','6','7','8','9','0','<','>','/','\"','\'','{','}','!','~','`'
                            ,'$','^','/*','*/','/**','**/','-','_','+','=']#得到标点
                            
    texts_filtered = [[word for word in document if not word in english_punctuations] for document in texts_filtered_stopwords]
    
    #stemmer
    texts_Stemmered=[]
    porter = nltk.PorterStemmer()
    for x in texts_filtered:
        texts_Stemmered.append([porter.stem(t) for t in x])#列表类型
    
        
    return texts_Stemmered[0] #返回一个列表
    """
    


#统计关键词及个数
def CountKey(text_words):
    try:
        #统计格式 格式<Key:Value> <属性:出现个数>
        i = 0
        table = {}
        
        #字典插入与赋值
        for word in text_words:
            if word!="" and table.has_key(word):      #如果存在次数加1
                num = table[word]
                table[word] = num + 1
            elif word!="":                            #否则初值为1
                table[word] = 1
        i = i + 1

        #键值从大到小排序 函数原型：sorted(dic,value,reverse)
        dic = sorted(table.iteritems(), key = lambda asd:asd[1], reverse = True)
        #print dic
        return dic
        
    except Exception,e:    
        print 'Error:',e
    finally:
        pass
        #print 'END\n\n'


''' ------------------------------------------------------- '''
#统计关键词及个数 并计算相似度
def MergeKeys(dic1,dic2,all_reports_tokens):
    #合并关键词 采用三个数组实现
    arrayKey = []
    for i in range(len(dic1)):
        arrayKey.append(dic1[i][0])       #向数组中添加元素
    for i in range(len(dic2)):       
        if dic2[i][0] in arrayKey:
            pass
            #print 'has_key',dic2[i][0]
        else:                             #合并
            arrayKey.append(dic2[i][0])
    else:
        pass
        #print '\n\n'
    
    #test = str(arrayKey).decode('string_escape')  #字符转换
    #print test

    #计算词频 infobox可忽略TF-IDF
    arrayNum1 = [0]*len(arrayKey)
    arrayNum2 = [0]*len(arrayKey)
    
    #赋值arrayNum1
    for i in range(len(dic1)):     
        key = dic1[i][0]
        value = dic1[i][1]#词频TF
        j = 0
        while j < len(arrayKey):
            if key == arrayKey[j]:
                
                #计算DF
                k=0
                for t in range(len(all_reports_tokens)):
                    if key in all_reports_tokens[t]:
                        k = k + 1
                        
                arrayNum1[j] = float(value) / float(k)
                #arrayNum1[j]=value
                break
            else:
                j = j + 1

    #赋值arrayNum2
    for i in range(len(dic2)):     
        key = dic2[i][0]
        value = dic2[i][1]
        j = 0
        while j < len(arrayKey):
            if key == arrayKey[j]:
                
                #计算DF
                k=0
                for t in range(len(all_reports_tokens)):
                    if key in all_reports_tokens[t]:
                        k = k + 1
                        
                arrayNum2[j] = float(value) / float(k)
                #arrayNum1[j]=value
                break
            else:
                j = j + 1
    
    #print arrayNum1
    #print arrayNum2
    #print len(arrayNum1),len(arrayNum2),len(arrayKey)

    #计算两个向量的点积
    x = 0
    i = 0
    while i < len(arrayKey):
        x = x + arrayNum1[i] * arrayNum2[i]
        i = i + 1
    #print x

    #计算两个向量的模
    i = 0
    sq1 = 0
    while i < len(arrayKey):
        sq1 = sq1 + arrayNum1[i] * arrayNum1[i]   #pow(a,2)
        i = i + 1
    #print sq1
    
    i = 0
    sq2 = 0
    while i < len(arrayKey):
        sq2 = sq2 + arrayNum2[i] * arrayNum2[i]
        i = i + 1
    #print sq2
    
    try:
        result = float(x) / ( math.sqrt(sq1) * math.sqrt(sq2) )
    except ZeroDivisionError:
        result=0.0
        
    return result


def all_compute2Similarity(text1,text2,basic_texts):#计算两个文本间的相似度
    #text1:[“。。。”]   text2: [“。。。”]   basic_texts:【 [ “。。。”] , [“。。。” ] , [ ] , [ ] 】
    
    #检查text1，或text2  是否在数据集中
    if text1 in basic_texts:
        pass
    else:
        basic_texts.append(text1)
    if text2 in basic_texts:
        pass
    else:
        basic_texts.append(text2)
        
    
    dic1 = CountKey(tokenize_stopwords_stemmer(text1))
    dic2 = CountKey(tokenize_stopwords_stemmer(text2))
    
    all_reports_tokens=[]
    for i in range(len(basic_texts)):
        text0=basic_texts[i]
        all_reports_tokens.append(tokenize_stopwords_stemmer(text0))
    
    result= MergeKeys(dic1, dic2,all_reports_tokens) 
    
    return result
    
def all_computeSimilarity(text1, basic_texts):#计算数据集中所有文本相似度
    #text1:[“。。。”]  basic_texts:【 [“。。。” ] , [ ] , [ ] , [ ] 】
    
    #检查text1，是否在数据集中
    if text1 in basic_texts:
        pass
    else:
        basic_texts.append(text1)      
    
    dic1 = CountKey(tokenize_stopwords_stemmer(text1))
    
    
    all_reports_tokens=[]
    for i in range(len(basic_texts)):
        text0=basic_texts[i]
        all_reports_tokens.append(tokenize_stopwords_stemmer(text0))
    
    
    result=[]
    for i in range(len(basic_texts)):
    #计算文档2-互动的关键词及个数
        dic2 = CountKey(all_reports_tokens[i])
    #合并两篇文章的关键词及相似度计算
        result.append( MergeKeys(dic1, dic2,all_reports_tokens) )
            
    return result


#其中  text1以及 basic_texts都是经过tokenize_stopwords_stemmer(texts)处理的
#此输入为tokenize_stopwords_stemmer(texts):的输出
def half_computeSimilarity(text1, basic_texts):#计算数据集中所有文本相似度
    #text1:[“。。。”]  basic_texts:【 [“  ” , "  " , "  " ] , 
    #                                 ......
    #                                [ "  ","  ","  " ] 】

    #检查text1，是否在数据集中
    if text1 in basic_texts:
        pass
    else:
        basic_texts.append(text1)      
    
    
    dic1 = CountKey(text1)
    
    result=[]
    for i in range(len(basic_texts)):
    #计算文档2-互动的关键词及个数
        dic2 = CountKey(basic_texts[i])
    #合并两篇文章的关键词及相似度计算
        result.append( MergeKeys(dic1, dic2,basic_texts) )
            
    return result
  
    

"""
def main():
    #!!!!!!!!!!!!!!!!!!!
    #数据集！！！！！以[ [..。] , [...] , ... , [...] , [...]]形式存放数据集
    all_reports_tokens=[]
    workbook = xlrd.open_workbook(r'/Users/apple/Documents/API推荐项目/hbase/Hbase.xlsx')
    #sheet1_name= workbook.sheet_names()[0]
    sheet1 = workbook.sheet_by_name('general_report')
    #print sheet1.cell(6,28).value.encode('utf-8')
    
    #得到所有文档的分词结果
    for i in range(4,5):
        texts0=[]
        texts0.append(sheet1.cell(i,28).value.encode('utf-8'))
        all_reports_tokens.append(tokenize_stopwords_stemmer(texts0))
    

    print all_reports_tokens[0]        
    
    #!!!!!!!!!!!!!!!!!!!
    #new feature
    #计算文档1-百度的关键词及个数
    dic1 = CountKey(all_reports_tokens[0])#这里的索引号即为 第0个report


    result=[]
    for i in range(0,1000):
    #计算文档2-互动的关键词及个数
        dic2 = CountKey(all_reports_tokens[i])
    #合并两篇文章的关键词及相似度计算
        result.append( MergeKeys(dic1, dic2,all_reports_tokens) )
            

    print result
    


if __name__ == '__main__':   
    main()
    
"""


        