package se.lth.cs.srl.features;

import java.util.LinkedList;
import java.util.List;

import se.lth.cs.srl.corpus.Corpus;
import se.lth.cs.srl.corpus.CorpusSentence;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.corpus.Word.WordData;

public class DiscourseDistanceFeature extends SetFeature {
	private static final long serialVersionUID = 1L;

	WordData attr;
	
	protected DiscourseDistanceFeature(FeatureName name,WordData attr, boolean usedForPredicateIdentification,String POSPrefix) {
		super(name,false,usedForPredicateIdentification,POSPrefix);
		this.attr=attr;
	}
	
	@Override
	public String[] getFeatureStrings(Sentence s, int predIndex, int argIndex) {
		return makeFeatureStrings(s.get(argIndex)); 
	}

	@Override
	public String[] getFeatureStrings(Predicate pred, Word arg) {
		return makeFeatureStrings(arg);
	}
	
	private String[] makeFeatureStrings(Word arg){
		if(arg==null) return new String[0];
		
		Corpus c = ((CorpusSentence)arg.getMySentence()).getMyCorpus();
		int curr_snum = c.indexOf(arg.getMySentence());
		int curr_wnum = arg.getIdx();
		
		if((arg.getPOS().equals("IN") || arg.getPOS().equals("TO")) && !arg.getChildren().isEmpty())
			arg = arg.getChildren().iterator().next();
		
		String ccid = c.corefId(arg);
	
		if(ccid.equals("_")) 			
			return new String[0];
		
		int distance = 100;
		for(String mention : c.chains.get(Integer.parseInt(ccid))) {
			String[] snum_wnum = mention.split("\\.");
			int snum = Integer.parseInt(snum_wnum[0]);
			int wnum = Integer.parseInt(snum_wnum[1]);
			
			if(snum>curr_snum) continue;
			if(snum==curr_snum && wnum>=curr_wnum) continue;
			
			int newdist = curr_snum-snum;
			if(newdist>5)
				newdist=5;
			if(newdist<distance)
				distance = newdist; 
		}
		
		if(distance==100)
			return new String[0];
		
		return new String[]{"D"+distance};		
	}

	@Override
	protected void performFeatureExtraction(Sentence curr_s, boolean allWords) {
		for(int i=1,size=curr_s.size();i<size;++i) {
			Word arg = curr_s.get(i);
			Corpus c = ((CorpusSentence)arg.getMySentence()).getMyCorpus();

			int curr_snum = c.indexOf(arg.getMySentence());
			int curr_wnum = arg.getIdx();
			
			if((arg.getPOS().equals("IN") || arg.getPOS().equals("TO")) && !arg.getChildren().isEmpty())
				arg = arg.getChildren().iterator().next();
			
			String ccid = c.corefId(arg);
		
			if(ccid.equals("_")) continue;
			
			int distance = 100;
			for(String mention : c.chains.get(Integer.parseInt(ccid))) {
				String[] snum_wnum = mention.split("\\.");
				int snum = Integer.parseInt(snum_wnum[0]);
				int wnum = Integer.parseInt(snum_wnum[1]);
				
				if(snum>curr_snum) continue;
				if(snum==curr_snum && wnum>=curr_wnum) continue;
				
				int newdist = curr_snum-snum;
				if(newdist>5)
					newdist=5;
				if(newdist<distance)
					distance = newdist; 
			}
			
			if(distance==100) continue;
			
			//System.err.println("D"+distance);
			addMap("D"+distance);		
		}
	}
}
