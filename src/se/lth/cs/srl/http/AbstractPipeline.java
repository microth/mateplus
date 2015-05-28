package se.lth.cs.srl.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;

import se.lth.cs.srl.http.ParseRequestHandler.StringPair;
import se.lth.cs.srl.languages.Language.L;
import se.lth.cs.srl.options.HttpOptions;

public abstract class AbstractPipeline {
	
	protected final int sentenceMaxLength;
	
	protected final String HTML_TOP_EXTRA;
	protected final String HTML_BOTTOM_EXTRA;
	
	public abstract String getStatusString();
	public abstract String getHTMLHead();
	public abstract String getHTMLTail();
	public abstract String getParseInterfaceHTML(L l);
	public abstract StringPair parseRequest(String inputSentence, Map<String, String> vars) throws Exception;
	public abstract DefaultHandler getDefaultHandler();
	
	protected AbstractPipeline(int sentenceMaxLength,HttpOptions options){
		this.sentenceMaxLength=sentenceMaxLength;
		HTML_TOP_EXTRA=options.pageTopHTMLFile!=null?slurp(options.pageTopHTMLFile):"";
		HTML_BOTTOM_EXTRA=options.pageBottomHTMLFile!=null?slurp(options.pageBottomHTMLFile):"";
	}

	static class SentenceTooLongException extends IllegalArgumentException {
		private static final long serialVersionUID = 1L;
		public final int length;
		public SentenceTooLongException(String message,int length){
			super(message);
			this.length=length;
		}
	}
	
	private static String slurp(File file) {
		try {
			BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			StringBuilder in=new StringBuilder();
			String line;
			while((line=reader.readLine())!=null)
				in.append(line).append('\n');
			reader.close();
			return in.toString();
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}
}
