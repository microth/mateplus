package se.lth.cs.srl.preprocessor;

import java.io.File;
import java.io.IOException;

import is2.data.SentenceData09;
import is2.tools.Tool;
import is2.transitionS2a.Parser;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.preprocessor.tokenization.Tokenizer;
import se.lth.cs.srl.util.Util;

public class HybridPreprocessor extends Preprocessor {
	protected final Parser parser;
	protected final Tool lemmatizer;

	public HybridPreprocessor(Tokenizer tokenizer, Tool lemmatizer, File modelfile) {
		this.tokenizer = tokenizer;
		this.lemmatizer = lemmatizer;
		parser = new Parser(modelfile.toString());		
	}

	@Override
	public StringBuilder getStatus(){
		StringBuilder sb=new StringBuilder();
		if(tokenizer!=null)
			sb.append("Tokenizer: "+tokenizer.getClass().getSimpleName()).append('\n');
		sb.append("Tokenizer time:  "+Util.insertCommas(tokenizeTime)).append('\n');
		sb.append("Lemmatizer time: "+Util.insertCommas(lemmatizeTime)).append('\n');
		sb.append("Parser time:     "+Util.insertCommas(dpTime)).append('\n');
		return sb;
	}


	@Override
	protected SentenceData09 preprocess(SentenceData09 sentence) {
		if(lemmatizer!=null){
			long start=System.currentTimeMillis();
			sentence = lemmatizer.apply(sentence);
			lemmatizeTime+=System.currentTimeMillis()-start;
		}
		return parser.apply(sentence);
	}
	
	public boolean hasParser() {
		return parser!=null;
	}
}
