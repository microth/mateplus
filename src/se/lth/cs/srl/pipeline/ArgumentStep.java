package se.lth.cs.srl.pipeline;

import java.util.Collection;
import java.util.TreeSet;
import java.util.Map;
import java.util.TreeMap;

import se.lth.cs.srl.Learn;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.features.Feature;
import se.lth.cs.srl.features.FeatureSet;
import uk.ac.ed.inf.srl.ml.LearningProblem;
import uk.ac.ed.inf.srl.ml.Model;

public abstract class ArgumentStep extends AbstractStep {

	public ArgumentStep(FeatureSet fs) {
		super(fs);
	}
	
	protected abstract Integer getLabel(Predicate pred,Word arg);

	protected String getPOSPrefix(String pos) {
		for(String prefix:featureSet.POSPrefixes){
			if(pos.startsWith(prefix))
				return prefix;
		}
		return null;
	}
	
	//TODO this way we lookup the POSPrefix several times for the same predicate. Maybe the second parameter should be Iterable<Word> ?
	protected void addInstance(Predicate pred, Word arg) {
		String POSPrefix=getPOSPrefix(pred.getPOS());
		if(POSPrefix==null){
			if(Learn.learnOptions.skipNonMatchingPredicates){
				return;
			} else {
				POSPrefix=featureSet.POSPrefixes[0];
			}
		}
		LearningProblem lp=learningProblems.get(POSPrefix);

		Collection<Integer> indices = new TreeSet<Integer>();
		Map<Integer, Double> nonbinFeats = new TreeMap<Integer, Double>();
		collectFeatures(pred,arg,POSPrefix,indices, nonbinFeats);

		if(POSPrefix!=null)
		    lp.addInstance(getLabel(pred,arg), indices, nonbinFeats);
	}

    protected void collectFeatures(Predicate pred, Word arg, String POSPrefix, Collection<Integer> indices, Map<Integer, Double> nonbinFeats) {
        if(POSPrefix==null)
            return;

	Integer offset=0;
	for(Feature f:featureSet.get(POSPrefix)){
	    f.addFeatures(indices, nonbinFeats, pred, arg, offset, false);
	    offset+=f.size(false);
	}
	return;
    }

	//TODO same thing as above.
	public Integer classifyInstance(Predicate pred, Word arg) {
		String POSPrefix=getPOSPrefix(pred.getPOS());
		if(POSPrefix==null){
			POSPrefix=featureSet.POSPrefixes[0];
			//System.out.println("Unknown POS-tag for predicate '"+pred.getForm()+"', falling back to "+POSPrefix);
		}		
		Model m=models.get(POSPrefix);

		Collection<Integer> indices = new TreeSet<Integer>();
		Map<Integer, Double> nonbinFeats = new TreeMap<Integer, Double>();
		collectFeatures(pred,arg,POSPrefix,indices, nonbinFeats);

		return m.classify(indices, nonbinFeats);
	}
}
