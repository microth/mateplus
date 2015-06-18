# please download these models and adjust their locations accordingly
LEMMA_MODEL=models/lemma-ger.model
PARSER_MODEL=models/parse-ger.model # Bernd's joint tagging/parsing model
TOKEN_MODEL=models/token-ger.model # German tokenization model for OpenNLP
SRL_MODEL=models/srl-EMNLP14+fs-ger.model # German mateplus SRL model, trained on joint tagger/parser output

RERANKER="-reranker"
TOKENIZE="-tokenize -token $TOKEN_MODEL"

JAVA=/usr/lib/jvm/java-1.8.0/bin/java

#parse $1
$JAVA -cp "lib/opennlp-maxent-3.0.3.jar:lib/opennlp-tools-1.5.3.jar:lib/liblinear-1.51-with-deps.jar:lib/stanford-corenlp-3.5.0.jar:lib/transition-1.30.jar:lib/whatswrong-0.2.3.jar:mateplus.jar" -Xmx6g se.lth.cs.srl.CompletePipeline ger -lemma $LEMMA_MODEL -parser $PARSER_MODEL -hybrid -srl models/srl-EMNLP14+fs-ger.model $RERANKER $TOKENIZE -test $1
