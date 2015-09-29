package se.lth.cs.srl.options;

import java.io.File;

import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.languages.Language.L;
import se.lth.cs.srl.pipeline.Step;

public class FeatureSelectionOptions {

	public Step step;
	public String POSPrefix;
	public File startingFeatureFile;
	public File trainingCorpus;
	public File testCorpus;
	public File tempDir;
	public File llbinary;

	public boolean crossValidated = false;

	public int partitions = 5;

	public double threshold = 0.01;

	public boolean deterministicPipeline = true;
	public boolean randomizeInput = false;
	public boolean dropSentencesWithoutPredicates = true;
	public boolean quadratic = false;
	public boolean includeFeats = false;
	public boolean skipUnknownPredicates = false;
	public String framenetdir = null;
	public boolean coref = false;

	public FeatureSelectionOptions(String[] args) {
		L l = L.valueOf(args[0]);
		Language.setLanguage(l);
		step = Step.valueOf(args[1]);
		int ai = 2;
		while (ai < args.length) {
			if (args[ai].equals("-framenet")) {
				ai++;
				framenetdir = args[ai++];
			}
			if (args[ai].equals("-prefix")) {
				ai++;
				POSPrefix = args[ai];
				ai++;
			} else if (args[ai].equals("-train")) {
				ai++;
				trainingCorpus = new File(args[ai]);
				ai++;
			} else if (args[ai].equals("-test")) {
				ai++;
				testCorpus = new File(args[ai]);
				ai++;
			} else if (args[ai].equals("-crossValidated")) {
				ai++;
				crossValidated = true;
			} else if (args[ai].equals("-startFeatureFile")) {
				ai++;
				startingFeatureFile = new File(args[ai]);
				ai++;
			} else if (args[ai].equals("-partitions")) {
				ai++;
				partitions = Integer.parseInt(args[ai]);
				ai++;
			} else if (args[ai].equals("-randomize")) {
				ai++;
				randomizeInput = true;
			} else if (args[ai].equals("-keepAll")) {
				ai++;
				dropSentencesWithoutPredicates = false;
			} else if (args[ai].equals("-threshold")) {
				ai++;
				threshold = Double.parseDouble(args[ai]);
				ai++;
			} else if (args[ai].equals("-coref")) {
				ai++;
				coref = true;
			} else if (args[ai].equals("-quadratic")) {
				ai++;
				quadratic = true;
			} else if (args[ai].equals("-includeFeats")) {
				ai++;
				includeFeats = true;
			} else if (args[ai].equals("-skipUnknownPredicates")) {
				ai++;
				skipUnknownPredicates = true;
			} else if (args[ai].equals("-llbinary")) {
				ai++;
				llbinary = new File(args[ai]);
				ai++;
			} else {
				System.err.println("Unknown option: " + args[ai]);
				System.exit(1);
			}
		}
		tempDir = LearnOptions.setupTempDir();
	}

	public void verifyArguments() {
		if (trainingCorpus == null || !trainingCorpus.exists()
				|| !trainingCorpus.canRead()) {
			System.err
					.println("You forgot to specify training corpus, or the file does not exist. Aborting.");
			System.exit(1);
		}
		if (startingFeatureFile != null
				&& (!startingFeatureFile.exists() || !startingFeatureFile
						.canRead())) {
			System.err
					.println("The starting feature file does not exist or can not be read. Aborting.");
			System.exit(1);
		}
	}

	public LearnOptions getLearnOptions() {
		LearnOptions options = new LearnOptions();
		options.skipNonMatchingPredicates = skipUnknownPredicates;
		options.tempDir = tempDir;
		options.deterministicPipeline = deterministicPipeline;
		options.liblinearBinary = llbinary;
		options.framenetdir = framenetdir;
		return options;
	}

}
