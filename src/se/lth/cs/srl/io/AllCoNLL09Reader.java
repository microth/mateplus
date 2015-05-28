package se.lth.cs.srl.io;

import java.io.File;
import java.io.IOException;

import se.lth.cs.srl.corpus.Corpus;
import se.lth.cs.srl.corpus.CorpusSentence;
import se.lth.cs.srl.corpus.Sentence;

public class AllCoNLL09Reader extends AbstractCoNLL09Reader {

    private Corpus c;
	
	public AllCoNLL09Reader(File file) {
		super(file);
	}

	protected void readNextSentence() throws IOException{
		String str;
		Sentence sen=null;
		StringBuilder senBuffer=new StringBuilder();
		while ((str = in.readLine()) != null) {
			if(!str.trim().equals("")) {
				senBuffer.append(str).append("\n");
			} else {
                            if(!senBuffer.toString().startsWith("_"))
                            c = new Corpus(senBuffer.toString().split("\t")[0]);
                            sen=new CorpusSentence(Sentence.newSentence((NEWLINE_PATTERN.split(senBuffer.toString()))),c);
                            
                            //sen=Sentence.newSentence((NEWLINE_PATTERN.split(senBuffer.toString())));
                            break;
			}
		}
		if(sen==null){
			nextSen=null;
			in.close();
		} else {
			nextSen=sen;
		}
	}

	
}
