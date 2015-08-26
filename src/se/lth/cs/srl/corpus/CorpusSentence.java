package se.lth.cs.srl.corpus;

import java.util.LinkedList;
import java.util.List;

public class CorpusSentence extends Sentence {
	
	Corpus c;
	double averageChainSize = -1;
	public Corpus getMyCorpus() {
		return c;
	}
	
	public double averageChainSize() {
		if(averageChainSize<0) {
			double chains = 0.0;
			double total = 0.0;
			int curr_snum = c.indexOf(this);
			for(CorefChain cc : c.chains.values()) {
				double subtotal = 0.0;
				for(String m : cc) {
					int snum = Integer.valueOf(m.split("\\.")[0]);
					if(snum>curr_snum) continue;
					if(snum<curr_snum-2) continue; // only look at previous 2 sentences
					subtotal += 1.0;				
				}
				if(subtotal>0) {
					total += subtotal;
					chains += 1.0;
				}
			}
			if(chains==0.0)
				averageChainSize = 1.0;
			else
				averageChainSize = (total/chains);
		}
		return averageChainSize;
	}
	
	public CorpusSentence(Sentence s, Corpus c) {
		super(s);
		c.add(this);
		this.c = c;
		
		for(int i=1; i<this.size(); i++) {
                    if(this.get(i).getCorefId()>-1)
                        c.addMention(this,i, this.get(i).getCorefId());
		}
	}
	
	public String toSpecialString() {
		String tag;
		StringBuilder ret=new StringBuilder();
		for(int i=1;i<super.size();++i){
			Word w=super.get(i);
			if(i==1 && c.get(0)==this)
				ret.append(c.getName());
			else
				ret.append("_");
			ret.append("\t").append(i).append("\t").append(w.toString());
					
			if(!(w instanceof Predicate)) //If its not a predicate add the FILLPRED and PRED cols
				ret.append("\t_\t_");
			
			// coref ids: a bit of a hack now--they should be stored for each word instead
			ret.append("\t").append(c.corefId(w));
			
			for(int j=0;j<predicates.size();++j){
				ret.append("\t");
				Predicate pred=predicates.get(j);
				ret.append((tag=pred.getArgumentTag(w))!=null?tag:"_");
			}
			ret.append("\n");
		}
		return ret.toString().trim();	
	}

	public int findhead(int begin, int end) {
		List<Integer> currids =  new LinkedList<Integer>();
		//System.err.println(begin + "--" + end);
		for(int i=1; i<this.size(); i++) {
			//System.err.println(this.get(i).begin + ":" + this.get(i).end + "\t" + this.get(i).Form);
			if(this.get(i).begin>=begin && this.get(i).end<=end)
				currids.add(i);
		}
		
		if(currids.size()==0)
			return -1;
		
		if(currids.size()==1) return currids.get(0);		
		if(this.get(currids.get(0)).getPOS().startsWith("V")) return currids.get(0);
		if(this.get(currids.get(0)).getPOS().startsWith("J")) return currids.get(currids.size()-1);
		if(this.get(currids.get(0)).getPOS().startsWith("N")) {
			for(Integer i : currids) {
				if(this.get(i).getForm().equals("of"))
					return currids.get(0);
			}
		}
		
		for(int i=currids.size()-1; i>=0; i--) {
			int newhead = this.get(currids.get(i)).getHeadId();
			if(!currids.contains(newhead))
				return currids.get(i);				
		}
		return -1;
		
		/*int head = currids.get(0);
		
		for(int i=1; i<currids.size(); i++) {
			if(currids.get(i)>head)
				head=currids.get(i);
		}
		
		boolean containshead = true;
		while(containshead) {
			int newhead = this.get(head).getHeadId();
			containshead = false;
			for(int i=0; i<currids.size(); i++)
				if(currids.get(i)==newhead)
					containshead = true;
			if(containshead)
				head = newhead;
		}
		
		return head;*/
	}

	public void setCorpusToNull() {
		c = null;
	}
}
