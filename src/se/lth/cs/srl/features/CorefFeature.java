package se.lth.cs.srl.features;

import java.util.Collection;
import java.util.Map;

import se.lth.cs.srl.corpus.CorpusSentence;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;

public class CorefFeature extends SingleFeature {
	private static final long serialVersionUID = 1L;
	/*
	 * Binary feature indicating whether word denotes an entity that is
	 * mentioned at several places in discourse
	 */
	public static final String SUBJ = "S";
	public static final String OBJ = "O";
	public static final String X = "X";
	public static final String NO = "N";

	boolean full = false;

	protected CorefFeature(boolean full, String POSPrefix) {
		super(full ? FeatureName.FullCoref : FeatureName.XNCoref, true, false,
				POSPrefix);
		this.full = full;
		indices.put(X, Integer.valueOf(1));
		indices.put(NO, Integer.valueOf(2));
		indices.put(SUBJ, Integer.valueOf(3));
		indices.put(OBJ, Integer.valueOf(4));
		indexcounter = 5;
	}

	@Override
	public void addFeatures(Sentence s, Collection<Integer> indices,
			Map<Integer, Double> nonbinFeats, int predIndex, int argIndex,
			Integer offset, boolean allWords) {
		indices.add(indexOf(getFeatureString(s, predIndex, argIndex)) + offset);
	}

	@Override
	public void addFeatures(Collection<Integer> indices,
			Map<Integer, Double> nonbinFeats, Predicate pred, Word arg,
			Integer offset, boolean allWords) {
		indices.add(indexOf(getFeatureString(pred, arg)) + offset);
	}

	@Override
	protected void performFeatureExtraction(Sentence s, boolean allWords) {
		// Do nothing, the map is constructed in the constructor.
	}

	@Override
	public String getFeatureString(Sentence s, int predIndex, int argIndex) {
		return getFeatureString((Predicate) s.get(predIndex), s.get(argIndex));
	}

	@Override
	public String getFeatureString(Predicate pred, Word arg) {
		CorpusSentence s = (CorpusSentence) arg.getMySentence();
		String cc = s.getMyCorpus().findChainInPrvSentence(
				s.getMyCorpus().corefId(arg), s, true);

		// if(!cc.equals("_") && pred.getArgMap().get(arg)!=null) {
		// System.out.print("Corpus " + s.getMyCorpus().getName() +
		// " / Sentence " + s.getMyCorpus().indexOf(s)+ " ");
		// System.out.println(pred.getSense()+"["+pred.getArgMap().get(arg)+"]: "
		// + arg.getForm() + "(previous sentence: "+cc+")");
		// }

		// String cc = ((CorpusSentence)s).getMyCorpus().corefId(arg);
		// System.out.println(cc);
		if (cc.equals("_"))
			return NO;
		else if (!full)
			return X;
		else if (cc.contains("SBJ") || cc.contains("SUBJ"))
			return SUBJ;
		else if (cc.contains("OBJ"))
			return OBJ;
		else
			return X;
	}

}
