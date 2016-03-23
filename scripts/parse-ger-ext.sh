# please install ParZu, all necessary dependencies and adjust the locations accordingly
export PATH=$PATH:../ParZu/fst/ # FST Library used by ParZu's morphological analyser
PARSER_MODEL=../ParZu/parzu # command line executable of the ParZu parser

# please download this model and adjust their locations accordingly 
TOKEN_MODEL=models/token-ger.model # German tokenization model for OpenNLP
SRL_MODEL=models/srl-EMNLP14+fs-extger.model # German mateplus SRL model, trained on ParZu output

RERANKER="-reranker"
TOKENIZE="-tokenize -token $TOKEN_MODEL"

JAVA=/usr/lib/jvm/java-1.8.0/bin/java

#parse $1
$JAVA -cp "lib/opennlp-maxent-3.0.3.jar:lib/opennlp-tools-1.5.3.jar:lib/liblinear-1.51-with-deps.jar:lib/stanford-corenlp-3.5.1.jar:lib/transition-1.30.jar:lib/whatswrong-0.2.3.jar:mateplus.jar" -Xmx6g se.lth.cs.srl.CompletePipeline ger -parser $PARSER_MODEL -external -srl $SRL_MODEL $RERANKER $TOKENIZE -test $1
