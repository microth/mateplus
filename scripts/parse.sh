# please download these models and adjust their locations accordingly
LEMMA_MODEL=models/lemma-eng.model
POS_MODEL=models/tagger-eng.model
PARSER_MODEL=models/parse-eng.model

RERANKER="-reranker"
TOKENIZE="-tokenize"

JAVA=/usr/lib/jvm/java-1.8.0/bin/java

#parse $1
$JAVA -cp "lib/*:bin/" -Xmx6g se.lth.cs.srl.CompletePipeline eng -lemma $LEMMA_MODEL -parser $PARSER_MODEL -tagger $POS_MODEL -srl models/srl-EMNLP14+fs-eng.model $RERANKER $TOKENIZE -test $1
