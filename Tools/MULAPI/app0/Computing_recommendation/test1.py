#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Sat Apr  7 22:03:55 2018

@author: Congying.Xu
"""
from issue import Issue0
import getRecmdAPI_result
import getFeatureLocation_result



def main():
        
    issue = Issue0()
    
    #print issue.Issuekey
    #issue.Issuekey = 'Key'
    issue.location_result_list =  getFeatureLocation_result.main(issue)
    issue.method_result_list =  getRecmdAPI_result.main(issue)
    
    print issue.location_result_list[:15]
    print issue.method_result_list[:15]
    
    
if __name__=='__main__':
    main()