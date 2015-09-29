package se.lth.cs.srl.fs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import se.lth.cs.srl.Learn;
import se.lth.cs.srl.corpus.Corpus;
import se.lth.cs.srl.corpus.CorpusSentence;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.features.Feature;
import se.lth.cs.srl.features.FeatureFile;
import se.lth.cs.srl.features.FeatureGenerator;
import se.lth.cs.srl.features.FeatureName;
import se.lth.cs.srl.features.FeatureSet;
import se.lth.cs.srl.fs.SelectFeatures.CorpusStruct;
import se.lth.cs.srl.io.AllCoNLL09Reader;
import se.lth.cs.srl.io.CoNLL09Writer;
import se.lth.cs.srl.io.DepsOnlyCoNLL09Reader;
import se.lth.cs.srl.io.SentenceReader;
import se.lth.cs.srl.io.SentenceWriter;
import se.lth.cs.srl.options.FeatureSelectionOptions;
import se.lth.cs.srl.pipeline.Pipeline;
import se.lth.cs.srl.pipeline.PredicateDisambiguator;
import se.lth.cs.srl.pipeline.Step;
import se.lth.cs.srl.util.BrownCluster;
import se.lth.cs.srl.util.WordEmbedding;
import se.lth.cs.srl.util.scorer.AbstractScorer;
import se.lth.cs.srl.util.scorer.ArgumentClassificationScorer;
import se.lth.cs.srl.util.scorer.ArgumentIdentificationScorer;
import se.lth.cs.srl.util.scorer.PredicateDisambiguationScorer;
import se.lth.cs.srl.util.scorer.PredicateIdentificationScorer;

public class SelectFeaturesRR {

	public class SelectionState {
		// public Map<Step,FeatureSet> featureSets;
		public Map<String, List<Feature>> current;
		// public List<Feature> current;
		public List<Feature> additional;
		public double score = 0;
		public List<String> comments;
	}

	public static class CorpusStruct {
		// public static File corpusDir;
		public static List<File> parts;
		public static List<List<Sentence>> trainingSets;
		public static List<List<Sentence>> testSets;
	}

	private FeatureSelectionOptions options;
	private FeatureGenerator fg;

	public static void printMemUsage() {
		System.gc();
		long free = Runtime.getRuntime().freeMemory();
		long total = Runtime.getRuntime().totalMemory();
		long used = total - free;
		System.out.println("Total memory: " + total / 1024 + "kb");
		System.out.println("Free memory:  " + free / 1024 + "kb");
		System.out.println("Used memory:  " + used / 1024 + "kb");
	}

	public static void main(String[] args) throws IOException {
		new SelectFeaturesRR(new FeatureSelectionOptions(args));
	}

	private SelectFeaturesRR(FeatureSelectionOptions options)
			throws IOException {
		this.options = options;
		Learn.learnOptions = options.getLearnOptions();
		BrownCluster bc = Learn.learnOptions.brownClusterFile == null ? null
				: new BrownCluster(Learn.learnOptions.brownClusterFile);
		WordEmbedding we = Learn.learnOptions.wordEmbeddingFile == null ? null
				: new WordEmbedding(Learn.learnOptions.wordEmbeddingFile);
		File corpusDir = new File(options.tempDir, "corpora");
		File ffDir = new File(options.tempDir, "features");
		corpusDir.mkdir();
		ffDir.mkdir();
		printMemUsage();
		List<Sentence> sentences = readSentences(options);
		printMemUsage();
		// FeatureGenerator fg=createFeatureGenerator(sentences,options);
		fg = new FeatureGenerator();
		// Here cross train prepipelines if step is not PI and preannotate
		// testing corpora or this
		List<Feature> startingFeatures = getStartSetFromFile(fg, bc);
		SelectionState startState = getStartingState(fg, startingFeatures, bc);
		printMemUsage();
		fg.buildFeatureMaps(sentences);
		printMemUsage();

		if (options.testCorpus == null) {
			List<List<Sentence>> partitions = partitionSentences(sentences,
					options);
			printMemUsage();
			CorpusStruct.parts = new ArrayList<File>();
			for (int i = 0, size = partitions.size(); i < size; ++i) {
				File save = new File(corpusDir, "gold-" + i);
				CorpusStruct.parts.add(save);
				SentenceWriter writer = new CoNLL09Writer(save);
				for (Sentence s : partitions.get(i)) {
					writer.specialwrite(s);
				}
				writer.close();
			}
			printMemUsage();
			CorpusStruct.testSets = partitions;
			CorpusStruct.trainingSets = new ArrayList<List<Sentence>>();
			for (int i = 0, size = partitions.size(); i < size; ++i) {
				List<Sentence> trainSet = new ArrayList<Sentence>();
				for (int j = 0; j < size; ++j) {
					printMemUsage();
					if (i != j) {
						trainSet.addAll(partitions.get(j));
					}
				}
				CorpusStruct.trainingSets.add(trainSet);
			}
		} else {
			CorpusStruct.testSets = new LinkedList<List<Sentence>>();
			CorpusStruct.trainingSets = new LinkedList<List<Sentence>>();
			CorpusStruct.trainingSets.add(sentences);

			FeatureSelectionOptions modifiedOptions = options;
			modifiedOptions.trainingCorpus = options.testCorpus;
			CorpusStruct.testSets.add(readSentences(modifiedOptions));

			File save = new File(corpusDir, "gold-" + 0);
			SentenceWriter writer = new CoNLL09Writer(save);
			for (Sentence s : CorpusStruct.testSets.get(0)) {
				writer.specialwrite(s);
			}
			writer.close();
			CorpusStruct.parts = new ArrayList<File>();
			CorpusStruct.parts.add(save);
		}
		int count = 0;
		printMemUsage();
		double increase = 999;

		startState.score = computeStartScore(startState);

		while (increase > options.threshold) {
			increase = iterate(startState);
			count++;
			FeatureFile.writeToFile(startState.current.get(options.POSPrefix),
					options.POSPrefix, startState.comments, new File(ffDir,
							options.step + "-fs-" + count));
		}
		printMemUsage();
		if (!options.quadratic) {
			options.quadratic = true;
			fg = new FeatureGenerator();
			printMemUsage();
			SelectionState qstate = getStartingState(fg,
					startState.current.get(options.POSPrefix), bc);
			qstate.score = startState.score;
			qstate.comments = startState.comments;
			printMemUsage();
			fg.buildFeatureMaps(sentences);
			printMemUsage();
			increase = 1.0;
			while (increase > options.threshold) {
				increase = iterate(qstate);
				count++;
				FeatureFile.writeToFile(qstate.current.get(options.POSPrefix),
						options.POSPrefix, qstate.comments, new File(ffDir,
								options.step + "-fs-" + count));
			}
		}
	}

	private double computeStartScore(SelectionState inputState)
			throws IOException {
		double retval = 0.0;
		int k_max = (options.crossValidated ? options.partitions : 1);
		AbstractScorer scorer = getScorer(options.step);
		Map<Step, FeatureSet> featureSets = new HashMap<Step, FeatureSet>();
		FeatureSet featureSet = new FeatureSet(inputState.current);

		for (int k = 0; k < k_max; ++k) {
			System.err.println(CorpusStruct.parts.get(k).toString());
			SentenceReader xreader = new AllCoNLL09Reader(
					CorpusStruct.parts.get(k));
			Iterator<Sentence> xit = xreader.iterator();
			featureSets.put(options.step, featureSet);
			Pipeline xp = Pipeline.trainNewPipeline(
					CorpusStruct.trainingSets.get(k), fg, null, featureSets);
			for (Sentence gold : CorpusStruct.testSets.get(k)) {
				Sentence parsed = xit.next();
				if (options.step == Step.ai)
					for (Predicate pd : parsed.getPredicates())
						pd.setArgMap(new HashMap<Word, String>());
				if (options.step == Step.ac)
					for (Predicate pd : parsed.getPredicates())
						for (Word w : pd.getArgMap().keySet())
							pd.addArgMap(w, "ARG");

				xp.parseSentence(parsed);
				scorer.accScore(gold, parsed);
			}
			retval += scorer.getAvgScore();
		}
		retval = retval / k_max;

		return retval;
	}

	private double iterate(SelectionState inputState) throws IOException {
		int size = inputState.additional.size();
		double[] scores = new double[size];
		// Map<Step,FeatureSet> featureSet=new HashMap<Step,FeatureSet>();
		// Map<Step,FeatureSet> featureSets=new HashMap<Step,FeatureSet>();
		// FeatureSet featureSet=new FeatureSet(inputState.current);
		// featureSets.put(options.step,featureSet);
		// List<FeatureSet> combinations=new ArrayList<FeatureSet>();
		// for(int i=0;i<size;++i){
		// FeatureSet fs=startingSet.clone();
		// fs.get(options.POSPrefix).add(inputState.additional.get(i));
		// combinations.add(fs);
		// }
		AbstractScorer scorer = getScorer(options.step);
		int k_max = (options.crossValidated ? options.partitions : 1);

		TrainTestThread[] threads = new TrainTestThread[size];
		for (int k = 0; k < k_max; ++k) {

			for (int i = 0; i < size; ++i) {
				while (running(threads) > 20) {
					// waiting ...
				}
				threads[i] = new TrainTestThread();
				threads[i].setParams(inputState, i,
						CorpusStruct.trainingSets.get(k),
						CorpusStruct.testSets.get(k),
						CorpusStruct.parts.get(k), getScorer(options.step));

				threads[i].start();

			}

			while (running(threads) > 0) {
				// waiting ...
			}

			for (int i = 0; i < size; ++i) {
				scores[i] += threads[i].score();
			}

			System.out.println("Cross: " + k);
		}

		double bestScore = 0;
		int bestIndex = -1;
		for (int i = 0, length = scores.length; i < length; ++i) {
			double avg = scores[i] / k_max;
			if (avg > bestScore) {
				bestScore = avg;
				bestIndex = i;
			}
		}
		double increase = bestScore - inputState.score;
		if (increase > 0) {
			inputState.score = bestScore;
			Feature newFeature = inputState.additional.remove(bestIndex);
			inputState.current.get(options.POSPrefix).add(newFeature);
			inputState.comments.add("F1: " + bestScore + ", increase: "
					+ increase);
		} else {
			System.out.println("negative increase.");
		}
		return increase;
	}

	private int running(Thread[] threads) {
		int count = 0;
		for (Thread t : threads)
			if (t != null && t.isAlive())
				count++;

		return count;
	}

	private class TrainTestThread extends Thread {
		Map<Step, FeatureSet> featureSets;
		List<Sentence> trainSet;
		List<Sentence> testSet;
		File testSetFile;
		AbstractScorer scorer;
		int i;

		public void setParams(SelectionState inputState, int i,
				List<Sentence> trainSet, List<Sentence> testSet,
				File testSetFile, AbstractScorer scorer) {

			Map<String, List<Feature>> currentState = new HashMap<String, List<Feature>>();
			for (String s : inputState.current.keySet()) {
				currentState.put(s,
						new LinkedList<Feature>(inputState.current.get(s)));
			}

			FeatureSet featureSet = new FeatureSet(currentState);
			featureSet.get(options.POSPrefix).add(inputState.additional.get(i));

			Map<Step, FeatureSet> featureSets = new HashMap<Step, FeatureSet>();
			featureSets.put(options.step, featureSet);

			this.featureSets = featureSets;
			this.trainSet = trainSet;
			this.testSet = testSet;
			this.testSetFile = testSetFile;
			this.scorer = scorer;
			this.i = i;
		}

		public void run() {
			try {
				// FeatureGenerator newfg=new FeatureGenerator();
				// List<Feature> startingFeatures=getStartSetFromFile(fg,null);
				// SelectionState
				// startState=getStartingState(fg,startingFeatures,null);
				// fg.buildFeatureMaps(trainSet);
				Pipeline p = new Pipeline(null, featureSets, trainSet);
				p.train(trainSet, i);

				SentenceReader reader = new AllCoNLL09Reader(testSetFile);
				Iterator<Sentence> it = reader.iterator();

				for (Sentence gold : testSet) {
					Sentence parsed = it.next();
					if (options.step == Step.ai)
						for (Predicate pd : parsed.getPredicates())
							pd.setArgMap(new HashMap<Word, String>());
					if (options.step == Step.ac)
						for (Predicate pd : parsed.getPredicates())
							for (Word w : pd.getArgMap().keySet())
								pd.addArgMap(w, "ARG");

					p.parseSentence(parsed);
					scorer.accScore(gold, parsed);
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public double score() {
			return scorer.getAvgScore();
		}
	}

	private static AbstractScorer getScorer(Step step) {
		switch (step) {
		case pi:
			return new PredicateIdentificationScorer();
		case pd:
			return new PredicateDisambiguationScorer();
		case ai:
			return new ArgumentIdentificationScorer();
		case ac:
			return new ArgumentClassificationScorer();
		default:
			throw new Error("You are wrong here, check your code");
		}
	}

	private List<Feature> getStartSetFromFile(FeatureGenerator fg,
			BrownCluster bc) throws IOException {
		List<Feature> features = new ArrayList<Feature>();
		if (options.startingFeatureFile != null) {
			Map<String, List<String>> fnameMap = FeatureFile
					.readFile(options.startingFeatureFile);
			List<String> fnames = fnameMap.get(options.POSPrefix);
			if (fnames == null) {
				throw new Error(
						"The feature file provided does not contain the POSPrefix we want to explore. Aborting.");
			}
			for (String name : fnames) {
				Feature f = fg.getFeature(name, options.step == Step.pi,
						options.POSPrefix, bc, null);
				features.add(f);
			}
		}
		return features;
	}

	private SelectionState getStartingState(FeatureGenerator fg,
			List<Feature> startFeatures, BrownCluster bc) throws IOException {
		SelectionState state = new SelectionState();

		state.current = new HashMap<String, List<Feature>>();
		state.additional = new ArrayList<Feature>();
		state.comments = new ArrayList<String>();

		List<String> except = new ArrayList<String>();
		for (Feature f : startFeatures) {
			except.add(f.getName());
		}

		Collection<Feature> allFeatures = getAllFeatures(fg, except, bc);
		for (Feature f : allFeatures) {
			if (!startFeatures.contains(f)) {
				state.additional.add(f);
			} else {
				System.out
						.println("We shouldnt end up here... Look into this.");
			}
		}
		state.current.put(options.POSPrefix, startFeatures);
		for (int i = 0, size = startFeatures.size(); i < size; ++i) {
			state.comments.add(null);
		}
		return state;
	}

	private Collection<Feature> getAllFeatures(FeatureGenerator fg,
			List<String> except, BrownCluster bc) {
		List<FeatureName> singleNames = new ArrayList<FeatureName>();
		Collections.addAll(singleNames, noArgsSingleFeatures);
		// List<FeatureName> singleNames=Arrays.asList(noArgsSingleFeatures);

		if (options.includeFeats) {
			Collections.addAll(singleNames, noArgsSingleFeatures_Feats);
		}
		if (options.step == Step.ai || options.step == Step.ac) {
			Collections.addAll(singleNames, argsSingleFeatures);
			if (options.includeFeats)
				Collections.addAll(singleNames, argsSingleFeatures_Feats);
		}

		if (options.coref) {
			Collection<Feature> ret = new HashSet<Feature>();
			for (FeatureName fn1 : new FeatureName[] { FeatureName.XNCoref,
					FeatureName.FullCoref, FeatureName.CorefPredFeature,
					FeatureName.CorefArgFeature,
					FeatureName.CorefPredArgFeature,
					FeatureName.DiscourseDistance,
			// FeatureName.Salience,
			}) {
				if (!except.contains(fn1.toString())) {
					Feature f = fg.getFeature(fn1, options.step == Step.pi,
							options.POSPrefix, bc, null);
					ret.add(f);
				}

				if (options.quadratic) {
					for (FeatureName fn2 : singleNames) {
						if (!except.contains(FeatureGenerator.getCanonicalName(
								fn1, fn2))) {
							Feature f = fg.getQFeature(fn1, fn2,
									options.step == Step.pi, options.POSPrefix,
									bc, null);
							if (f != null)
								ret.add(f);
						}
					}
				}
			}
			return ret;
		}

		Collection<Feature> ret = new HashSet<Feature>();
		for (FeatureName fn : singleNames) {
			if (!except.contains(fn.toString())) {
				Feature f = fg.getFeature(fn, options.step == Step.pi,
						options.POSPrefix, bc, null);
				ret.add(f);
			}
		}

		if (options.quadratic) {
			for (FeatureName fn1 : singleNames) {
				for (FeatureName fn2 : singleNames) {
					if (fn1 == fn2)
						continue;
					try {
						if (!except.contains(FeatureGenerator.getCanonicalName(
								fn1, fn2))) {
							Feature f = fg.getQFeature(fn1, fn2,
									options.step == Step.pi, options.POSPrefix,
									bc, null);
							ret.add(f);
						}
					} catch (IllegalArgumentException e) {
						// Do nothing. This is when we try making illegal
						// pairings.
					}
				}
			}
		}
		return ret;
	}

	// private static List<FeatureSet> setupFeatureSets(FeatureGenerator fg,
	// FeatureSelectionOptions options) {
	// List<Feature> startSet=new ArrayList<Feature>();
	// List<Feature> additionalSet=fg.getAllFeatures(options.quadratic);
	// if(options.startingFeatureFile!=null) {
	// //then remove startSet from the additionalSet
	// throw new Error("Not implemented");
	// } else {
	// //Do nothing.
	// }
	// return explodeFeatures(startSet,fg,options);
	// }

	private static final FeatureName[] noArgsSingleFeatures = {
			FeatureName.PredWord, FeatureName.PredLemma, FeatureName.PredPOS,
			FeatureName.PredDeprel, FeatureName.PredParentWord,
			FeatureName.PredParentPOS, FeatureName.DepSubCat,
			FeatureName.ChildDepSet, FeatureName.ChildPOSSet,
			FeatureName.ChildWordSet };
	private static final FeatureName[] noArgsSingleFeatures_Feats = {
			FeatureName.PredFeats, FeatureName.PredParentFeats };
	private static final FeatureName[] argsSingleFeatures = {
			FeatureName.ArgWord, FeatureName.ArgLemma, FeatureName.ArgPOS,
			FeatureName.ArgDeprel, FeatureName.DeprelPath,
			FeatureName.Position, FeatureName.RightWord, FeatureName.LeftWord,
			FeatureName.RightPOS, FeatureName.LeftPOS,
			FeatureName.RightSiblingWord, FeatureName.LeftSiblingWord,
			FeatureName.RightSiblingPOS, FeatureName.LeftSiblingPOS,
			FeatureName.PredLemmaSense, FeatureName.POSDepPath,
			FeatureName.Distance, FeatureName.PredSubjPOS,
			FeatureName.PredSubjWord, FeatureName.POSDepPath,
	// FeatureName.CorefArgFeature, FeatureName.CorefPredFeature,
	// FeatureName.CorefPredArgFeature,
	// FeatureName.FullCoref, FeatureName.XNCoref,
	// FeatureName.DiscourseDistance,
	};
	private static final FeatureName[] argsSingleFeatures_Feats = {};

	private static List<Sentence> readSentences(FeatureSelectionOptions options) {
		List<Sentence> ret = new ArrayList<Sentence>();
		SentenceReader reader = new AllCoNLL09Reader(options.trainingCorpus);
		for (Sentence s : reader) {
			if (!options.dropSentencesWithoutPredicates
					|| s.getPredicates().size() > 0)
				ret.add(s);
		}
		reader.close();
		return ret;
	}

	// //For PI and PD we only want the features that have includeArgs==false,
	// ie we dont want features such as ArgWord or POSPath etc.
	// private static FeatureGenerator createFeatureGenerator(List<Sentence>
	// sentences, FeatureSelectionOptions options) {
	// //Here we also need to gather the single features if we are doing
	// q-selection
	// FeatureGenerator fg=new FeatureGenerator();
	// List<Feature> features=new ArrayList<Feature>();
	// for(FeatureName fn:noArgsSingleFeatures){
	// Feature f=fg.getFeature(fn, options.step==Step.pi, options.POSPrefix);
	// features.add(f);
	// }
	// if(options.includeFeats){
	// for(FeatureName fn:noArgsSingleFeatures_Feats){
	// Feature f=fg.getFeature(fn, options.step==Step.pi, options.POSPrefix);
	// features.add(f);
	// }
	// }
	// fg.buildFeatureMaps(sentences);
	// return fg;
	// }

	private static List<List<Sentence>> partitionSentences(
			List<Sentence> sentences, FeatureSelectionOptions options) {
		List<List<Sentence>> ret = new ArrayList<List<Sentence>>();
		for (int i = 0; i < options.partitions; ++i) {
			ret.add(new ArrayList<Sentence>());
		}

		if (sentences.get(0) instanceof CorpusSentence) {
			// split corpus into multiple documents
			Corpus c = ((CorpusSentence) sentences.get(0)).getMyCorpus();
			int count = 0;
			for (Sentence s : sentences) {
				if (((CorpusSentence) s).getMyCorpus() != c) {
					c = ((CorpusSentence) s).getMyCorpus();
					++count;
				}
				ret.get(count % options.partitions).add(s);
			}

		} else {
			if (options.randomizeInput)
				Collections.shuffle(sentences);

			int count = 0;
			for (Sentence s : sentences) {
				ret.get(count % options.partitions).add(s);
				++count;
			}
		}
		return ret;
	}

}
