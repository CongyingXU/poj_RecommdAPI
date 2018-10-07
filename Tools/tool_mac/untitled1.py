#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Tue Nov 28 21:41:33 2017

@author: Congying.Xu
"""

import requests
response = requests.get("http://211.249.63.55")
text = response.text
print(text)