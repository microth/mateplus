package se.lth.cs.srl.preprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import is2.data.SentenceData09;
import is2.tools.Tool;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.preprocessor.tokenization.Tokenizer;
import se.lth.cs.srl.util.Util;

public class ExternalPreprocessor extends Preprocessor {
	protected final ExternalParser parser;
	protected final Tool lemmatizer;

	public ExternalPreprocessor(Tokenizer tokenizer, File parser) {
		this.tokenizer = tokenizer;
		lemmatizer = null;
		this.parser = new ExternalParser(parser);
	}

	@Override
	public StringBuilder getStatus() {
		StringBuilder sb = new StringBuilder();
		if (tokenizer != null)
			sb.append("Tokenizer: " + tokenizer.getClass().getSimpleName())
					.append('\n');
		sb.append("Tokenizer time:  " + Util.insertCommas(tokenizeTime))
				.append('\n');
		sb.append("Lemmatizer time: " + Util.insertCommas(lemmatizeTime))
				.append('\n');
		sb.append("Parser time:     " + Util.insertCommas(dpTime)).append('\n');
		return sb;
	}

	@Override
	protected SentenceData09 preprocess(SentenceData09 sentence) {
		if (lemmatizer != null) {
			long start = System.currentTimeMillis();
			sentence = lemmatizer.apply(sentence);
			lemmatizeTime += System.currentTimeMillis() - start;
		}
		return parser.apply(sentence);
	}

	public boolean hasParser() {
		return parser != null;
	}

	class ExternalParser {
		String parser;

		public ExternalParser(File parser) {
			this.parser = parser.toString();
		}

		public SentenceData09 apply(SentenceData09 sentence) {
			String s = sentence.oneLine();
			try {
				Process p = Runtime.getRuntime().exec(parser);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
						p.getOutputStream()));
				BufferedReader br = new BufferedReader(new InputStreamReader(
						p.getInputStream()));

				bw.write(s);
				bw.newLine();
				bw.close();

				if (p.waitFor() == 0) {
					String line = "";
					int i = 1;
					sentence.plemmas = new String[sentence.length() + 1];
					sentence.pfeats = new String[sentence.length() + 1];
					sentence.ppos = new String[sentence.length() + 1];
					sentence.plabels = new String[sentence.length() + 1];
					sentence.pheads = new int[sentence.length() + 1];

					sentence.pheads[0] = -1; // root
					while ((line = br.readLine()) != null) {
						String[] parts = line.split("\\s+");
						if (parts.length < 10)
							break; // sentence done.

						sentence.plemmas[i] = parts[2];
						sentence.ppos[i] = parts[4];
						sentence.pfeats[i] = parts[5];
						sentence.pheads[i] = Integer.parseInt(parts[6]);
						sentence.plabels[i] = parts[7];

						i++;
					}
					System.out.println("Done!");
				} else {
					System.err
							.println("Parsing process threw error message ...");
					System.exit(1);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			return sentence;
		}

	}
}
