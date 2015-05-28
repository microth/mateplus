package se.lth.cs.srl.features;

import java.util.List;

import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.corpus.Word.WordData;

public class DepBasedPathFeature extends SingleFeature {
	private static final long serialVersionUID = 1L;

	private static final String UP="0";
	private static final String DOWN="1";
	
	private WordData attr;
	
	protected DepBasedPathFeature(FeatureName name,WordData attr,String POSPrefix) {
		super(name,true,false,POSPrefix);
		this.attr=attr;
	}

	@Override
	protected void performFeatureExtraction(Sentence s, boolean allWords) {
		for(Predicate pred:s.getPredicates()){
			if(doExtractFeatures(pred))
				for(Word arg:pred.getArgMap().keySet()){
					addMap(getFeatureString(pred,arg));
				}
		}
	}

	@Override
	public String getFeatureString(Sentence s, int predIndex, int argIndex) {
		return makeFeatureString(s.get(predIndex),s.get(argIndex));
	}
	@Override
	public String getFeatureString(Predicate pred, Word arg) {
		return makeFeatureString(pred,arg);
	}
	public String makeFeatureString(Word pred,Word arg){
		boolean up=true;
		List<Word> path=Word.findPath(pred,arg);
		if(path.size()==0)
			return " ";
		StringBuilder ret=new StringBuilder();
		for(int i=0;i<(path.size()-1);++i){
			Word w=path.get(i);
			ret.append(w.getAttr(attr));
			if(up){
				if(w.getHead()==path.get(i+1)) { //Arrow up
					ret.append(UP);
				} else { //Arrow down
					ret.append(DOWN);
					up=false;
				}
			} else {
				ret.append(DOWN);
			}
		}
		return ret.toString();		
	}
}
