package se.lth.cs.srl.features;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import se.lth.cs.srl.Learn;
import se.lth.cs.srl.Parse;
import se.lth.cs.srl.corpus.Corpus;
import se.lth.cs.srl.corpus.CorpusSentence;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.corpus.Word.WordData;

public class CorefSetFeature extends SetFeature {
	private static final long serialVersionUID = 1L;

	WordData attr;
	boolean barg;
	boolean bpred;
	private Map<String, String> fe2semtype;
	private Map<String, String> fe2superfe;

	protected CorefSetFeature(FeatureName name,WordData attr, boolean pred, boolean arg, boolean usedForPredicateIdentification,String POSPrefix) {
		super(name,true,usedForPredicateIdentification,POSPrefix);
		this.attr=attr;
		this.bpred=pred;
		this.barg=arg;
		if((Parse.parseOptions!=null && Parse.parseOptions.framenet) || 
                   ((Learn.learnOptions!=null && Learn.learnOptions.framenet))) {
			fe2semtype = createSemtypeMapping("/disk/scratch/mroth/framenet/fndata-1.5/");
			fe2superfe = createSuperFEMapping("/disk/scratch/mroth/framenet/fndata-1.5/");
		}
	}
	
	private Map<String, String> createSuperFEMapping(String string) {
		Map<String,String> retval = new HashMap<String, String>();
		BufferedReader br = null;
		try {			
			br = new BufferedReader(new FileReader(string+"frRelation.xml"));
			String line = "";
			String subf=null;
			String supf=null;
			while((line=br.readLine())!=null) {
		        if(line.contains("<frameRelation ")) {
		        	subf = line.replaceAll(".*subFrameName=\"", "").replaceAll("\".*", "");
		        	supf = line.replaceAll(".*superFrameName=\"", "").replaceAll("\".*", "");
		        	continue;
		        }
				if(!line.contains("<FERelation ")) continue;
				String sub = line.replaceAll(".*subFEName=\"", "").replaceAll("\".*", "");
				String sup = line.replaceAll(".*superFEName=\"", "").replaceAll("\".*", ""); 
				//if(!sub.equals(sup))
				retval.put(subf+":"+sub,supf+":"+sup);
			}
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			try {
				br.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		return retval;
	}
	
	private Map<String, String> createSemtypeMapping(String string) {
		Map<String,String> retval = new HashMap<String, String>();
		BufferedReader br = null;
		try {			
			/* read all FE semantic types */
			File[] files = new File(string+"/frame").listFiles(new FilenameFilter() {				
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			});
			for(File f : files) {
				br = new BufferedReader(new FileReader(f));
				String line = "";
				String frame = f.getName().split("\\.")[0];
				String fe = "";
				while((line=br.readLine())!=null) {
					if(line.contains("<FE ")) {
						fe = line.replaceAll(".*name=\"", "").replaceAll("\".*", "");
						continue;
					}
					if(line.contains("<semType ")) {
						line = line.replaceAll(".*name=\"", "").replaceAll("\".*", "");
						retval.put(frame+":"+fe, line);
					}
				}
				try {
					br.close();
				} catch (IOException e) {
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

	@Override
	public String[] getFeatureStrings(Sentence s, int predIndex, int argIndex) {
		return makeFeatureStrings(null, s.get(argIndex)); 
	}

	@Override
	public String[] getFeatureStrings(Predicate pred, Word arg) {
		return makeFeatureStrings(pred, arg);
	}
	
	private String[] makeFeatureStrings(Predicate pred, Word arg){
		if(arg==null) return new String[0];
		
		//if((arg.getPOS().equals("IN") || arg.getPOS().equals("TO")) && !arg.getChildren().isEmpty())
		//	arg = arg.getChildren().iterator().next();
		
		List<String> roles = new LinkedList<String>();
		
		Corpus c = ((CorpusSentence)arg.getMySentence()).getMyCorpus();
		String ccid = c.corefId(arg);
		
		int curr_snum = c.indexOf(arg.getMySentence());
		int curr_wnum = arg.getIdx();
		
		if(ccid.equals("_"))
			return new String[0];
		
		for(String mention : c.chains.get(Integer.parseInt(ccid))) {
			String[] snum_wnum = mention.split("\\.");
			int snum = Integer.parseInt(snum_wnum[0]);
			int wnum = Integer.parseInt(snum_wnum[1]);
			if(snum>curr_snum) continue;
			if(snum<curr_snum-2) continue; // only look at previous 2 sentences
			
			Sentence s = c.get(snum);
			Word w = s.get(wnum);
			
			//if(w.getHead().getPOS().equals("IN") || w.getHead().getPOS().equals("TO"))
			//	w = w.getHead();
			
			for(Predicate p : s.getPredicates()) {
				if(p==pred) continue;
				if(p.getArgMap().containsKey(w)) {
					//System.err.println( (bpred?p.getSense():"") + ":" + (barg?p.getArgMap().get(w):"") );
					if(bpred || semtype(p.getSense()+":"+p.getArgMap().get(w)) != null)
						roles.add( (bpred?p.getSense():"") + ":" + (barg?semtype(p.getSense()+":"+p.getArgMap().get(w))/*.split(":")[1]*/:"") );
				}
			}
		}
	
		String[] ret=new String[roles.size()];
		ret = roles.toArray(ret);

		return ret;		
	}

	@Override
	protected void performFeatureExtraction(Sentence curr_s, boolean allWords) {
		for(int i=1,size=curr_s.size();i<size;++i) {
			Word arg = curr_s.get(i);
			
			//if((arg.getPOS().equals("IN") || arg.getPOS().equals("TO")) && !arg.getChildren().isEmpty())
			//	arg = arg.getChildren().iterator().next();
			
			Corpus c = ((CorpusSentence)arg.getMySentence()).getMyCorpus();
			String ccid = c.corefId(arg);
		
			int curr_snum = c.indexOf(arg.getMySentence());
			int curr_wnum = arg.getIdx();
		
			if(ccid.equals("_"))
				continue;
		
			for(String mention : c.chains.get(Integer.parseInt(ccid))) {
				String[] snum_wnum = mention.split("\\.");
				int snum = Integer.parseInt(snum_wnum[0]);
				int wnum = Integer.parseInt(snum_wnum[1]);
				if(snum>curr_snum) continue;
				if(snum<curr_snum-2) continue; // only look at previous 2 sentences				
				
				Sentence s = c.get(snum);
				Word w = s.get(wnum);
				
				//if(w.getHead().getPOS().equals("IN") || w.getHead().getPOS().equals("TO"))
				//	w = w.getHead();
				
				for(Predicate p : s.getPredicates()) {
					if(p.getArgMap().containsKey(w)) {
						//System.err.println( (bpred?p.getSense():"") + ":" + (barg?p.getArgMap().get(w):"") );
						if(bpred || semtype(p.getSense()+":"+p.getArgMap().get(w)) != null)
							addMap( (bpred?p.getSense():"") + ":" + (barg?semtype(p.getSense()+":"+p.getArgMap().get(w))/*.split(":")[1]*/:"") );
					}
				}
			}
		}

	}

	private String semtype(String role) {
		if((Parse.parseOptions!=null && !Parse.parseOptions.framenet) || 
                   ((Learn.learnOptions!=null && !Learn.learnOptions.framenet)))
                    return role;

		Set<String> visited = new HashSet<String>();
		String type = fe2semtype.containsKey(role)?fe2semtype.get(role):null;
		while(type==null && fe2superfe.containsKey(role)) {
			// hierarchy is cyclic, skip if already visited
			if(visited.contains(role))
				break;
			
			// check next level in the hierarchy
			role = fe2superfe.get(role);			
			if(fe2semtype.containsKey(role))
				type = fe2semtype.get(role);

			visited.add(role);
		}
		if(type!=null) System.err.println(role + " -> " + type);
		return type;
	}
}
