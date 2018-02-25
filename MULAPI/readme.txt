

API及其使用推荐项目的代码
（第一次用python写的工具，较凌乱，汗‘’‘）

Experiment:
	para_trainingResult0.txt中记录了feature location中参数训练的结果
	para_trainingResult1.txt中记录了API recommendation中参数训练的结果

Result:
	API_Usage_Location_experiment_result: the result of API usage location 			recommendation 
	API_recommendation_experiments_result: the result of API methods recommendation 

Tool:
	tool_mac : 在本机上对功能进行实现,以及一些数据处理以及准备工作
	tool_win : 在实验室服务器上进行大批量的实验所用，参数训练以及测试等运算量较大的工程。
	（二者之间无较大的差别，仅仅用途有所不同，功能实现上略有差异而已）
	
	功能实现：
	computeSimilarity.py：实现文本相似度计算的一些功能
	evaluate.py：实现评估指标的计算

	getStrucCmptScors.py：从源文件中信息出发，与issue report信息进行相似度计算，实现特征定位
	getSimilarityScores2Reports.py：从历史角度，计算相似的issue report，以致推荐出使用的API以及	设计的源代码文件等
	getFeatureLocation_result.py：将特征定位的结果进行汇总，得到最后的结果
	
	getAPISrcfileScores.py：用于数据准备工作，得到API description与feature location结果相关的	源文件s的相似度分数，便于后期实验
	getAPIdscpScors.py：用于数据准备工作，得到API description与issue report中的相似度分数，便于	后期实验
	getRecmdAPI_result.py：将API推荐的结果汇总，得到API推荐的最后结果

	Set_training.py：用于训练集的参数训练
	Set_testing.py：用于测试集的参数测试
	trainModel2WordSimilarity.py：利用词嵌技术，实现计算出两个词的相似度
	
	数据准备：
	extrPatchInfo.py：issue解决后，从提交的Attachments（diff）即 .patch文件中提取有用的信息（使	用的API，涉及的文件等）
	getUsedAPI.py：上一文件的分支（提取使用的API）
	extrSrcFileInfo.py：对java源文件进行解析，提取java文件中的关键信息（主要是：javalang包的使用）
	getAPISrcfileScores.py：用于数据准备工作，得到API description与feature location结果相关的	源文件s的相似度分数，便于后期实验
	getAPIdscpScors.py：用于数据准备工作，得到API description与issue report中的相似度分数，便于	后期实验

愿安好
	