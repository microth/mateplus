package se.lth.cs.srl.corpus;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Corpus extends LinkedList<CorpusSentence> {

	String name;
	Map<String, CorefChain> word2chain;
	Map<CorefChain, List<String>> chain2words;

	public Map<Integer, CorefChain> chains;

	public Corpus(String name) {
		this.name = name;
		chains = new HashMap<Integer, CorefChain>();
		word2chain = new HashMap<String, CorefChain>();
		chain2words = new HashMap<CorefChain, List<String>>();
	}

	public String getName() {
		return name;
	}

	public void addSentence(Sentence s) {
		CorpusSentence cs = new CorpusSentence(s, this);

		for (Word w : cs) {
			if (w.getCorefId() > -1) {
				int key = w.getCorefId();
				if (!chains.containsKey(key))
					chains.put(key, new CorefChain(key));
				chains.get(key).add(this.indexOf(cs) + "." + w.idx);
			}
		}
	}

	public String corefId(Word w) {
		String s = this.indexOf(w.mySentence) + "." + w.idx;
		if (word2chain.containsKey(s)) {
			return word2chain.get(s).getId();
		}
		return "_";
	}

	public void addMention(Sentence sen, int wnum, int corefId) {
		String s = this.indexOf(sen) + "." + wnum;
		if (!chains.containsKey(corefId))
			chains.put(corefId, new CorefChain(corefId));
		chains.get(corefId).add(s);
		word2chain.put(s, chains.get(corefId));
	}

	public void addCoreferenceChainsFromFile(String string) {
		// System.err.println("Overwriting coreference chains ...");
		chains = new HashMap<Integer, CorefChain>();
		word2chain = new HashMap<String, CorefChain>();
		chain2words = new HashMap<CorefChain, List<String>>();

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(string));
			String line = "";
			int chainid = 0;
			while ((line = br.readLine()) != null) {
				String[] indices = line.split(" ");
				if (indices.length < 3)
					continue;

				CorefChain cc = new CorefChain(chainid);
				chains.put(chainid++, cc);

				for (int i = 1; i < indices.length; i++) {
					String[] snum_begin_end = indices[i].split(":");
					CorpusSentence cs = this.get(Integer
							.parseInt(snum_begin_end[0]));
					int index = cs.findhead(
							Integer.parseInt(snum_begin_end[1]),
							Integer.parseInt(snum_begin_end[2]));
					if (index == -1) {
						System.err
								.println("WARNING: No head found for coreference mention");
						continue;
					}
					Word w = cs.get(index);
					cc.add((this.indexOf(cs) + "." + cs.indexOf(w)));
					word2chain
							.put((this.indexOf(cs) + "." + cs.indexOf(w)), cc);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	public String findChainInPrvSentence(String corefId, Sentence s,
			boolean lastsentenceonly) {
		if (corefId.equals("_"))
			return "_";
		int index = this.indexOf(s) - 1;
		for (String w : chains.get(Integer.parseInt(corefId))) {
			// if(w.startsWith(index+".")) {
			String[] snum_wnum = w.split("\\.");
			int snum = Integer.parseInt(snum_wnum[0]);
			if (snum > index || (lastsentenceonly && snum != index))
				continue;

			int wnum = Integer.parseInt(snum_wnum[1]);
			return this.get(snum).get(wnum).getDeprel();
		}
		return "_";
	}

}
