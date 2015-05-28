package se.lth.cs.srl.util.scorer;

import java.util.HashSet;
import java.util.Set;

import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;

public class PredicateDisambiguationScorer extends AbstractScorer {

	
	public static double score(Sentence gold, Sentence parsed) {
		if(gold.getPredicates().size()==0 && parsed.getPredicates().size()==0)
			return 1;
		int tp=0,fp=0;
		for(Predicate pred:parsed.getPredicates()){
			int index=parsed.indexOf(pred);
			if(gold.get(index) instanceof Predicate && ((Predicate)gold.get(index)).getSense().equals(pred.getSense())) {
				tp++;
			} else {
				fp++;
			}
		}
		double p=(double) tp/(tp+fp);
		double r=(double) tp/(tp+fp);
		if(p+r>0)
			return 2*p*r/(p+r);
		else
			return 0;		
	}

	@Override
	public double computeScore(Sentence gold, Sentence parsed) {
		return score(gold,parsed);
	}

	
	
}
