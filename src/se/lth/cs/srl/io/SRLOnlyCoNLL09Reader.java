package se.lth.cs.srl.io;

import java.io.File;
import java.io.IOException;

//import se.lth.cs.srl.corpus.Corpus;
//import se.lth.cs.srl.corpus.CorpusSentence;
import se.lth.cs.srl.corpus.Corpus;
import se.lth.cs.srl.corpus.CorpusSentence;
import se.lth.cs.srl.corpus.Sentence;

public class SRLOnlyCoNLL09Reader extends AbstractCoNLL09Reader {

	private Corpus c;
	
	public SRLOnlyCoNLL09Reader(File file) {
		super(file);
	}

	@Override
	protected void readNextSentence() throws IOException {
            String str;
            Sentence sen=null;
            StringBuilder senBuffer=new StringBuilder();
            while ((str = in.readLine()) != null) {
                if(!str.trim().equals("")) {
                    senBuffer.append(str).append("\n");
                } else {
                    if(!senBuffer.toString().startsWith("_") && !senBuffer.toString().matches("^[0-9]*$"))
                        c = new Corpus(senBuffer.toString().split("\t")[0]);
				
				if(c!=null)
					sen=new CorpusSentence(Sentence.newSRLOnlySentence((NEWLINE_PATTERN.split(senBuffer.toString()))),c);
				else
					sen=Sentence.newSRLOnlySentence((NEWLINE_PATTERN.split(senBuffer.toString())));
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
