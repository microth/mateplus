package se.lth.cs.srl;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import se.lth.cs.srl.io.AllCoNLL09Reader;
import se.lth.cs.srl.io.SentenceReader;
import se.lth.cs.srl.options.LearnOptions;
import se.lth.cs.srl.pipeline.Pipeline;
import se.lth.cs.srl.pipeline.Reranker;
import se.lth.cs.srl.util.BrownCluster;
import se.lth.cs.srl.util.Util;
import se.lth.cs.srl.util.WordEmbedding;

public class Learn {

	public static LearnOptions learnOptions;

	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		learnOptions = new LearnOptions(args);
		learn();
		System.out.println("Total time consumtion: "
				+ Util.insertCommas(System.currentTimeMillis() - startTime)
				+ "ms");
	}

	private static void learn() throws IOException {
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(
				new FileOutputStream(learnOptions.modelFile)));
		if (learnOptions.trainReranker) {
			new Reranker(learnOptions, zos);
		} else {
			BrownCluster bc = Learn.learnOptions.brownClusterFile == null ? null
					: new BrownCluster(Learn.learnOptions.brownClusterFile);
			WordEmbedding we = Learn.learnOptions.wordEmbeddingFile == null ? null
					: new WordEmbedding(Learn.learnOptions.wordEmbeddingFile);

			SentenceReader reader = new AllCoNLL09Reader(
					learnOptions.inputCorpus);
			Pipeline.trainNewPipeline(reader, learnOptions.getFeatureFiles(),
					zos, bc, we);
		}
		zos.close();
	}
}
