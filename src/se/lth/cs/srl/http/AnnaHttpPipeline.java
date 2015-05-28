package se.lth.cs.srl.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.http.ParseRequestHandler.StringPair;
import se.lth.cs.srl.http.whatswrongglue.WhatsWrongHelper;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.languages.Language.L;
import se.lth.cs.srl.options.HttpOptions;
import se.lth.cs.srl.preprocessor.Preprocessor;
import se.lth.cs.srl.util.Sentence2RDF;

public class AnnaHttpPipeline extends AbstractPipeline {
	
	private final Preprocessor pp;
	private final ImageCache imageCache;
	private final DefaultHandler defaultHandler;

	public AnnaHttpPipeline(Preprocessor pp,ImageCache imageCache,L l,int sentenceMaxLength, HttpOptions options){
		super(sentenceMaxLength,options);
		defaultHandler=new DefaultHandler(l,this);
		this.imageCache=imageCache;
		this.pp=pp;
	}
	
	@Override
	public String getStatusString() {
		return pp.getStatus().toString();
	}

	private static final String STYLESHEET=
			"<style type=\"text/css\">\n" +
			"  table { background-color:#000000 }\n" +
			"  td { background-color: #EEEEEE}\n" +
			"  th { background-color: #EEEEEE}\n" +
			"  .topRowCell {border-bottom: 1px solid black}\n" +
			"</style>\n";
	private static final String HTMLHEAD="<html><head>\n<title>Anna Demo</title></head>\n"+STYLESHEET+"<body>\n";
	private static final String HTMLTAIL="</body>\n</html>";
	@Override
	public String getHTMLHead() {
		return HTMLHEAD;
	}

	@Override
	public String getHTMLTail() {
		return HTMLTAIL;
	}

	@Override
	public String getParseInterfaceHTML(L l) {
		String s=
						"  <h3>Try the parsing pipeline</h3>\n" +
						"  Enter a sentence in <b>"+Language.getLanguage().toLangNameString()+"</b> and press Parse.<br/>\n" +
						"  <form action=\"/parse\" method=\"POST\">\n" +
						"    <table cellpadding=\"2\" cellspacing=\"2\">\n" +
						"      <tr><td valign=\"center\"><b>Input</b><td><textarea name=\""+AbstractHandler.sentenceDataVarName+"\" rows=\"3\" cols=\"40\"></textarea></td></tr>\n" +
						"      <tr><td valign=\"center\"><b>Return type</b><td><input type=\"radio\" name=\"returnType\" value=\"html\" checked=\"checked\" />&nbsp;&nbsp;HTML<br /><input type=\"radio\" name=\"returnType\" value=\"text\"/>&nbsp;&nbsp;Raw text<br/><input type=\"radio\" name=\"returnType\" value=\"rdf\"/>&nbsp;&nbsp;RDF/N3</td></tr>\n" +
						"      <tr><td colspan=\"2\"><input type=\"checkbox\" name=\"doRenderDependencyGraph\" checked=\"CHECKED\"/> <font size=\"-1\">Include graphical dependency tree output</font></td></tr>" +
						"      <tr><td colspan=\"2\" align=\"center\"><input type=\"submit\" value=\"Parse\" /><br /></td></tr>\n" +
						"  </table></form><br/>\n" +
						"  <font size=\"-1\">\n" +
						"    <b>Note:</b> For optimal performance, please\n" +
						"    <ul>\n" +
						"      <li>Spell properly</li>\n" +
						"      <li>Make sure to end the sentence with a period or other punctuation (In languages where punctuation is typically used, that is)</li>\n" +
						"      <li>Start the sentence with an uppercase letter (In languages where this is applicable, that is)</li>\n" +
						"      <li>Only feed the parser one sentence a time</li>\n" +
						"    </ul>\n" +
						"  </font>\n" +
						"  <font size=\"-1\">\n" +
						"    <b>System composition</b>\n" +
						"    <ul>\n" +
						"      <li>Tokenization - <a href=\"http://opennlp.apache.org/\">OpenNLP tools</a> tokenizer (most languages), <a href=\"http://nlp.stanford.edu/software/segmenter.shtml\">Stanford Chinese Segmenter</a> (Chinese), <a href=\"http://nlp.stanford.edu/software/tokenizer.shtml\">Stanford PTB tokenizer</a> (English), flex-based automaton by Peter Exner (Swedish) </li>\n"+
						"      <li>POS-tagger, lemmatizer, morphological tagger, and dependency parser - by Bernd Bohnet</li>\n" +
						"      <li>Graph Visualization - using <a href=\"http://code.google.com/p/whatswrong/\">What's Wrong With My NLP?</a></li>\n" +
						"    </ul>\n"+
						"  </font>\n" +
						"  <font size=\"-1\">For downloads and more information see <a href=\"http://code.google.com/p/mate-tools/\">http://code.google.com/p/mate-tools/</a>.</font><br/>\n";
		return s;
	}

	@Override
	public StringPair parseRequest(String inputSentence,Map<String, String> vars) throws Exception {
		long t0=System.currentTimeMillis();
		String[] tokens=pp.tokenize(inputSentence);
		if(sentenceMaxLength>0 && tokens.length>sentenceMaxLength)
			throw new SentenceTooLongException("Sentence too long: "+tokens.length+" tokens",tokens.length);
		Sentence s=new Sentence(pp.preprocess(tokens),!pp.hasParser());
		long time=System.currentTimeMillis()-t0;
		String httpResponse;
		String content_type;
		//Prepare the response;
		if(vars.containsKey("returnType") && vars.get("returnType").equals("html")){
			boolean includeDependencyGraphImage=vars.containsKey("doRenderDependencyGraph") && pp.hasParser();
			httpResponse=getHTMLResponse(s,time,includeDependencyGraphImage);
			content_type="text/html; charset=UTF-8";
		} else if(vars.containsKey("returnType") && vars.get("returnType").equals("rdf")){
			httpResponse=Sentence2RDF.sentence2RDF(s);
			content_type="text/n3; charset=utf-8";
		} else {
			httpResponse=s.toString();
			content_type="text/plain; charset=utf-8";
		}
		System.out.println("Content type returned: "+content_type);
		System.out.println("Sentence returned:");
		System.out.println(s.toString());
		return new StringPair(httpResponse,content_type);
	}

	private String getHTMLResponse(Sentence sen,long time,boolean includeDependencyGraphImage) {
		StringBuilder ret=new StringBuilder(HTMLHEAD);
		ret.append('\n').append(super.HTML_TOP_EXTRA);
		if(includeDependencyGraphImage){
			try {
				ByteArrayOutputStream baos=WhatsWrongHelper.renderPNG(WhatsWrongHelper.getNLPInstance(sen),1);
				String key=imageCache.addObject(baos);
				ret.append("<img src=\"/img/"+key+".png\"/>");
				ret.append("<br/>\n<hr/>\n<br/>\n");
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		//The raw CoNLL 2009 output as a table
		ret.append("<table><tr><th>ID</th><th>Form</th><th>Lemma</th><th>PLemma</th><th>POS</th><th>PPOS</th><th>Feats</th><th>PFeats</th><th>Head</th><th>PHead</th><th>Deprel</th><th>PDeprel</th><th>IsPred</th><th>Pred</th>");
		for(int i=0;i<sen.getPredicates().size();++i)
			ret.append("<th>Args: "+sen.getPredicates().get(i).getSense()+"</th>");
		ret.append("</tr>\n");
		for(String line:sen.toString().split("\n")){
			ret.append("<tr>");
			int tokIdx=0;
			for(String token:line.split("\t")){
				if(token=="null" || tokIdx%2==0 && tokIdx>1 && tokIdx<12)
					token="_";
				ret.append("<td>").append(token).append("</td>");
				++tokIdx;
			}
			ret.append("</tr>\n");
		}
		ret.append("</table>\n<br/><hr/><br/>");
		ret.append(defaultHandler.pages.get("default"));
		ret.append('\n').append(super.HTML_BOTTOM_EXTRA);
		ret.append("</body></html>");
		return ret.toString();
	}

	@Override
	public DefaultHandler getDefaultHandler() {
		return defaultHandler;
	}
	
	public static void main(String[] args) throws IOException{
		HttpOptions options=new HttpOptions();
		options.parseCmdLineArgs(args);
		Preprocessor pp=Language.getLanguage().getPreprocessor(options);
		AnnaHttpPipeline ahp=new AnnaHttpPipeline(pp, HttpPipeline.imageCache, options.l,options.sentenceMaxLength,options);
		HttpPipeline.setupHttpPipeline(options, ahp);
		System.out.println("done.");
	}
}
