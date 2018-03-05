# -*- coding: utf-8 -*-
"""
Created on Mon Mar 05 17:05:42 2018

@author: xcy
用遗传算法进行参数训练
"""

print 'start'
import evaluate
import getSimilarityScores2Reports
import getStrucCmptScors
import getVersionHisScors
import getReporterInfoScors
import csv
import getFeatureLocation_result
import math

print 'Importing is done!'
getSimilarityScores2Reports.init()
getVersionHisScors.init()
getReporterInfoScors.init()
getStrucCmptScors.init()
print 'Initing is done!'

poj = 'HadoopHDFS'

#功能简化型调参数
def getevaluate(weights):

    Result_dict = getFeatureLocation_result.getFinal_Result(VersionHis_result, Similarreports_result , Structure_result ,Reporter_result, weights)
    
    Aimresult=getAimList()
    #MAP = evaluate.main(Aimresult,Result_dict)
    MAP,MRR,k1,k5,k10 = evaluate.main_All(Aimresult,Result_dict)
    ObjFunction = math.e**(MAP+MRR)
    return ObjFunction

#为了减少I/O，定向制作

#为针对  csv文件单独写的 ,用于特征定位调参数   
def getAimList():
    #准备设计成 字典，以issuekey 作为键   ，  其aimresulr  为值
    Aimresult={}
    with open("Input/"+poj+"_Attachments_PatchInfo.csv","r") as csvfile:
        reader = csv.reader(csvfile)
        #这里不需要readlines
        for i,rows in enumerate(reader):
            #if i <20 :
                aimresult = rows[1:]
                Aimresult[rows[0]] = aimresult
            #else:
             #   break
    return Aimresult

#c=1
weights = [1, 1, 1, 1]

#weights = [1,  0.39589087254670696,       0, 0, 1, 0.4, 0.6, 0, 0.4, 1, 
#           1 ,1]
#0 1 调餐  两个角度feature location的结果
#2 3 为0     4为1  5 6 7 调参数   8为1    
#后二默认不调参


#All_result,issuekey_file_num,issuekey_file_list = getSimilarityScores2Reports.getAll_Info()
Similarreports_result = getSimilarityScores2Reports.main()
Structure_result = getStrucCmptScors.main()
VersionHis_result = getVersionHisScors.main()
Reporter_result = getReporterInfoScors.main()



"""
print 1
print 2
solutionnow =weights 
valuenow = getevaluate(solutionnow)#目标函数解
print valuenow


best = 0.0
for count in range(6):
#for index in [1] + range(3,9) + range(10,17):
    #for index in range(len(solutionnow)):
    for index in range(len(solutionnow)):
        i=1.0
        best_i = solutionnow[index]
        while i>0.005: 
            solutionnow[index] = i
            valuenew = getevaluate(solutionnow)#目标函数解
            if valuenew> valuenow:
                valuenow = valuenew
                best = valuenow
                best_i = i
            i = i - 0.1
           
        solutionnow[index]  = best_i
        print index
    
    print count
    print valuenow
    print solutionnow

print valuenow
print solutionnow
"""


# Types
from deap import base, creator

creator.create("FitnessMin", base.Fitness, weights=(1.0,))
# weights 1.0, 求最大值,-1.0 求最小值
# (1.0,-1.0,)求第一个参数的最大值,求第二个参数的最小值
creator.create("Individual", list, fitness=creator.FitnessMin)

# Initialization
import random
from deap import tools

IND_SIZE = 4  

toolbox = base.Toolbox()
toolbox.register("attribute", random.random)
# 调用randon.random为每一个基因编码编码创建 随机初始值 也就是范围[0,1]
toolbox.register("individual", tools.initRepeat, creator.Individual,
                 toolbox.attribute, n=IND_SIZE)
toolbox.register("population", tools.initRepeat, list, toolbox.individual)


# Operators
# difine evaluate function
# Note that a comma is a must
def evaluate0(individual):
    return getevaluate(individual),


# use tools in deap to creat our application
toolbox.register("mate", tools.cxTwoPoint) # mate:交叉
toolbox.register("mutate", tools.mutGaussian, mu=0, sigma=1, indpb=0.1) # mutate : 变异
toolbox.register("select", tools.selTournament, tournsize=3) # select : 选择保留的最佳个体
toolbox.register("evaluate0", evaluate0)  # commit our evaluate


# Algorithms
def main():
    # create an initial population of 300 individuals (where
    # each individual is a list of integers)
    pop = toolbox.population(n=50)
    CXPB, MUTPB, NGEN = 0.5, 0.2, 200

    '''
    # CXPB  is the probability with which two individuals
    #       are crossed
    #
    # MUTPB is the probability for mutating an individual
    #
    # NGEN  is the number of generations for which the
    #       evolution runs
    '''

    # Evaluate the entire population
    fitnesses = map(toolbox.evaluate0, pop)
    for ind, fit in zip(pop, fitnesses):
        ind.fitness.values = fit

    print("  Evaluated %i individuals" % len(pop))  # 这时候，pop的长度还是300呢
    print("-- Iterative %i times --" % NGEN)

    for g in range(NGEN):
        if g % 10 == 0:
            print("-- Generation %i --" % g)
        # Select the next generation individuals
        offspring = toolbox.select(pop, len(pop))
        # Clone the selected individuals
        offspring = list(map(toolbox.clone, offspring))
        # Change map to list,The documentation on the official website is wrong

        # Apply crossover and mutation on the offspring
        for child1, child2 in zip(offspring[::2], offspring[1::2]):
            if random.random() < CXPB:
                toolbox.mate(child1, child2)
                del child1.fitness.values
                del child2.fitness.values

        for mutant in offspring:
            if random.random() < MUTPB:
                toolbox.mutate(mutant)
                del mutant.fitness.values

        # Evaluate the individuals with an invalid fitness
        invalid_ind = [ind for ind in offspring if not ind.fitness.valid]
        fitnesses = map(toolbox.evaluate0, invalid_ind)
        for ind, fit in zip(invalid_ind, fitnesses):
            ind.fitness.values = fit

        # The population is entirely replaced by the offspring
        pop[:] = offspring

    print("-- End of (successful) evolution --")

    best_ind = tools.selBest(pop, 1)[0]

    return best_ind, best_ind.fitness.values  # return the result:Last individual,The Return of Evaluate function


if __name__ == "__main__":
    # t1 = time.clock()
    best_ind, best_ind.fitness.values = main()
    # print(pop, best_ind, best_ind.fitness.values)
    # print("pop",pop)
    print("best_ind",best_ind)
    print("best_ind.fitness.values",best_ind.fitness.values)
    
    Result_dict = getFeatureLocation_result.getFinal_Result(VersionHis_result, Similarreports_result , Structure_result ,Reporter_result, weights)
    Aimresult=getAimList()
    print evaluate.main_All(Aimresult,Result_dict)
    print getevaluate(best_ind)
    

    # t2 = time.clock()

    # print(t2-t1)






