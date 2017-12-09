#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Wed Aug  9 14:56:42 2017

@author: Congying.Xu
进行API参数的推荐
"""
import xlrd
import extrPatchInfo
import extrSrcFileInfo
from pygit import PyGit
# https://github.com/mijdavis2/pygit
# pip install git+git://github.com/mijdavis2/pygit.git

repo_path = "/Users/apple/Git/hbase-1"

def Info_IO():
    dir0 = 'Output/FeaturnLocation_result.xls' 
    workbook0 = xlrd.open_workbook(dir0,'r')
    sheet0 = workbook0.sheet_by_name('sheet1')
    return sheet0

def getRelatedSrcfile(issuekey ,sheet0):
    #从feation location中，得到结果
    RelatedSrcfile_list= []
    for i in range(1,sheet0.nrows):
        if sheet0.cell(i,0).value == issuekey:
            j=1
            while 1:
                try:
                    RelatedSrcfile_list.append( sheet0.cell(i,j).value )
                    j = j+1
                    ############################
                    ############################
                    if j>11:#目前提取10个相关的源文件
                        break
                except IOError:
                    break
            break
    return RelatedSrcfile_list

#将项目退回到某一版本，并进行解析信息
# file_name:以列表形式  存取所有相关的源文件
def getSrcvariable_Info_git(issuekey,file_name_list):
    git_repo = PyGit(repo_path)
    if not extrPatchInfo.CommitHash_dict.has_key(issuekey):
        pass
        #print issuekey
    try:
        commit_hash = extrPatchInfo.CommitHash_dict[issuekey]
    except KeyError:
        #print issuekey
        return {'':''}
    git_repo('reset --hard '+commit_hash)
    variable_Info_dict = {}
    for file_name in file_name_list:
        file_name = repo_path + file_name [file_name.find('/'):]#因为有些是  hbase-1-master开头的
        #String_dir = file_name.split('Input/')##因为路径名不统一而采取的 手段，很烦，删
        try:
            class_info_list = extrSrcFileInfo.get_class(file_name)[ file_name ]
        except KeyError:
            #print issuekey
            return {'':''}
        #格式：{ file_name;[(变量类型，变量名)] }
        variable_Info_dict[file_name] = class_info_list[2]
    
    return variable_Info_dict



