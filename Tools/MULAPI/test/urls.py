"""test URL Configuration

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/1.11/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  url(r'^$', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  url(r'^$', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.conf.urls import url, include
    2. Add a URL to urlpatterns:  url(r'^blog/', include('blog.urls'))
"""
from django.conf.urls import url
from django.contrib import admin


import app0.views as av

urlpatterns = [
    url(r'^admin/', admin.site.urls),
    url(r'^$', av.index),
    url(r'^get_text', av.get_text),
    url(r'^get_description', av.get_description),
    url(r'^Similar_issues(?P<Issuekey>[0-9a-zA-Z.\/\-]{1,})', av.get_Similar_issues ,name  = 'Similar_issues'),
    url(r'^Candidate_locations(?P<Issuekey>[0-9a-zA-Z.\/\-]{1,})', av.get_Candidate_locations),
    url(r'^Candidate_APIs(?P<Issuekey>[0-9a-zA-Z.\/\-]{1,})', av.get_Candidate_APIs),
    #url(r'^Reordered_Candidate_APIs(?P<Dir>[0-9a-zA-Z.\/\-]{1,})', av.get_Reordered_Candidate_APIs),
    url(r'^Reordered_Candidate_APIs(?P<file>[0-9a-zA-Z.\/\- \ <>]{1,})_(?P<Issuekey>[0-9a-zA-Z.\/\-]{1,})', av.get_Reordered_Candidate_APIs,name  = 'Reordered_Candidate_APIs'),
    url(r'^file/(?P<Dir>[0-9a-zA-Z.\/\-]{1,})', av.get_fileContent),

]
