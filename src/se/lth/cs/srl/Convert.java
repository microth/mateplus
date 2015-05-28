package se.lth.cs.srl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;

import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.StringInText;
import se.lth.cs.srl.io.ANNWriter;
import se.lth.cs.srl.io.AllCoNLL09Reader;
import se.lth.cs.srl.io.CoNLL09Writer;
import se.lth.cs.srl.io.DepsOnlyCoNLL09Reader;
import se.lth.cs.srl.io.SRLOnlyCoNLL09Reader;
import se.lth.cs.srl.io.SentenceReader;
import se.lth.cs.srl.io.SentenceWriter;
import se.lth.cs.srl.options.ParseOptions;
import se.lth.cs.srl.pipeline.Pipeline;
import se.lth.cs.srl.pipeline.Reranker;
import se.lth.cs.srl.pipeline.Step;
import se.lth.cs.srl.preprocessor.tokenization.StanfordPTBTokenizer;
import se.lth.cs.srl.preprocessor.tokenization.Tokenizer;
import se.lth.cs.srl.util.Util;

public class Convert {
	public static ParseOptions parseOptions;
	
	public static void main(String[] args) throws Exception{
		// HACK :-)
		args = new String[]{"annotations/new/all.conll"};
		
		SentenceWriter writer=new ANNWriter(new File(args[0]+".ann"));
		SentenceReader anno_reader=new AllCoNLL09Reader(new File(args[0]));
		BufferedReader sent_reader=new BufferedReader(new FileReader(args[0].replace(".conll", ".txt")));

		Tokenizer tokenizer = new StanfordPTBTokenizer();
		
		for(Sentence s:anno_reader){
			String line = sent_reader.readLine();
			line = line.replace("/", " ");

			List<StringInText> words = Arrays.asList(tokenizer.tokenizeplus(line));		
			for(int i=0; i<words.size(); i++) {
				// sanity check
				System.err.println(s.get(i).getForm() + "\t" + words.get(i).word());

				s.get(i).setBegin(words.get(i).begin());
				s.get(i).setEnd(words.get(i).end());
			}
			
			writer.write(s);
		}
		writer.close();
		anno_reader.close();
		sent_reader.close();
	}
}
