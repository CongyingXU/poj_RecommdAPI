# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models
#import ast

# Create your models here.




class issue(models.Model):
    Issuekey = models.CharField(max_length = 32, default = 'Issuekey',primary_key=True)
    Summary = models.TextField(default = 'null')
    Description = models.TextField(default = 'null')
    Project = models.CharField(max_length = 32)
    Component = models.CharField(max_length = 32,default = 'null')
    Reporter = models.CharField(max_length = 32)
    Priority = models.CharField(max_length = 32)
    Linkedissue = models.CharField(max_length = 32,default = 'null')
    #Url = models.URLField(null=True)
    Status = models.CharField(max_length = 16, default = 'closed')
    location_result_list = models.TextField()
    method_result_list = models.TextField()
    similar_issues_list = models.TextField(default = '')


class API(models.Model):
    Name = models.CharField(max_length = 32, default = 'Issuekey',primary_key=True)
    Description = models.TextField(default = 'null')
    Parameters = models.CharField(max_length = 32,default = '( )')
    ModifierType = models.CharField(max_length = 32,default = 'void')
    Url = models.URLField(null=True)
    Example = models.TextField(default = 'null')


class file(models.Model):
    Dir = models.CharField(max_length=128, default='Dir',primary_key=True)
    Name = models.CharField(max_length = 32, default = '')
    Method_name = models.TextField(default='')
    Variables_name = models.TextField(default = '')
    Variables_info = models.TextField(default='')
    Description = models.TextField(default = '')
    Content = models.TextField(default='')
    Project = models.CharField(max_length = 32)




    
