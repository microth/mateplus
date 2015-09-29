package se.lth.cs.srl.pipeline;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import se.lth.cs.srl.Learn;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.features.FeatureSet;
import uk.ac.ed.inf.srl.ml.LearningProblem;
import uk.ac.ed.inf.srl.ml.Model;
import uk.ac.ed.inf.srl.ml.liblinear.LibLinearLearningProblem;

public abstract class AbstractStep implements PipelineStep {

	public static final Integer POSITIVE = 1;
	public static final Integer NEGATIVE = 0;

	protected FeatureSet featureSet;
	protected Map<String, LearningProblem> learningProblems;
	protected Map<String, Model> models;

	public AbstractStep(FeatureSet fs) {
		this.featureSet = fs;
	}

	public void prepareLearning(String filePrefix) {
		learningProblems = new HashMap<String, LearningProblem>();
		for (String POS : featureSet.POSPrefixes) {
			File dataFile = new File(Learn.learnOptions.tempDir, filePrefix
					+ POS);
			learningProblems.put(POS, new LibLinearLearningProblem(dataFile,
					false));
		}
	}

	public abstract void extractInstances(Sentence s);

	public abstract void parse(Sentence s);

	public void done() {
		for (LearningProblem lp : learningProblems.values())
			lp.done();
	}

	public void train() {
		models = new HashMap<String, Model>();

		for (String POSPrefix : learningProblems.keySet()) {
			LearningProblem lp = learningProblems.get(POSPrefix);
			Model m = lp.train();
			models.put(POSPrefix, m);
		}
	}

	protected abstract String getModelFileName();

	@Override
	public void readModels(ZipFile zipFile) throws IOException,
			ClassNotFoundException {
		models = new HashMap<String, Model>();
		readModels(zipFile, models, getModelFileName());
	}

	@Override
	public void writeModels(ZipOutputStream zos) throws IOException {
		writeModels(zos, models, getModelFileName());
	}

	static void readModels(ZipFile zipFile, Map<String, Model> models,
			String filename) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(
				zipFile.getInputStream(zipFile.getEntry(filename)));
		int numberOfModels = ois.readInt();
		for (int i = 0; i < numberOfModels; ++i) {
			String POSPrefix = (String) ois.readObject();
			Model m = (Model) ois.readObject();
			models.put(POSPrefix, m);
		}
	}

	static void writeModels(ZipOutputStream zos, Map<String, Model> models,
			String filename) throws IOException {
		zos.putNextEntry(new ZipEntry(filename));
		ObjectOutputStream oos = new ObjectOutputStream(zos);
		int numberOfModels = models.size();
		oos.writeInt(numberOfModels);
		for (String POSPrefix : models.keySet()) {
			oos.writeObject(POSPrefix);
			oos.writeObject(models.get(POSPrefix));
		}
		oos.flush();
	}
}
