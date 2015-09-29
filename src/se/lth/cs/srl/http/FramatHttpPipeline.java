package se.lth.cs.srl.http;

import is2.data.SentenceData09;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CoNLL2011DocumentReader.Options;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import se.lth.cs.srl.CompletePipeline;
import se.lth.cs.srl.Parse;
import se.lth.cs.srl.SemanticRoleLabeler;
import se.lth.cs.srl.corpus.Corpus;
import se.lth.cs.srl.corpus.CorpusSentence;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.corpus.Yield;
import se.lth.cs.srl.http.ParseRequestHandler.StringPair;
import se.lth.cs.srl.http.whatswrongglue.WhatsWrongHelper;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.languages.Language.L;
import se.lth.cs.srl.options.HttpOptions;
import se.lth.cs.srl.pipeline.Pipeline;
import se.lth.cs.srl.pipeline.Reranker;
import se.lth.cs.srl.pipeline.Step;
import se.lth.cs.srl.util.FileExistenceVerifier;
import se.lth.cs.srl.util.Sentence2RDF;

public class FramatHttpPipeline extends AbstractPipeline {

	private final SemanticRoleLabeler srl;
	private final ImageCache imageCache;
	private final DefaultHandler defaultHandler;
	private final StanfordCoreNLP pipeline;
	private Map<String, Double[]> initialvecs = null;
	private Map<String, Double[]> squaredgrads = null;
	private Map<String, Integer> vocab = null;
	private String glovedir = null;
	
	public FramatHttpPipeline(SemanticRoleLabeler srl, ImageCache imageCache,
			L l, int sentenceMaxLength, HttpOptions options) {
		super(sentenceMaxLength, options);
		this.srl = srl;
		this.defaultHandler = new DefaultHandler(l, this);
		this.imageCache = imageCache;

		Properties props = new Properties();
		props.put("annotators",
				"tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		props.put("dcoref.sievePasses", "MarkRole," + "DiscourseMatch,"
				+ "ExactStringMatch," + "RelaxedExactStringMatch,"
				+ "PreciseConstructs," + "StrictHeadMatch1,"
				+ "StrictHeadMatch2," + "StrictHeadMatch3,"
				+ "StrictHeadMatch4," + "RelaxedHeadMatch");

		pipeline = new StanfordCoreNLP(props);
		if(options.glovedir!=null) {
			System.err.println("Reading initial word vectors ...");
			initialvecs = new HashMap<String, Double[]>();
			squaredgrads = new HashMap<String, Double[]>();
			vocab = new HashMap<String, Integer>();
			
			BufferedReader br1 = null;
			BufferedReader br2 = null;
			BufferedReader br3 = null;
			try {
				br1 = new BufferedReader(new FileReader(new File(options.glovedir+"/vectors.txt")));
				br2 = new BufferedReader(new FileReader(new File(options.glovedir+"/gradsq.txt")));
				br3 = new BufferedReader(new FileReader(new File(options.glovedir+"/vocab.txt")));
				String line = "";
				while((line = br1.readLine())!=null) {
					String[] parts = line.split(" ");
					Double[] vec = new Double[parts.length-1];
					for(int i=1; i<parts.length; i++)
						vec[i-1] = Double.parseDouble(parts[i]);
					initialvecs.put(parts[0], vec);
				}
				
				while((line = br2.readLine())!=null) {
					String[] parts = line.split(" ");
					Double[] vec = new Double[parts.length-1];
					for(int i=1; i<parts.length; i++)
						vec[i-1] = Double.parseDouble(parts[i]);
					squaredgrads.put(parts[0], vec);
				}
				
				while((line = br3.readLine())!=null) {
					String[] parts = line.split(" ");
					vocab.put(parts[0], Integer.parseInt(parts[1]));
				}
			} catch(IOException e) {
				e.printStackTrace();
				System.exit(1);
			} finally {
				try {
					br1.close();
					br2.close();
					br3.close();
				} catch(IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
	}

	@Override
	public String getStatusString() {
		return null;// pipeline.getStatusString();
	}

	@Override
	public DefaultHandler getDefaultHandler() {
		return defaultHandler;
	}

	private static final String STYLESHEET = "<style type=\"text/css\">\n"
			+ "  table { background-color:#000000 }\n"
			+ "  td { background-color: #EEEEEE}\n"
			+ "  th { background-color: #EEEEEE}\n"
			+ "  .topRowCell {border-bottom: 1px solid black}\n"
			+ "  .A0, .C-A0 {background-color:#CCCC00}\n"
			+ "  .A1, .C-A1 {background-color:#CC0000}\n"
			+ "  .A2, .C-A2 {background-color:#00CC00}\n"
			+ "  .A3, .C-A3 {background-color:#0000CC}\n"
			+ "  .AM-NEG {background-color:#CC00CC}\n"
			+ "  .AM-MNR {background-color:#00CCCC}\n"
			+ "  .ARG_DEFAULT {background-color:#CCCCCC}\n" + "</style>\n";

	// TODO improve the color codes for different labels, and across languages.
	// I.e. refactor this and put it in the Language classes, rather than here.
	private static final String HTMLHEAD = "<html><head>\n<title>Semantic Role Labeler Demo</title>\n"
			+ STYLESHEET + "</head>\n<body>\n";
	private static final String HTMLTAIL = "</body>\n</html>";

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
		String s = "  <h3>Try the semantic role labeler</h3>\n"
				+ "  Enter a sentence in <b>"
				+ Language.getLanguage().toLangNameString()
				+ "</b> and press Parse.<br/>\n"
				+ "  <form action=\"/parse\" method=\"POST\">\n"
				+ "    <table cellpadding=\"2\" cellspacing=\"2\">\n"
				+ "      <tr><td valign=\"center\"><b>Input</b><td><textarea name=\""
				+ AbstractHandler.sentenceDataVarName
				+ "\" rows=\"3\" cols=\"40\"></textarea></td></tr>\n"
				+ "      <tr><td valign=\"center\"><b>Return type</b><td><input type=\"radio\" name=\"returnType\" value=\"html\" checked=\"checked\" />&nbsp;&nbsp;HTML<br /><input type=\"radio\" name=\"returnType\" value=\"text\"/>&nbsp;&nbsp;Raw text<br/><input type=\"radio\" name=\"returnType\" value=\"rdf\"/>&nbsp;&nbsp;RDF/N3</td></tr>\n"
				+ "      <tr><td colspan=\"2\"><input type=\"checkbox\" name=\"doRenderDependencyGraph\" checked=\"CHECKED\"/> <font size=\"-1\">Include graphical dependency tree output</font></td></tr>"
				+ "      <tr><td colspan=\"2\"><input type=\"checkbox\" name=\"doPerformDictionaryLookup\" /> <font size=\"-1\">Attempt to lookup and reference predicates in dictionary<sup>&dagger;</sup>.</font></td></tr>\n"
				+ "      <tr><td colspan=\"2\" align=\"center\"><input type=\"submit\" value=\"Parse\" /><br /></td></tr>\n"
				+ "  </table></form><br/>\n"
				+ "  <font size=\"-1\">\n"
				+ "    <b>Note:</b> For optimal performance, please\n"
				+ "    <ul>\n"
				+ "      <li>Spell properly</li>\n"
				+ "      <li>Make sure to end the sentence with a period or other punctuation (In languages where punctuation is typically used, that is)</li>\n"
				+ "      <li>Start the sentence with an uppercase letter (In languages where this is applicable, that is)</li>\n"
				+ "      <li>Only feed the parser one sentence a time</li>\n"
				+ "    </ul>\n"
				+ "  </font>\n"
				+ "  <font size=\"-1\">\n"
				+ "    <b>System composition</b>\n"
				+ "    <ul>\n"
				+ "      <li>Tokenization - <a href=\"http://opennlp.apache.org/\">OpenNLP tools</a> tokenizer (most languages), <a href=\"http://nlp.stanford.edu/software/segmenter.shtml\">Stanford Chinese Segmenter</a> (Chinese), <a href=\"http://nlp.stanford.edu/software/tokenizer.shtml\">Stanford PTB tokenizer</a> (English), flex-based automaton by Peter Exner (Swedish) </li>\n"
				+ "      <li>POS-tagger, lemmatizer, morphological tagger, and dependency parser - by Bernd Bohnet</li>\n"
				+ "      <li>Semantic Role Labeling - based on LTH's contribution to the CoNLL 2009 ST</li>\n"
				+ "      <li>Graph Visualization - using <a href=\"http://code.google.com/p/whatswrong/\">What's Wrong With My NLP?</a></li>\n"
				+ "    </ul>\n"
				+ "  </font>\n"
				+ "  <font size=\"-1\">For downloads and more information see <a href=\"http://code.google.com/p/mate-tools/\">http://code.google.com/p/mate-tools/</a>.</font><br/>\n"
				+ "  <font size=\"-1\"><sup>&dagger;</sup> This is only applicable for HTML response, and with English. Note that this takes longer, and if the online dictionary is down, it may time out and take a significant amount of time.</font>\n";
		return s;
	}

	@Override
	public StringPair parseRequest(String inputText, Map<String, String> vars)
			throws Exception {
		long parsingTime = System.currentTimeMillis();

		Annotation document = new Annotation(inputText);
		pipeline.annotate(document);

		Map<String, Double[]> word2vecs = null; 
		if(initialvecs!=null)
			createvecs(document);
		
		Corpus c = new Corpus("tmp");

		for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
			StringBuffer posOutput = new StringBuffer();

			/**
			 * if(sentenceMaxLength>0 && tokens.length>sentenceMaxLength) throw
			 * new SentenceTooLongException("Sentence too long",tokens.length);
			 * Sentence sen=new Sentence(s09,false);
			 **/

			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				if (posOutput.length() > 0) {
					posOutput.append(" ");
				}
				posOutput.append(token.word());
				posOutput.append("_");
				posOutput.append(token.tag());
			}

			String parse = runProcess("nc stkilda 12345", posOutput.toString());
			parse = parse.replaceAll("-\t-", "_\t_\n@#").replaceAll("@#\t", "")
					.replaceAll("@#", "");

			String[] lines = parse.split("\n");
			String[] words = new String[lines.length + 1];
			String[] lemmas = new String[lines.length + 1];
			String[] tags = new String[lines.length + 1];
			String[] morphs = new String[lines.length + 1];
			int[] heads = new int[lines.length];
			String[] deprels = new String[lines.length];

			for (int i = 1; i < words.length; i++) {
				String[] parts = lines[i - 1].split("\t");
				words[i] = sentence.get(TokensAnnotation.class).get(i - 1)
						.word();
				tags[i] = sentence.get(TokensAnnotation.class).get(i - 1).tag();
				lemmas[i] = sentence.get(TokensAnnotation.class).get(i - 1)
						.lemma();
				morphs[i] = "_";
				heads[i - 1] = Integer.parseInt(parts[6]);
				deprels[i - 1] = parts[7];
			}
			Sentence sen = new Sentence(words, lemmas, tags, morphs);
			sen.setHeadsAndDeprels(heads, deprels);

			/* add labeled predicates from SEMAFOR */
			String json = runProcess("nc stkilda 8043", parse);
			Pattern pred_frame = Pattern
					.compile("\\{\"target\":\\{\"name\":\"([A-Za-z_]*)\",\"spans\":\\[\\{\"start\":([0-9]*),\"");
			Matcher m = pred_frame.matcher(json);
			while (m.find()) {
				String frame = m.group(1);
				int index = Integer.parseInt(m.group(2));
				System.out.println(index + "\t" + frame);

				sen.makePredicate(index + 1);
				((Predicate) sen.get(index + 1)).setSense(frame);
			}
			
			if(word2vecs!=null)
				for(Word w : sen)
					if(word2vecs.containsKey(w.getForm().toLowerCase()))
						w.setRep(word2vecs.get(w.getForm().toLowerCase()));

			new CorpusSentence(sen, c);
		}

		/* add coref output to corpus */
		Map<Integer, CorefChain> coref = document
				.get(CorefChainAnnotation.class);
		int num = 1;
		for (Map.Entry<Integer, CorefChain> entry : coref.entrySet()) {
			CorefChain cc = entry.getValue();
			// skip singleton mentions
			if (cc.getMentionsInTextualOrder().size() == 1)
				continue;

			for (CorefMention m : cc.getMentionsInTextualOrder()) {
				c.addMention(c.get(m.sentNum - 1), m.headIndex, num);
			}
			num++;
		}

		for (Sentence sen : c) {
			srl.parseSentence(sen);
		}
		parsingTime = System.currentTimeMillis() - parsingTime;

		String httpResponse = "";
		String content_type = null;
		for (Sentence sen : c) {

			// Prepare the response;
			if (vars.containsKey("returnType")
					&& vars.get("returnType").equals("html")) {
				boolean performURLLookup = vars
						.containsKey("doPerformDictionaryLookup");
				boolean includeDependencyGraphImage = vars
						.containsKey("doRenderDependencyGraph");
				httpResponse += getHTMLResponse(sen, parsingTime,
						performURLLookup, includeDependencyGraphImage);
				content_type = "text/html; charset=UTF-8";
			} else if (vars.containsKey("returnType")
					&& vars.get("returnType").equals("rdf")) {
				httpResponse = Sentence2RDF.sentence2RDF(sen);
				content_type = "text/n3; charset=utf-8";
			} else {
				httpResponse += sen.toString() + "\n";
				content_type = "text/plain; charset=utf-8";
			}

			System.out.println("Content type returned: " + content_type);
			System.out.println("Sentence returned:");
			System.out.println(sen.toString());
		}

		if (vars.containsKey("returnType")
				&& vars.get("returnType").equals("html")) {
			httpResponse += super.HTML_BOTTOM_EXTRA;
			httpResponse += "</body></html>";
		}

		return new StringPair(httpResponse, content_type);
	}

	private Map<String, Double[]> createvecs(Annotation document) {
		Map<String, Double[]> retval = new HashMap<String, Double[]>();
		Set<String> localvocab = new TreeSet<String>();
		
		StringBuffer text = new StringBuffer();
		for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				if(token.index()>0) text.append(" ");
				text.append(token.word().toLowerCase());
				localvocab.add(token.word().toLowerCase());
			}
			text.append("\n");
		}
		
		try {
			// write temporary text file
			File tmp = File.createTempFile("glv", ".txt");
			BufferedWriter bw = null;
			BufferedWriter bw1 = null;
			BufferedWriter bw2 = null;
			BufferedWriter bw3 = null;
			try {			
				bw = new BufferedWriter(new FileWriter(tmp));
				bw1= new BufferedWriter(new FileWriter(new File(tmp.toString()+".init")));
				bw2= new BufferedWriter(new FileWriter(new File(tmp.toString()+".grad")));
				bw3= new BufferedWriter(new FileWriter(new File(tmp.toString()+".vocab")));
						
				bw.write(text.toString());
				for(String s : localvocab) {
					if(initialvecs.containsKey(s)) {
						bw1.write(s);
						for(Double d : initialvecs.get(s)) {
							bw1.write(" ");
							bw1.write(d.toString());
						}
						bw1.newLine();
						
						bw2.write(s);
						for(Double d : squaredgrads.get(s)) {
							bw2.write(" ");
							bw2.write(d.toString());
						}
						bw2.newLine();
						
						bw3.write(s);
						bw3.write(" ");
						bw3.write(new Integer(vocab.get(s)).toString());
						bw3.newLine();
					}
				}
			} catch(IOException e) {
				e.printStackTrace();
				System.exit(1);
			} finally {
				try {
					bw.close();
					bw1.close();
					bw2.close();
					bw3.close();
				} catch(IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}		
			
			// execute glove
			Process p;
			try {
				p = Runtime.getRuntime().exec(new String[]{glovedir+"/processtmp.sh", tmp.toString()});
				if (p.waitFor() != 0) {
					System.err.println("Could not run GloVe script!");
					System.exit(1);
				}			
			} catch(Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			// read created vectors
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(tmp.toString()+".vectors"));
				String line = "";
				boolean first = true;
				while((line = br.readLine())!=null) {
					String[] parts = line.split(" ");
					Double[] vec = new Double[parts.length-1];
					for(int i=1; i<parts.length; i++) {
						vec[i-1] = Double.parseDouble(parts[i]);
					}
					if(first) {
						System.err.println(parts[0] + "\t" + vec.toString());
						first = false;
					}
					retval.put(parts[0], vec);
				}
			} catch(IOException e) {
				e.printStackTrace();
				System.exit(1);
			} finally {
				try {
					br.close();
				} catch(IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}	
		return retval;
	}

	private String runProcess(String command, String text) {
		StringBuffer retval = new StringBuffer();

		try {
			Process p = Runtime.getRuntime().exec(command);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
					p.getOutputStream()));
			BufferedReader br = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			bw.write(text);
			bw.newLine();
			bw.close();

			if (p.waitFor() == 0) {
				String line = "";
				while ((line = br.readLine()) != null) {
					retval.append(line);
				}
			} else {
				System.err.println("Parsing process threw error message ...");
				System.exit(1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return retval.toString();
	}

	private static final HashSet<String> styleSheetArgs;
	static {
		styleSheetArgs = new HashSet<String>();
		styleSheetArgs.add("A0");
		styleSheetArgs.add("C-A0");
		styleSheetArgs.add("A1");
		styleSheetArgs.add("C-A1");
		styleSheetArgs.add("A2");
		styleSheetArgs.add("C-A2");
		styleSheetArgs.add("A3");
		styleSheetArgs.add("C-A3");
		styleSheetArgs.add("AM-NEG");
		styleSheetArgs.add("AM-MNR");
	}

	private String getHTMLResponse(Sentence sen, long parsingTime,
			boolean performURLLookup, boolean includeDependencyGraphImage) {
		StringBuilder ret = new StringBuilder(HTMLHEAD);
		ret.append('\n').append(super.HTML_TOP_EXTRA);
		// ret.append("<html><head><title>Semantic Parser</title>\n"+STYLESHEET+"</head><body>\n");
		ret.append("<table cellpadding=10 cellspacing=1>\n<tr><td class=\"topRowCell\">&nbsp;</td>");
		for (int i = 1; i < sen.size(); ++i) {
			ret.append("<td align=\"center\" class=\"topRowCell\">")
					.append(sen.get(i).getForm()).append("</td>");
		}
		StringBuilder errors = new StringBuilder();
		for (Predicate pred : sen.getPredicates()) {
			int indexCount = 1;
			ret.append("\n<tr><td>");
			String URL = Language.getLanguage().getLexiconURL(pred);
			if (performURLLookup && URL != null && isValidURL(URL)) {
				ret.append("<a href=\"" + URL + "\">");
				ret.append(pred.getSense());
				ret.append("</a>");
			} else {
				ret.append(pred.getSense());
			}

			ret.append("</td>\n");
			// if(pred.getPOS().startsWith("V")){ //Link to propbank
			// ret.append("<a href=\"http://verbs.colorado.edu/propbank/framesets-english/"+pred.getLemma()+"-v.html\">");
			// } else { //Link to Nombank
			// ret.append("<a href=\"http://nlp.cs.nyu.edu/meyers/nombank/nombank.1.0/frames/"+pred.getLemma()+".xml\">");
			// }
			// ret.append(pred.getSense()+"</a></td>");

			SortedSet<Yield> yields = new TreeSet<Yield>();
			Map<Word, String> argmap = pred.getArgMap();
			for (Word arg : argmap.keySet()) {
				yields.addAll(arg.getYield(pred, argmap.get(arg),
						argmap.keySet()).explode());
			}
			for (Yield y : yields) {
				if (!y.isContinuous()) { // Warn the user if we have
											// discontinuous yields
					errors.append("((Discontinous yield of argument '" + y
							+ "' of predicate '" + pred.getSense()
							+ "'. Yield contains tokens [");
					for (Word w : y)
						errors.append("'" + w.getForm() + "', ");
					errors.delete(errors.length() - 2, errors.length());
					errors.append("])).\n");
				}
				int blankColSpan = y.first().getIdx() - indexCount; // sen.indexOf(y.first())-indexCount;
				if (blankColSpan > 0) {
					ret.append("<td colspan=\"").append(blankColSpan)
							.append("\">&nbsp;</td>");
				} else if (blankColSpan < 0) {
					errors.append("Argument '" + y.getArgLabel() + "' of '"
							+ pred.getSense() + "' at index " + indexCount
							+ " overlaps with previous argument(s), ignored.\n");
					continue;
				}
				int argColWidth = y.last().getIdx() - y.first().getIdx() + 1;
				String argLabel = y.getArgLabel();
				ret.append("<td colspan=\"")
						.append(argColWidth)
						.append("\" class=\"")
						.append((styleSheetArgs.contains(argLabel) ? argLabel
								: "ARG_DEFAULT"))
						.append("\" align=\"center\">").append(argLabel)
						.append("</td>");
				indexCount += argColWidth;
				indexCount += blankColSpan;
			}
			if (indexCount < sen.size())
				ret.append("<td colspan=\"" + (sen.size() - indexCount)
						+ "\">&nbsp;</td>");
			ret.append("</tr>");
		}
		ret.append("\n</table><br/>\n");
		ret.append("Parsing sentence required " + parsingTime + "ms.<br/>\n");
		if (errors.length() > 0) {
			ret.append("<br/><hr><br/><font color=\"#FF0000\">Errors</font><br/>");
			ret.append(errors.toString().replace("\n", "<br/>"));
			System.err.println(errors.toString().trim());
		}
		ret.append("<br/>\n<hr/>\n<br/>\n");
		// The dependency graph image
		if (includeDependencyGraphImage) {
			try {
				ByteArrayOutputStream baos = WhatsWrongHelper.renderPNG(
						WhatsWrongHelper.getNLPInstance(sen), 1);
				String key = imageCache.addObject(baos);
				ret.append("<img src=\"/img/" + key + ".png\"/>");
				ret.append("<br/>\n<hr/>\n<br/>\n");
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		// The raw CoNLL 2009 output as a table
		ret.append("<table><tr><th>ID</th><th>Form</th><th>Lemma</th><th>PLemma</th><th>POS</th><th>PPOS</th><th>Feats</th><th>PFeats</th><th>Head</th><th>PHead</th><th>Deprel</th><th>PDeprel</th><th>IsPred</th><th>Pred</th>");
		for (int i = 0; i < sen.getPredicates().size(); ++i)
			ret.append("<th>Args: " + sen.getPredicates().get(i).getSense()
					+ "</th>");
		ret.append("</tr>\n");
		for (String line : sen.toString().split("\n")) {
			ret.append("<tr>");
			int tokIdx = 0;
			for (String token : line.split("\t")) {
				if (tokIdx % 2 == 0 && tokIdx > 1 && tokIdx < 12)
					token = "_";
				ret.append("<td>").append(token).append("</td>");
				++tokIdx;
			}
			ret.append("</tr>\n");
		}
		ret.append("</table>\n<br/><hr/><br/>");
		// ret.append(defaultHandler.pages.get("default"));
		return ret.toString();
	}

	/**
	 * This method tries to make an HTTP request to the given URL and if it
	 * works and the request is OK (i.e. return code 200), it returns true.
	 * Otherwise false.
	 * 
	 * @param url
	 *            the url
	 * @return true if the url resolves and returns properly (i.e. return code
	 *         200), otherwise false
	 */
	private boolean isValidURL(String url) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url)
					.openConnection();
			conn.setRequestMethod("HEAD");
			conn.connect();
			return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void main(String[] args) throws ZipException, IOException,
			ClassNotFoundException {
		HttpOptions options = new HttpOptions();
		options.parseCmdLineArgs(args);

		String error = FileExistenceVerifier
				.verifyCompletePipelineAllNecessaryModelFiles(options);
		if (error != null) {
			System.err.println(error);
			System.err.println("Aborting.");
			System.exit(1);
		}

		Parse.parseOptions = options.getParseOptions();
		Parse.parseOptions.skipPD = true;
		Parse.parseOptions.skipPI = true;
		SemanticRoleLabeler srl;
		if (options.reranker) {
			srl = new Reranker(Parse.parseOptions);
		} else {
			ZipFile zipFile = new ZipFile(Parse.parseOptions.modelFile);
			if (Parse.parseOptions.skipPI) {
				srl = Pipeline.fromZipFile(zipFile, new Step[] { Step.ai,
						Step.ac });// ,Step.po,Step.ao});
			} else {
				srl = Pipeline.fromZipFile(zipFile);
			}
			zipFile.close();
		}
		HttpPipeline.setupHttpPipeline(options, new FramatHttpPipeline(srl,
				HttpPipeline.imageCache, options.l, options.sentenceMaxLength,
				options));
		System.out.println("done.");
	}

}
