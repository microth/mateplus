#!/bin/sh

## There are three sets of options that need, may need to, and could be changed.
## (1) deals with input and output. You have to set these (in particular, you need to provide a training corpus)
## (2) deals with the jvm parameters and may need to be changed
## (3) deals with the behaviour of the system

#0;136;0c# For further information on switches, see the source code, or run
## java -cp srl.jar se.lth.cs.srl.Learn --help

##################################################
## (1) The following needs to be set appropriately
##################################################
FRAMENETDIR="/disk/scratch/framenet/fndata-1.5/" # adjust this to your FN dir
CORPUS="annotations/fn15_train+dev_multFEs_MST.conll"
Lang="eng"
MODEL="srl-TACL15-nocontext-srl.model"

if [ "$1" != "" ]; then
  CORPUS=$1;
fi

##################################################
## (2) These ones may need to be changed
##################################################
JAVA="/usr/lib/jvm/java-1.8.0-sun/bin/java"
#JAVA="java" #Edit this i you want to use a specific java binary.
MEM="20g -XX:-UseGCOverheadLimit" #Memory for the JVM, might need to be increased for large corpora.
CP="lib/*:mateplus.jar"
JVM_ARGS="-cp $CP -Xmx$MEM"

##################################################
## (3) The following changes the behaviour of the system
##################################################
RERANKER="-reranker -framenet $FRAMENETDIR -fdir featuresets/framat-nocontext/"

#Execute
CMD="$JAVA $JVM_ARGS se.lth.cs.srl.Learn $Lang $CORPUS $MODEL $DEBUG $RERANKER $LLBINARY"
if [ "$CLUSTER" != "" ]; then
    CMD="$CMD -cluster $CLUSTER"
fi
if [ "$EMBEDDING" != "" ]; then
    CMD="$CMD -embedding $EMBEDDING"
fi

echo "Executing: $CMD"
$CMD
