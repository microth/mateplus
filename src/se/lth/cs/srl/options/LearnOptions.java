package se.lth.cs.srl.options;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import se.lth.cs.srl.Learn;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.pipeline.Step;

public class LearnOptions extends Options {
	public static final Map<Step, String> featureFileNames;
	static {
		Map<Step, String> map = new HashMap<Step, String>();
		map.put(Step.pi, "pi.feats");
		map.put(Step.pd, "pd.feats");
		map.put(Step.ai, "ai.feats");
		map.put(Step.ac, "ac.feats");
		// map.put(Step.ao,"ao.feats");
		// map.put(Step.po,"po.feats");
		featureFileNames = Collections.unmodifiableMap(map);
	}

	public File liblinearBinary;
	public File tempDir;
	private File featureFileDir;

	public boolean skipNonMatchingPredicates = false;
	public boolean trainReranker = false;
	public boolean deleteTrainFiles = true;

	public boolean deterministicPipeline = true;
	public boolean deterministicReranker = true;

	public String framenetdir = null;

	public boolean global_insertGoldMapForTrain = true;
	public int global_numberOfCrossTrain = 5;

	private Map<Step, File> featureFiles;

	public File brownClusterFile;
	public File wordEmbeddingFile;
	public boolean globalFeats = false;

	LearnOptions() {
	}

	public LearnOptions(String[] args) {
		superParseCmdLine(args);
	}

	@Override
	int parseCmdLine(String[] args, int ai) {
		if (args[ai].equals("-fdir")) {
			ai++;
			featureFileDir = new File(args[ai]);
			ai++;
		} else if (args[ai].equals("-globalFeats")) {
			ai++;
			globalFeats = true;
		} else if (args[ai].equals("-llbinary")) {
			ai++;
			liblinearBinary = new File(args[ai]);
			ai++;
		} else if (args[ai].equals("-reranker")) {
			ai++;
			trainReranker = true;
		} else if (args[ai].equals("-framenet")) {
			ai++;
			framenetdir = args[ai++];
		} else if (args[ai].equals("-partitions")) {
			ai++;
			global_numberOfCrossTrain = Integer.parseInt(args[ai]);
			ai++;
		} else if (args[ai].equals("-dontInsertGold")) {
			ai++;
			global_insertGoldMapForTrain = false;
		} else if (args[ai].equals("-skipUnknownPredicates")) {
			ai++;
			skipNonMatchingPredicates = true;
		} else if (args[ai].equals("-dontDeleteTrainData")) {
			ai++;
			deleteTrainFiles = false;
		} else if (args[ai].equals("-ndPipeline")) {
			ai++;
			deterministicPipeline = false;
		} else if (args[ai].equals("-ndReranker")) {
			ai++;
			deterministicPipeline = false;
			deterministicReranker = false;
			trainReranker = true;
		} else if (args[ai].equals("-cluster")) {
			ai++;
			brownClusterFile = new File(args[ai++]);
		} else if (args[ai].equals("-embedding")) {
			ai++;
			wordEmbeddingFile = new File(args[ai++]);
		}
		return ai;
	}

	@Override
	void usage() {
		System.err.println("Usage:");
		System.err.println(" java -cp <classpath> " + Learn.class.getName()
				+ " <lang> <input-corpus> <model-file> [options]");
		System.err.println();
		System.err.println("Example:");
		System.err
				.println(" java -cp srl.jar:lib/liblinear-1.51-with-deps.jar "
						+ Learn.class.getName()
						+ " eng ~/corpora/eng/CoNLL2009-ST-English-train.txt eng-srl.mdl -reranker -fdir ~/features/eng -llbinary ~/liblinear-1.6/train");
		System.err.println();
		System.err
				.println(" trains a complete pipeline and reranker based on the corpus and saves it to eng-srl.mdl");
		System.err.println();
		super.printUsageLanguages(System.err);
		System.err.println();
		super.printUsageOptions(System.err);
		System.err.println();
		System.err.println("Learning-specific options:");
		System.err
				.println(" -fdir <dir>             the directory with feature files (see below)");
		System.err
				.println(" -reranker               trains a reranker also (not done by default)");
		System.err
				.println(" -llbinary <file>        a reference to a precompiled version of liblinear,");
		System.err
				.println("                         makes training much faster than the java version.");
		System.err
				.println(" -partitions <int>       number of partitions used for the reranker");
		System.err
				.println(" -dontInsertGold         don't insert the gold standard proposition during");
		System.err
				.println("                         training of the reranker.");
		System.err
				.println(" -skipUnknownPredicates  skips predicates not matching any POS-tags from");
		System.err.println("                         the feature files.");
		System.err
				.println(" -dontDeleteTrainData    doesn't delete the temporary files from training");
		System.err
				.println("                         on exit. (For debug purposes)");
		System.err
				.println(" -ndPipeline             Causes the training data and feature mappings to be");
		System.err
				.println("                         derived in a non-deterministic way. I.e. training the pipeline");
		System.err
				.println("                         on the same corpus twice does not yield the exact same models.");
		System.err
				.println("                         This is however slightly faster.");
		// TODO There is some something undeterministic about the deterministic
		// reranker. Needs to be looked into.
		// System.err.println(" -dReranker              Same as above, but with the reranker. This option implies");
		// System.err.println("                         a deterministic pipeline as well. It also implies the");
		// System.err.println("                         -reranker option (obviously)");
		System.err.println();
		System.err
				.println("The feature file dir needs to contain four files with feature sets. See");
		System.err
				.println("the website for further documentation. The files are called");
		System.err.println("pi.feats, pd.feats, ai.feats, and ac.feats");
		System.err
				.println("All need to be in the feature file dir, otherwise you will get an error.");
	}

	@Override
	boolean verifyArguments() {
		verifyFeatureFiles();
		if (liblinearBinary != null
				&& (!liblinearBinary.exists() || !liblinearBinary.canExecute())) {
			System.err
					.println("The provided liblinear binary does not exists or can not be executed. Aborting.");
			System.exit(1);
		}
		tempDir = setupTempDir();
		return true;
	}

	public static File setupTempDir() {
		String curDateTime = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		String tempDirPath = System.getProperty("java.io.tmpdir")
				+ File.separator + "srl_" + curDateTime;
		File tempDir = new File(tempDirPath);
		if (tempDir.exists()) {
			throw new Error("Temporary dir " + tempDir
					+ " already exists. Look into this.");
		} else {
			if (!tempDir.mkdir()) {
				throw new Error("Failed to create temporary dir " + tempDir);
			}
			System.out.println("Using temporary directory " + tempDir);
		}
		tempDir.deleteOnExit();
		return tempDir;
	}

	private void verifyFeatureFiles() {
		if (featureFileDir == null) {
			featureFileDir = new File("featuresets" + File.separator
					+ Language.getLanguage().getL());
		}
		if (!featureFileDir.exists() || !featureFileDir.canRead()) {
			System.err.println("Feature file dir " + featureFileDir
					+ " does not exist or can not be read. Aborting.");
			System.exit(1);
		}
		featureFiles = new HashMap<Step, File>();
		for (Step s : Step.values()) {
			File f = new File(featureFileDir, featureFileNames.get(s));
			if (!f.exists() || !f.canRead()) {
				System.out.println("Feature file " + f
						+ " does not exist or can not be read, aborting.");
				System.exit(1);
			}
			featureFiles.put(s, f);
		}
	}

	public Map<Step, File> getFeatureFiles() {
		return featureFiles;
	}
}
