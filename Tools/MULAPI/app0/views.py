# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.shortcuts import render, HttpResponseRedirect
from django.http import HttpResponse

from . import models
import get_issuekeyInfo

from aip import AipNlp

""" 你的 APPID AK SK """
APP_ID = '10493871'
API_KEY = '97AMli67i7a5GlWeFuPRNR91'
SECRET_KEY = 'yMaiiCKUB8G4bImZC263wRYUTiVrun1y '

client = AipNlp(APP_ID, API_KEY, SECRET_KEY)
model_dict = {'model': 'CNN'}


# Create your views here.



Issuekey = 'HBASE-20046'

def index(request):
    #return HttpResponse('Hello!')
    return render(request,'index.html')
    #return render(request, "Similar_issues.html")

def get_text(request):
    
    #global issuekey
    issuekey = request.GET['InputIssueKey']
    try:
        issue = models.issue.objects.get(pk=issuekey)
        result_list = issue.similar_issues_list.split(',')
        result_list_info = []
        for method in result_list:
            try:

                method_info = models.issue.objects.get(pk=method)  # issue
                method_info.method_result_list = method_info.method_result_list.split(',')

                for i in range(len(method_info.method_result_list)):
                    method_info.method_result_list[i] = models.API.objects.get(
                        pk=method_info.method_result_list[i])  # 将method名，转换成 API对象

                method_info.location_result_list = method_info.location_result_list.split(',')

                for i in range(len(method_info.location_result_list)):
                    method_info.location_result_list[i] = models.file.objects.get(
                        pk=method_info.location_result_list[i])  # 将 file路径，转换成 file对象

            except:
                continue
            if method == Issuekey:
                continue
            else:
                result_list_info.append(method_info)

        result_list_info1 = result_list_info[:5]
        result_list_info2 = result_list_info[5:10]
        result_list_info3 = result_list_info[10:15]
        result_list_info4 = result_list_info[15:20]

        return render(request, "Similar_issues.html",
                      {"Issue": issue, "result_list1": result_list_info1, "result_list2": result_list_info2,
                       "result_list3": result_list_info3, "result_list4": result_list_info4})
    except:
        issue = get_issuekeyInfo.main(issuekey)
        return HttpResponse(issue.similar_issues_list)

def get_description(request):

    description  =  request.GET['InputDescription']
    project = request.GET['InputProject']  #可以在这个时候，根据正则表达式，进行匹配
    issue = get_issuekeyInfo.main_description(description,project)

    location_result_list_str = ''
    for ele in issue.location_result_list:
        location_result_list_str = location_result_list_str +','+ele

    method_result_list_str = ''
    for ele in issue.method_result_list:
        method_result_list_str = method_result_list_str +','+ele

    similar_issues_list_str = ''
    for ele in issue.similar_issues_list:
        similar_issues_list_str = similar_issues_list_str +','+ele

    models.issue.objects.filter(Issuekey='Issuekey').update(Description = description,Project = project,location_result_list = location_result_list_str.strip(','),
                                method_result_list = method_result_list_str.strip(','),similar_issues_list = similar_issues_list_str.strip(','))

    #get_Similar_issues(request, issue.Issuekey)

    issue = models.issue.objects.get(pk='Issuekey')

    # return HttpResponse(issuekey)
    # return render(request,"Similar_issues.html",{"issue":issue})
    # return render(request, "Similar_issues.html")
    result_list = issue.method_result_list.split(',')
    result_list_info = []
    for method in result_list:
        try:
            method_info = models.API.objects.get(pk=method)
            result_list_info.append(method_info)
        except:
            continue

    location_list = issue.location_result_list.split(',')

    result_list_info1 = result_list_info[:5]
    result_list_info2 = result_list_info[5:10]
    result_list_info3 = result_list_info[10:15]
    result_list_info4 = result_list_info[15:20]

    return render(request, "Candidate_APIs.html",
                  {"Issue": issue, "result_list1": result_list_info1, "result_list2": result_list_info2,
                   "result_list3": result_list_info3, "result_list4": result_list_info4})


def get_Similar_issues(request, Issuekey):

    issue = models.issue.objects.get(pk=Issuekey)
    result_list = issue.similar_issues_list.split(',')
    result_list_info = []
    for method in result_list:
        try:

            method_info = models.issue.objects.get(pk=method)#issue
            method_info.method_result_list = method_info.method_result_list.split(',')[:4] # 切片的原因是，需要特殊处理

            for i in range(len(method_info.method_result_list)):
                method_info.method_result_list[i] = models.API.objects.get(pk= method_info.method_result_list[i] )  #将method名，转换成 API对象


            method_info.location_result_list = method_info.location_result_list.split(',')

            for i in range(len(method_info.location_result_list)):
                method_info.location_result_list[i] = models.file.objects.get(pk=method_info.location_result_list[i])  # 将 file路径，转换成 file对象

        except:
            continue
        if method == Issuekey:
            continue
        else:
            result_list_info.append(method_info)

    result_list_info1 = result_list_info[:5]
    result_list_info2 = result_list_info[5:10]
    result_list_info3 = result_list_info[10:15]
    result_list_info4 = result_list_info[15:20]

    return render(request, "Similar_issues.html",
                  {"Issue": issue, "result_list1": result_list_info1, "result_list2": result_list_info2,
                   "result_list3": result_list_info3, "result_list4": result_list_info4})





def get_Candidate_locations(request, Issuekey):
    global issuekey
    issuekey = Issuekey

    # result_list_info指的是  location的result
    # 这里根据某一文件，对API进行重排序



    ############################################


    issue = models.issue.objects.get(pk=Issuekey)
    result_list = issue.location_result_list.split(',')
    result_list_info = []

    ###########################################
    # 申明以及初始化
    Candidate_API = {}
    ModifiedBy_issues = {}
    issues = models.issue.objects.all()
    for location in result_list:
        ModifiedBy_issues[location] = []
        Candidate_API[location] = []
    ###########################################




    method_list = issue.method_result_list.split(',')
    method_list_info = []
    for method in method_list:
        try:
            method_info = models.API.objects.get(pk=method)
            method_list_info.append(method_info)
        except:
            continue



    for location in result_list:
        try:
            location_info = models.file.objects.get(pk=location)
            location_info.Description = location_info.Description.split('*//**')[0] + '* /' + '\n ...'
            result_list_info.append(location_info)


            """
            #冲排序计算！
            ordered_dict = {}
            description = location_info.Description
            for method in method_list_info[:3]:  ##为了提速
                reponse = client.simnet(method.Description.encode('gbk', 'ignore'), description[:100].encode('gbk', 'ignore'), model_dict)

                if reponse.has_key('score'):
                    score = reponse['score']
                else:
                    score = 0
                ordered_dict[method] = score

            ordered_list = sorted(ordered_dict.iteritems(), key=lambda asd: asd[1], reverse=True)  # 列表类型，【 （key，value） 】
            ordered_list = [ele[0] for ele in ordered_list]
            Candidate_API [location_info.Dir] = ordered_list[:5]
            """

        except:
            continue

    result_list_info1 = result_list_info[:5]
    result_list_info2 = result_list_info[5:10]
    result_list_info3 = result_list_info[10:15]
    result_list_info4 = result_list_info[15:20]



    for iss in issues:
        if iss.Project == issue.Project:
            location_list = iss.location_result_list.split(',') #某一历史issue，中的location_result_list
            for location in location_list:
                if location in result_list:
                    if len(ModifiedBy_issues[location]) < 5:

                        # 将iss中的 used_method 换成数据对象：
                        try:
                            iss_result_list = iss.location_result_list.split(',')
                            iss_result_list_info = []
                            for location0 in iss_result_list:
                                try:
                                    location_info = models.file.objects.get(pk=location0)
                                    iss_result_list_info.append(location_info)
                                except:
                                    continue
                            iss.location_result_list = iss_result_list_info[:5]
                        except:
                            pass

                        # 将iss中的 used_method 换成数据对象：
                        try:
                                iss_result_list = iss.method_result_list.split(',')
                                iss_result_list_info = []
                                for method0 in iss_result_list:
                                    try:
                                        method_info = models.API.objects.get(pk=method0)
                                        iss_result_list_info.append(method_info)
                                    except:
                                        continue
                                iss.method_result_list = iss_result_list_info
                        except:
                                pass

                        if iss == issue:
                            continue
                        else:
                            #result_list_info.append(method_info)
                            # 存入字典中
                            ModifiedBy_issues[location] = ModifiedBy_issues[location] + [iss]


    return render(request, "Candidate_locations.html",
                  {"Issue": issue, "result_list1": result_list_info1, "result_list2": result_list_info2,
                   "result_list3": result_list_info3, "result_list4": result_list_info4, 'ModifiedBy_issues_dict': ModifiedBy_issues , 'Candidate_API_dict': Candidate_API})



def get_Candidate_APIs(request, Issuekey):


    issue = models.issue.objects.get(pk=Issuekey)
    result_list = issue.method_result_list.split(',')
    result_list_info = []
    for method in result_list:
        try:
            method_info = models.API.objects.get(pk=method)
            method_info.Example = method_info.Example.strip(' ').split(method_info.Name)
            result_list_info.append(method_info)
        except:
            continue


    result_list_info1 = result_list_info[:5]
    result_list_info2 = result_list_info[5:10]
    result_list_info3 = result_list_info[10:15]
    result_list_info4 = result_list_info[15:20]

    UsedBy_issues = {}
    issues = models.issue.objects.all()
    for method in result_list:
        UsedBy_issues[method] = []

    for iss in issues:
        if iss.Project == issue.Project:
            method_list = iss.method_result_list.split(',')   #某一历史issue，中的method_result_list
            for method in method_list:
                if method in result_list:
                    if len(UsedBy_issues[method]) < 3:

                        #将iss中的 used_method 换成数据对象：
                        try:
                            iss_result_list = iss.method_result_list.split(',')
                            iss_result_list_info = []
                            for method0 in iss_result_list:
                                try:
                                    method_info = models.API.objects.get(pk=method0)
                                    iss_result_list_info.append(method_info)
                                except:
                                    continue
                            iss.method_result_list = iss_result_list_info
                        except:
                            pass

                        #存入字典中
                        UsedBy_issues[method] = UsedBy_issues[method]+[iss]





    return render(request, "Candidate_APIs.html",
                  {"Issue": issue, "result_list1": result_list_info1, "result_list2": result_list_info2,"result_list3": result_list_info3, "result_list4": result_list_info4, 'UsedBy_issues_dict':UsedBy_issues})



def get_Reordered_Candidate_APIs(request, file, Issuekey):


    files = models.file.objects.get(Name=file)

    #global Issuekey

    issue = models.issue.objects.get(pk=Issuekey)
    result_list = issue.method_result_list.split(',')
    result_list_info = []
    for method in result_list:
        try:
            method_info = models.API.objects.get(pk=method)
            method_info.Example = method_info.Example.strip(' ').split(method_info.Name)
            result_list_info.append(method_info)
        except:
            continue


    #重排序
    ordered_dict = {}
    description = files.Description
    for method in result_list_info:  ##为了提速
        reponse = client.simnet(method.Description.encode('gbk', 'ignore'), description[:100].encode('gbk', 'ignore'),
                                model_dict)

        if reponse.has_key('score'):
            score = reponse['score']
        else:
            score = 0
        ordered_dict[method] = score

    ordered_list = sorted(ordered_dict.iteritems(), key=lambda asd: asd[1], reverse=True)  # 列表类型，【 （key，value） 】
    ordered_list = [ele[0] for ele in ordered_list]
    result_list_info = ordered_list[:]


    result_list_info1 = result_list_info[:5]
    result_list_info2 = result_list_info[5:10]
    result_list_info3 = result_list_info[10:15]
    result_list_info4 = result_list_info[15:20]

    UsedBy_issues = {}
    issues = models.issue.objects.all()
    for method in result_list:
        UsedBy_issues[method] = []

    for iss in issues:
        if iss.Project == issue.Project:
            method_list = iss.method_result_list.split(',')   #某一历史issue，中的method_result_list
            for method in method_list:
                if method in result_list:
                    if len(UsedBy_issues[method]) < 3:

                        #将iss中的 used_method 换成数据对象：
                        try:
                            iss_result_list = iss.method_result_list.split(',')
                            iss_result_list_info = []
                            for method0 in iss_result_list:
                                try:
                                    method_info = models.API.objects.get(pk=method0)
                                    iss_result_list_info.append(method_info)
                                except:
                                    continue
                            iss.method_result_list = iss_result_list_info
                        except:
                            pass

                        #存入字典中
                        UsedBy_issues[method] = UsedBy_issues[method]+[iss]

    return render(request, "Reordered_Candidate_APIs.html",
                  {"Issue": issue, "result_list1": result_list_info1, "result_list2": result_list_info2,"result_list3": result_list_info3, "result_list4": result_list_info4, 'UsedBy_issues_dict':UsedBy_issues})



def get_fileContent(request, Dir):
    try:
        file = models.file.objects.get(pk = Dir)

        return render(request, "file.html",{"Content": file.Content})
    except :
        return HttpResponse(Dir)







print 2