package se.lth.cs.srl.preprocessor;

import java.io.File;
import java.io.IOException;

import is2.data.SentenceData09;
import is2.transitionS2a.Parser;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.util.Util;

public class HybridPreprocessor extends Preprocessor {
	protected final Parser parser;
	
	public HybridPreprocessor(File modelfile) throws IOException{
		tokenizer = Language.getLanguage().getTokenizer(null);
		parser = new Parser(modelfile.toString());		
	}

	@Override
	public StringBuilder getStatus(){
		StringBuilder sb=new StringBuilder();
		if(tokenizer!=null)
			sb.append("Tokenizer: "+tokenizer.getClass().getSimpleName()).append('\n');
		sb.append("Tokenizer time:  "+Util.insertCommas(tokenizeTime)).append('\n');
		sb.append("Parser time:     "+Util.insertCommas(dpTime)).append('\n');
		return sb;
	}


	@Override
	protected SentenceData09 preprocess(SentenceData09 sentence) {
		return parser.parse(sentence);
	}
	
	public boolean hasParser() {
		return parser!=null;
	}
}
