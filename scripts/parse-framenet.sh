JAVA=/usr/lib/jvm/java-1.8.0/bin/java

# directory to which fndata-1.5 was extracted
FRAMENETDIR=/disk/scratch/framenet/fndata-1.5/

# directory in which glove10a was extracted
# (please also modify processtmp.sh in glove dir accordingly!)
GLOVEDIR=/disk/scratch/glove/ 

$JAVA -Xmx8g -cp lib/*:mateplus.jar se.lth.cs.srl.CompletePipeline eng -lemma models/lemma-eng.model -parser models/parse-eng.model -tagger models/tagger-eng.model -srl models/srl-TACL15-eng.model -glove $GLOVEDIR -reranker -semafor stkilda:8043 -mst stkilda:12345 -framenet $FRAMENETDIR -test $1
