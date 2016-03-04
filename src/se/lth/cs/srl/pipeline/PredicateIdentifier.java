package se.lth.cs.srl.pipeline;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.TreeMap;

import se.lth.cs.srl.Learn;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.features.Feature;
import se.lth.cs.srl.features.FeatureSet;
import se.lth.cs.srl.languages.German;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.languages.Language.L;
import uk.ac.ed.inf.srl.ml.LearningProblem;
import uk.ac.ed.inf.srl.ml.Model;

public class PredicateIdentifier extends AbstractStep {

	private static final String FILEPREFIX = "pi_";

	public PredicateIdentifier(FeatureSet fs) {
		super(fs);
	}

	public void extractInstances(Sentence s) {
		/*
		 * We add an instance if it 1) Is a predicate. Then either to its
		 * specific classifier, or the fallback one. (if fallback behavior is
		 * specified, i.e. skipNonMatchingPredicates=false 2) Is not a
		 * predicate, but matches the POS-tag
		 */
		for (int i = 1, size = s.size(); i < size; ++i) {
			Word potentialPredicate = s.get(i);
			String POS = potentialPredicate.getPOS();
			String POSPrefix = null;
			for (String prefix : featureSet.POSPrefixes) {
				if (POS.startsWith(prefix)) {
					POSPrefix = prefix;
					break;
				}
			}
			if (POSPrefix == null) { // It matches a prefix, we will use it for
										// sure.
				if (!Learn.learnOptions.skipNonMatchingPredicates
						&& potentialPredicate instanceof Predicate) {
					POSPrefix = featureSet.POSPrefixes[0];
				} else {
					continue; // Its just some word we dont care about
				}
			}
			Integer label = potentialPredicate instanceof Predicate ? POSITIVE
					: NEGATIVE;
			addInstance(s, i, POSPrefix, label);
		}
	}

	private void addInstance(Sentence s, int i, String POSPrefix, Integer label) {
		LearningProblem lp = learningProblems.get(POSPrefix);
		Collection<Integer> indices = new TreeSet<Integer>();
		Map<Integer, Double> nonbinFeats = new TreeMap<Integer, Double>();
		Integer offset = 0;
		for (Feature f : featureSet.get(POSPrefix)) {
			f.addFeatures(s, indices, nonbinFeats, i, -1, offset, true);
			offset += f.size(true);
		}
		lp.addInstance(label, indices, nonbinFeats);
	}

	public void parse(Sentence s) {
		boolean containspreds = false;
		for (int i = 1, size = s.size(); i < size; ++i) {
			Integer label = classifyInstance(s, i);

			if (label.equals(POSITIVE)
					|| (Language.getLanguage() instanceof German && s.get(i)
							.getPOS().startsWith("VV"))) {
				s.makePredicate(i);
				containspreds = true;
			}
		}

		if ((Language.getLanguage() instanceof German)) {
			// Set<Word> heads = s.get(0).getChildren();
			// OUTER: for(Word w : heads) {
			// if(w.getLemma().equals("sein")) {
			// for(Word c : w.getChildren()) {
			for (int i = 1, size = s.size(); i < size; ++i) {
				if(s.get(i) instanceof Predicate) continue;
				
				Word c = s.get(i);
				if (c.getDeprel().equals("PD") || c.getDeprel().equals("pred")) {
					s.makePredicate(c.getIdx());
					// break OUTER;
				}
				// }
				// }
			}
		}
	}

	private Integer classifyInstance(Sentence s, int i) {
		String POSPrefix = null;
		String POS = s.get(i).getPOS();
		for (String prefix : featureSet.POSPrefixes) {
			if (POS.startsWith(prefix)) {
				POSPrefix = prefix;
				break;
			}
		}
		if (POSPrefix == null)
			return NEGATIVE;
		Model m = models.get(POSPrefix);
		Collection<Integer> indices = new TreeSet<Integer>();
		Map<Integer, Double> nonbinFeats = new TreeMap<Integer, Double>();
		Integer offset = 0;
		for (Feature f : featureSet.get(POSPrefix)) {
			f.addFeatures(s, indices, nonbinFeats, i, -1, offset, true);
			offset += f.size(true);
		}
		return m.classify(indices, nonbinFeats);
	}

	@Override
	public void prepareLearning() {
		super.prepareLearning(FILEPREFIX);
	}

	@Override
	protected String getModelFileName() {
		return FILEPREFIX + ".models";
	}

	@Override
	public void prepareLearning(int i) {
		prepareLearning();
	}

}
