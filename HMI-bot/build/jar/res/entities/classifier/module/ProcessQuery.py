import os
import sys
import _pickle as cPickle
from nltk.stem import SnowballStemmer
from nltk import word_tokenize
from sklearn.metrics.pairwise import linear_kernel
import glob

domain=sys.argv[1]
scriptDir=os.path.dirname(__file__)
picklePath=os.path.join(scriptDir,'..','model',domain+'_')

intent=cPickle.load(open(picklePath+'intent.m','rb'))
utterance=cPickle.load(open(picklePath+'utterance.m','rb'))
tfidfVec=cPickle.load(open(picklePath+'tfidfVec.m','rb'))
svd=cPickle.load(open(picklePath+'svd.m','rb'))
trainLSA=cPickle.load(open(picklePath+'trainLSA.m','rb'))
 
def replace_nth(string,sub,repl,nth):
 find=string.find(sub)
 i=find!=-1
 while find!=-1 and i!=nth:
  find=string.find(sub,find+1)
  i+=1
 if i==nth:
  return string[:find]+repl+string[find+len(sub):]
 return string
def wordReplacer(utter,matchedDict,combinations):
 matchedDict=matchedDict.copy()
 while(len(matchedDict)>0):
  replacement=matchedDict.popitem()
  for wordReplacement in replacement[1]['synonym']:
   new_utter=utter.replace(replacement[0],wordReplacement)
   combinations.append(new_utter)
   wordReplacer(new_utter,matchedDict,combinations) 
def genSentences(utter,matchedDict,combinations):
 matchedDict=matchedDict.copy() 
 while(len(matchedDict)>0):
  replacement=matchedDict.popitem()
  for count in range(replacement[1]['count']):
   for wordReplacement in replacement[1]['synonym']:
    new_utter=replace_nth(utter,replacement[0],wordReplacement,count+1)
    combinations.append(new_utter)
    wordReplacer(new_utter,matchedDict,combinations)
def processUtterance(utter):
 scoreList={}
 idList={}
 for query in utter:
  query=query.lower()
  query=[query]
  test=tfidfVec.transform(query).toarray()
  LSATest=svd.transform(test)
  cosineSimilarities=linear_kernel(LSATest,trainLSA).flatten()
  related_docs_indices=cosineSimilarities.argsort()[::-1]
  for i in range(len(related_docs_indices)):
   fID=related_docs_indices[i] 
   fScore=cosineSimilarities[fID]
   fIntent=intent[related_docs_indices[i]]
   if(fIntent in scoreList):
    scoreList[fIntent]=max(fScore,scoreList[fIntent])
    if(fScore>cosineSimilarities[idList.get(fIntent)]):
     idList[fIntent]=fID
   else:
    scoreList[fIntent]=fScore
    idList[fIntent]=fID
 orderedIntents=sorted(scoreList,key=scoreList.get,reverse=True)
 response={'class_1':orderedIntents[0],'score_1':"{:.2f}".format(scoreList[orderedIntents[0]])}
 return response
synonymFile=os.path.join(scriptDir,'..','..','..','dictionary','synonyms_en.txt')
with open(synonymFile,'r')as rawSynonymsFileobj:
 rawSynonyms=rawSynonymsFileobj.read()
 rawSynonyms=rawSynonyms.split('\n')
synonymsList=[]
for i in rawSynonyms:
 synonymsList.append(i.split(','))
def genUtterances(utter):
 matched={}
 utteranceSet=set(utter.split())
 for synonym in synonymsList:
  for word in set(synonym)&utteranceSet:
   count=utter.split().count(word)
   matched[word]={'synonym':list(set(synonym)-set([word])),'count':count}
 combinations=[utter]
 genSentences(utter,matched,combinations)
 combinations.sort()
 return combinations