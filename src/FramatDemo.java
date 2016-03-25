import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import se.lth.cs.srl.CompletePipeline;
import se.lth.cs.srl.Parse;
import se.lth.cs.srl.corpus.Corpus;
import se.lth.cs.srl.corpus.CorpusSentence;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.options.CompletePipelineCMDLineOptions;
import se.lth.cs.srl.options.FullPipelineOptions;
import se.lth.cs.srl.options.ParseOptions;
import se.lth.cs.srl.util.ExternalProcesses;


public class FramatDemo {
	private final StanfordCoreNLP preprocessor;
	private final String semafor;
	private final String mstparser;
	private final ExternalProcesses glove;
	
	private static String[] pipelineOptions = new String[]{
			"eng",										// language
			"-lemma", "models/lemma-eng.model",			// lemmatization mdoel
			"-tagger", "models/tagger-eng.model",		// tagger model
			"-parser", "models/parse-eng.model",		// parsing model
			"-srl", "models/srl-TACL15-eng.model",		// SRL model
			"-framenet", "framenet/fndata-1.5/",		// location of FrameNet data
			"-semafor", "localhost 8043",				// location of semafor server 
			"-mst", "localhost 12345",					// location of mstparser server
			"-glove", "glove/",
			"-reranker",								// turn on reranking
	};

	public static void main(String[] args) throws Exception {		
		String text = "This example shows how to use Framat in JAVA.";		
		FramatDemo demo = new FramatDemo(pipelineOptions); // replace with "args" if options from command line are to be used
		
		demo.parse(text);
	}
	
	CompletePipeline pipeline;
	
	public FramatDemo(String[] commandlineoptions) throws ZipException, ClassNotFoundException, IOException {
		FullPipelineOptions options = new CompletePipelineCMDLineOptions();
		options.parseCmdLineArgs(commandlineoptions); // process options
		
		Parse.parseOptions = options.getParseOptions();
		Parse.parseOptions.globalFeats = true; // activate additional global features
		
		// set glove directory if available		
		glove = (options.glovedir!=null)?new ExternalProcesses(options.glovedir):null;		
		
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		preprocessor = new StanfordCoreNLP(props); // initialize preprocessing		
		mstparser = "nc " + options.mstserver;
		semafor = "nc " + options.semaforserver;
		
		pipeline = CompletePipeline.getCompletePipeline(options); // initialize pipeline	
	}
	
	public void parse(String text) throws Exception {
		// apply Stanford CoreNLP as preprocessing
		Annotation document = new Annotation(text);
		preprocessor.annotate(document);		
		
		// create temporary object that contains sentences from the input text in preprocessed form
		Corpus c = new Corpus("tmp");
		for (CoreMap sentence : document.get(SentencesAnnotation.class)) {
			// run MST parser and SEMAFOR (predicate identification+disambiguation) on each sentence
			Sentence s = preparse(sentence);
			
			// create and apply document-specific word vector representations			 
			if(glove!=null) {
				Map<String, Double[]> word2vecs = glove.createvecs(document);			
				for(Word w : s)
					if(word2vecs.containsKey(w.getForm().toLowerCase()))
						w.setRep(word2vecs.get(w.getForm().toLowerCase()));
			}
			
			new CorpusSentence(s, c);
		}
		
		// mark coreferent mentions in the temporary corpus object
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
		
		for(Sentence s : c) {
			// run Framet to do the actual role labeling
			pipeline.srl.parseSentence(s);
		
			System.out.println();
			
			// a sentence is just a list of words
			int size = s.size();
			for(int i = 1; i<size; i++) {
				Word w = s.get(i); // skip word number 0 (ROOT token)
				// each word object contains information about a word's actual word form / lemma / POS
				System.out.println(w.getForm() + "\t " + w.getLemma() + "\t" + w.getPOS());			
			}
			
			System.out.println();
			
			// some words in a sentence are recognized as predicates
			for(Predicate p : s.getPredicates()) {
				// every predicate has a sense that defines its semantic frame
				System.out.println(p.getForm() + " (" + p.getSense()+ ")");
				// show arguments from the semantic frame that are instantiated in a sentence 
				for(Word arg : p.getArgMap().keySet()) {
					System.out.print("\t" + p.getArgMap().get(arg) + ":");
					// "arg" is just the syntactic head word; let's iterate through all words in the argument span
					for(Word w : arg.getSpan())
						System.out.print(" " + w.getForm());				
					System.out.println();
				}
				
				System.out.println();
	
			}
		}
		
	}

	private Sentence preparse(CoreMap sentence) {
		StringBuffer posOutput = new StringBuffer();
		for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
			if (posOutput.length() > 0) {
				posOutput.append(" ");
			}
			posOutput.append(token.word());
			posOutput.append("_");
			posOutput.append(token.tag());
		}
		
		String parse = ExternalProcesses.runProcess(mstparser, posOutput.toString());
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
		Sentence s = new Sentence(words, lemmas, tags, morphs);
		s.setHeadsAndDeprels(heads, deprels);

		/* add labeled predicates from SEMAFOR */
		String json = ExternalProcesses.runProcess(semafor, parse);
		Pattern pred_frame = Pattern
				.compile("\\{\"target\":\\{\"name\":\"([A-Za-z_]*)\",\"spans\":\\[\\{\"start\":([0-9]*),\"");
		Matcher m = pred_frame.matcher(json);
		while (m.find()) {
			String frame = m.group(1);
			int index = Integer.parseInt(m.group(2));
			System.out.println(index + "\t" + frame);

			s.makePredicate(index + 1);
			((Predicate) s.get(index + 1)).setSense(frame);
		}
		return s;
	}
}
