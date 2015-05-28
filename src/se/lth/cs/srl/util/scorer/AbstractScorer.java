package se.lth.cs.srl.util.scorer;

import se.lth.cs.srl.corpus.Sentence;

public abstract class AbstractScorer {

	private int count=0;
	private double accScore=0;
	
	public double getAccumulatedScore() {
		return accScore;
	}

	public double getAvgScore() {
		return (double) accScore/count;
	}

	public int getCount() {
		return count;
	}

	public void reset() {
		count=0;
		accScore=0;
	}

	public double accScore(Sentence gold,Sentence parsed){
		double score=computeScore(gold,parsed);
		accScore+=score;
		count++;
		return score;
	}
	public abstract double computeScore(Sentence gold,Sentence parsed);
	
}
