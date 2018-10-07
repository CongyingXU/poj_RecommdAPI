#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Mon Aug 14 20:09:45 2017

@author: Congying.Xu
"""

#!/usr/bin/env python2
# -*- coding: utf-8 -*-

import gensim
import logging
import multiprocessing
import os
import re
import sys
 
from pattern.en import tokenize
from time import time
 
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s',
                    level=logging.INFO)
 
 
def cleanhtml(raw_html):
    cleanr = re.compile('<.*?>')
    cleantext = re.sub(cleanr, ' ', raw_html)
    return cleantext
 
 
class MySentences(object):
    def __init__(self, dirname):
        self.dirname = dirname
 
    def __iter__(self):
        for root, dirs, files in os.walk(self.dirname):
            for filename in files:
                file_path = root + '/' + filename
                for line in open(file_path):
                    sline = line.strip()
                    if sline == "":
                        continue
                    rline = cleanhtml(sline)
                    tokenized_line = ' '.join(tokenize(rline))
                    is_alpha_word_line = [word for word in
                                          tokenized_line.lower().split()
                                          if word.isalpha()]
                    yield is_alpha_word_line
 
def mian(texts_dir,result_model_dir):
    """
    texts_dir:训练样本所在的目录地址（！！！到目录名）
    result_model_dir：训练好的模型所在的位置（！！！到具体的文件名）
    """
#if __name__ == '__main__':
    #if len(sys.argv) != 2:
    #    print "Please use python train_with_gensim.py data_path"
    #    exit()
    #data_path = sys.argv[1]
    
    #data_path = '/Users/apple/Downloads/test/texts'
    
    #begin = time()
 
    sentences = MySentences(texts_dir)
    model = gensim.models.Word2Vec(sentences,
                                   size=200,
                                   window=10,
                                   min_count=10,
                                   workers=multiprocessing.cpu_count())
    model.save(result_model_dir)
    #model.save("/Users/apple/Downloads/test/data/model/word2vec_gensim")
    #model.wv.save_word2vec_format("/Users/apple/Downloads/test/data/model/word2vec_org",
    #                              "/Users/apple/Downloads/test/data/model/vocabulary",
    #                              binary=False)
 
    #end = time()
    #print "Total procesing time: %d seconds" % (end - begin)
    
    #model = Word2Vec.load('/Users/apple/Downloads/test/data/model/word2vec_gensim')
    #model.similarity('king','man')
    
    