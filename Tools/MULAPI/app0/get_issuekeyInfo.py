#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Sat Apr  7 12:46:08 2018

@author: Congying.Xu

实时抓取 issuekey网页信息
https://issues.apache.org/jira/browse/HBASE-20359
"""

#import sys
#import os
#sys.path.append(os.getcwd()+'/Computing_recommendation')

import requests
from bs4 import BeautifulSoup

from Computing_recommendation import test1

from Computing_recommendation.issue import Issue0
from Computing_recommendation import getRecmdAPI_result
from Computing_recommendation import getFeatureLocation_result
from Computing_recommendation import getSimilarityScores2Reports


issue = Issue0()

def get_recommendations():
        
    global  issue
    
    #print issue.Issuekey
    #issue.Issuekey = 'Key'
    issue.similar_issues_list  =  getSimilarityScores2Reports.get_similar_issues(issue)
    issue.location_result_list =  getFeatureLocation_result.main(issue)
    issue.method_result_list =  getRecmdAPI_result.main(issue)
    
    #print issue.location_result_list[:15]
    #print issue.method_result_list[:15]


def get_issueInfo(issuekey):
    global issue 
    url = 'https://issues.apache.org/jira/browse/'+issuekey
    html = requests.get(url).text
    soup = BeautifulSoup(html, "html.parser")
    head = soup.find('div',attrs={'class':"aui-page-header-main"})
    summary =  head('h1')[0].contents[0]
    
    
    issuedetails = soup.find('ul',attrs={'class':'property-list two-cols','id':"issuedetails"})
    li_list = issuedetails('li')
    for li in li_list:
        if li('strong')[0].contents[0] == 'Priority:':
            priority =  li('span')[0].contents[-1].strip(' ')
            #print Priority
        if li('strong')[0].contents[0] == 'Component/s:':
            Components = []
            for a in li('a'):
                Components.append( a.contents[-1].strip(' ').strip('\n') )
            if len(Components)==0:
                Components.append('')
            print Components
        
        
    descriptionmodule = soup.find('div',attrs={'id':"descriptionmodule"})
    try:
        description = descriptionmodule('p')[0].contents[0]
    except:
        description = ''
    
    
    link = soup.find('dl',attrs={'class':"links-list "})
    linked_issues = []
    try :
        for a in link('a'):
            linked_issues.append(a.contents[0]) 
    except:
        pass        
    if len(linked_issues)==0:
        linked_issues  = ['']
        
        
    project = soup.find('a',attrs={'id':"project-name-val"}).string
    print 'poj',project
    people = soup.find('li',attrs={'class':"people-details"})
    reporter_info  = soup.find('span',attrs={'id':"reporter-val",'class':'view-issue-field'})
    #print reporter
    
    
    issue.Component = Components
    issue.Description = description
    issue.Issuekey  = issuekey
    issue.Linkedissue = linked_issues
    issue.Priority = priority
    issue.Project = project
    #issue.Reporter = 
    issue.Summary = summary
    
    #return issue
    

def main(issuekey):
    get_issueInfo(issuekey)
    #get_recommendations()
    return issue

def main_description(description,project):
    global issue
    issue.Description = description
    issue.Project = project

    issue.Component = ['']
    issue.Issuekey = 'Issuekey'
    issue.Linkedissue = ['']
    issue.Priority = ''
    issue.Project = project
    issue.Summary = ''

    get_recommendations()
    return issue

if __name__=='__main__':
    main('HBASE-18434')
