package se.lth.cs.srl.features;

import java.util.Collection;
import java.util.Map;

import se.lth.cs.srl.corpus.CorefChain;
import se.lth.cs.srl.corpus.Corpus;
import se.lth.cs.srl.corpus.CorpusSentence;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.corpus.Word.WordData;

public class ProminenceFeature extends AttrFeature {
	private static final long serialVersionUID = 1L;

	protected ProminenceFeature(FeatureName name, WordData attr, TargetWord tw,
			String POSPrefix) {
		super(name, attr, tw, true, false, POSPrefix);
	}

	@Override
	protected void performFeatureExtraction(Sentence s, boolean allWords) {
		for (Predicate p : s.getPredicates()) {
			if (doExtractFeatures(p))
				for (Word arg : p.getArgMap().keySet()) {
					for (Word w : arg.getSpan())
						addMap(makeFeatureString(w));
				}
		}
	}

	private String makeFeatureString(Word arg) {
		Corpus c = ((CorpusSentence) arg.getMySentence()).getMyCorpus();
		double prominence = 1.0 - ((CorpusSentence) arg.getMySentence())
				.averageChainSize();

		String ccid = c.corefId(arg);
		int curr_snum = c.indexOf(arg.getMySentence());
		int curr_wnum = arg.getIdx();

		if (!ccid.equals("_"))
			for (String mention : c.chains.get(Integer.parseInt(ccid))) {
				String[] snum_wnum = mention.split("\\.");
				int snum = Integer.parseInt(snum_wnum[0]);
				int wnum = Integer.parseInt(snum_wnum[1]);
				if (snum > curr_snum)
					continue;
				if (snum < curr_snum - 2)
					continue; // only look at previous 2 sentences
				prominence += 1.0;
			}
		// System.err.println(c.getName());
		// System.err.println(ccid + " " + prominence);
		return NumFeature.bin((int) Math.round(prominence));
	}

	@Override
	public String getFeatureString(Sentence s, int predIndex, int argIndex) {
		Word w = wordExtractor.getWord(s, predIndex, argIndex);
		return makeFeatureString(w);
	}

	@Override
	public String getFeatureString(Predicate pred, Word arg) {
		Word w = wordExtractor.getWord(pred, arg);
		return makeFeatureString(w);

	}

	@Override
	public void addFeatures(Sentence s, Collection<Integer> indices,
			Map<Integer, Double> nonbinFeats, int predIndex, int argIndex,
			Integer offset, boolean allWords) {
		addFeatures(indices, getFeatureString(s, predIndex, argIndex), offset,
				allWords);

	}

	@Override
	public void addFeatures(Collection<Integer> indices,
			Map<Integer, Double> nonbinFeats, Predicate pred, Word arg,
			Integer offset, boolean allWords) {
		addFeatures(indices, getFeatureString(pred, arg), offset, allWords);
	}

	private void addFeatures(Collection<Integer> indices, String featureString,
			Integer offset, boolean allWords) {
		if (featureString == null)
			return;
		Integer i = indexOf(featureString);
		if (i != -1 && (allWords || i < predMaxIndex))
			indices.add(i + offset);
	}
}
