package se.lth.cs.srl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.util.Relation;
import se.lth.cs.srl.util.SentenceAnnotation;

public class AnnotationMerger {

	List<String> annotation1;
	List<String> annotation2;
	boolean entity_agreement; 

	AnnotationMerger () {
		annotation1 = new LinkedList<String>();
		annotation2 = new LinkedList<String>();
		entity_agreement = true;
	}
	
	public void computeKappa() {
		int instances = annotation1.size();
		int agreements = 0;
		int disagreements = 0;
		
		double chance = 0;
		
		for(int i=0; i<instances; i++) {
			if(annotation1.get(i).equals(annotation2.get(i)))
				agreements++;
			else	
				disagreements++;
		}
		
		String[] labels;
		if(entity_agreement)
			labels = new String[]{"None", "Actor", "Action", "Theme", "Property"};
		else
			labels = new String[]{"None", "Property", "Theme", "Actor"};
		
		for(String cat : labels) {
			double chance_of_category = 0.0;
			int count = 0;

			for(int i=0; i<instances; i++) {
				chance_of_category += ( (annotation1.get(i).equals(cat)?1.0:0.0) / (2.0* instances) );
				chance_of_category += ( (annotation2.get(i).equals(cat)?1.0:0.0) / (2.0* instances) );
				
				if(annotation1.get(i).equals(cat) && !annotation2.get(i).equals(cat)) count++;
				if(annotation2.get(i).equals(cat) && !annotation1.get(i).equals(cat)) count++;
			}
			chance += (chance_of_category * chance_of_category);
			
			double kappa_c = 1.0;
			kappa_c = kappa_c - ( (double)count / (2 * instances * 1 * chance_of_category * (1-chance_of_category) ) );

			System.out.println("Kappa (" + cat + "): " + kappa_c);
		}
		
		double agreement = (double)(agreements)/(double)(agreements+disagreements);
		System.out.println("---");
		System.out.println("Raw agreement:    " + agreement);
		System.out.println("Chance agreement: " + chance);
		System.out.println("Kappa: " + (agreement-chance)/(1.0-chance) );
			
		System.out.println("---");
		System.out.println("Absolute disagreements");
		System.out.println("\tProperty vs. null: " + count_confusion("Property", "None"));
		System.out.println("\tProperty vs. Theme: " + count_confusion("Property", "Theme"));
		System.out.println("---");
		System.out.println("Annotations (Action): " + sum_merged("Action"));
		System.out.println("Annotations (Actor): " + sum_merged("Actor"));
		System.out.println("Annotations (Object): " + sum_merged("Theme"));
		System.out.println("Annotations (Property): " + sum_merged("Property"));
		System.out.println("Total annotations: " + sum(annotation1) + "/" + sum(annotation2) );
	}
	
	private int sum_merged(String cat) {
		int retval=0;
		for(int i=0; i<annotation1.size(); i++)
			if(annotation1.get(i).equals(cat) && (annotation2.get(i).equals(cat) || annotation2.get(i).equals("None")) 
					|| (annotation2.get(i).equals(cat) && annotation1.get(i).equals("None"))) retval++;
		return retval;
	}
	
	private int sum(List<String> annotation) {
		int retval=0;
		for(int i=0; i<annotation.size(); i++)
			if(!annotation.get(i).equals("None")) retval++;
		return retval;
	}
	
	private int count_confusion(String s1, String s2) {
		int retval = 0;
		for(int i=0; i<annotation1.size(); i++) {
			if((annotation1.get(i).equals(s1) && annotation2.get(i).equals(s2)) ||
					(annotation2.get(i).equals(s1) && annotation1.get(i).equals(s2)))
				retval++;
		}
		return retval;
	}
	
	public int[] mergeAndApply(SentenceAnnotation anno1, SentenceAnnotation anno2, Sentence sen) {		
		TreeMap<Integer, String> predicates1 = getPredicates(sen, anno1);
		TreeMap<Integer, String> predicates2 = getPredicates(sen, anno2);

		List<Relation> relations1 = getRelations(sen, anno1);
		List<Relation> relations2 = getRelations(sen, anno2);

		
		// add predicates/senses (TreeMap.keySet() is in ascending ordered!) 
		for(Integer i : predicates1.keySet()) {
			sen.makePredicate(i);
			((Predicate)sen.get(i)).setSense(predicates1.get(i));
		}
		for(Integer i : predicates2.keySet()) {
			if(!(sen.get(i) instanceof Predicate)) {
				sen.makePredicate(i);
				((Predicate)sen.get(i)).setSense(predicates2.get(i));
			}
		}
		
		if(entity_agreement) {
			List<String> sen_annotation1 = new LinkedList<String>();
			List<String> sen_annotation2 = new LinkedList<String>();
			
			boolean entity_agreement; 
			
			int sen_size = sen.size();
			for(int i=1; i<sen_size; i++) {
				String rel1 = "None";
				String rel2 = "None";	
				for(Relation r : relations1) {
					//if(r.head==i) { System.err.println(sen.get(i).getForm() + " (" + normalize_label(r.label, true)+")" +
					//									" --"+r.label+"-> " + 
					//								 sen.get(r.dependent).getForm() + " (" + normalize_label(r.label, false)+")"); }
					if(r.head==i && r.label.equals("Property")) continue; // skip for later
					if(r.head==i) 		rel1 = normalize_label(r.label,true);
					if(r.dependent==i)	rel1 = normalize_label(r.label,false);
				}
				for(Relation r : relations2) {
					if(r.head==i && r.label.equals("Property")) continue; // skip for later
					if(r.head==i) 		rel2 = normalize_label(r.label,true);
					if(r.dependent==i)	rel2 = normalize_label(r.label,false);
				}
				sen_annotation1.add(rel1);
				sen_annotation2.add(rel2);			
			}
						
			for(int i=1; i<sen_size; i++) {
				for(Relation r : relations1)
					if(r.head==i && r.label.equals("Property") && sen_annotation1.get(i-1).equals("None")) {
						sen_annotation1.remove(i-1);
						sen_annotation1.add(i-1, "Theme");
					}
				for(Relation r : relations2)				
					if(r.head==i && r.label.equals("Property") && sen_annotation2.get(i-1).equals("None")) {
						sen_annotation2.remove(i-1);
						sen_annotation2.add(i-1, "Theme");
					}
			}
			
			annotation1.addAll(sen_annotation1);
			annotation2.addAll(sen_annotation2);
		
		} else {
			int rel_agreements = 0;
			int rel_disagreements = 0;
			int missing = 0;
			
			Set<Integer> instances = new TreeSet<Integer>();
			for(Relation r : relations1) {
				instances.add(r.head); instances.add(r.dependent); }
			for(Relation r : relations2) {
				instances.add(r.head); instances.add(r.dependent); }
					
			for(int i : instances) {
				for(int j : instances) {
					if(i==j) continue;
					
					String rel1 = "None";
					String rel2 = "None";				
					for(Relation r : relations1)
						if(r.head==i && r.dependent==j) rel1 = r.label;
					for(Relation r : relations2)
						if(r.head==i && r.dependent==j) rel2 = r.label;
					
					annotation1.add(rel1);
					annotation2.add(rel2);
				}
			}
		}
		
		
		// add predicate-argument relationships
		for(Relation r : relations1) {
			((Predicate)sen.get(r.head)).addArgMap(sen.get(r.dependent), r.label);
			
			boolean found = false;
			for(Relation r2 : relations2) {
				if(r.head==r2.head && r.dependent==r2.dependent) {
					found = true;
					
					if(r.label.equals(r2.label)) {
						//rel_agreements++;
					} else {
						//rel_disagreements++;
						System.out.println("DISAGREEMENT: " + sen.get(r.head).getForm() + " --["+ r.label + " vs. " + r2.label + "]--> " + sen.get(r.dependent).getForm());
					}
				}
				if(r.head==r2.dependent && r.dependent==r2.head) {
					//rel_disagreements++;
					System.out.println("DISAGREEMENT: " + sen.get(r.head).getForm() + " <-- ?? --> " + sen.get(r.dependent).getForm());
				}
			}
			//if(!found)
			//	missing++;
		}
		for(Relation r : relations2) {
			boolean found = false;
			for(Relation r2 : relations1) {
				if(r.head==r2.head && r.dependent==r2.dependent) {
					found = true;
				}
			}
			if(!found) {
				//missing++;
				((Predicate)sen.get(r.head)).addArgMap(sen.get(r.dependent), r.label);
			}
		}
		
		//System.out.printf("Raw agreement: %2f\n", (double)(agreements)/(double)(agreements+disagreements) );
		return new int[]{0,0,0/*rel_agreements,rel_disagreements,missing*/};
	}
	
	private String normalize_label(String label, boolean b) {
		if(label.equals("Actor"))
			return b?"Action":"Actor";
		else if(label.equals("Property"))
			return b?"ERROR":"Property";
		else if(label.equals("Theme"))
			return b?"Action":"Theme";
		
		System.err.println("Unknown label: "+label);
		return "";
	}

	private TreeMap<Integer, String> getPredicates(Sentence sen, SentenceAnnotation curr_anno) {
		TreeMap<Integer, String> retval = new TreeMap<Integer, String>();
		
		Map<String,Word> concept2word = new HashMap<String,Word>();
		Map<String,String> concept2label = new HashMap<String,String>();
		List<List<Integer>> wordids;
		
		wordids = new LinkedList<List<Integer>>();
		
		int[] begins = new int[sen.size()-1];
		int[] ends   = new int[sen.size()-1];
		for(int i=0; i<begins.length;i++) {
			Word w = sen.get(i+1);
			begins[i] = w.getBegin();
			ends[i]   = w.getEnd(); /** XXX: if FN, use w.getEnd()-1 instead? **/
		}
		for(String[] anno : curr_anno.conceptAnno) {
			String label       = anno[1].split(" ")[0];
			int startCharacter = Integer.parseInt(anno[1].split(" ")[1]); 
			int endCharacter   = Integer.parseInt(anno[1].split(" ")[2]);
			
			List<Integer> currids = new LinkedList<Integer>();
			for(int i=0; i<begins.length;i++) {
				if(begins[i]>=startCharacter && ends[i]<=endCharacter)
					currids.add(i);
			}
			if(currids.size()==0) {
				System.err.println(sen.toString());
				System.err.println("Error: no matching token found for span from " + startCharacter + ":"+ endCharacter);
				System.err.println("Tokens are:");
				for(int i=0; i<begins.length;i++) {
					System.err.println("  " + begins[i] + ":" + ends[i] + "\t" + sen.get(i+1).getForm());
				}
			} else {
				concept2word.put(anno[0], sen.get(head(sen, currids)+1));
				//System.out.println(anno[2]+" "+ concept2word.get(anno[0]).getForm()); // Sanity check
				concept2label.put(anno[0], label);
			}
		}
	
		// mark predicates and collect predicate-argument relationships
		for(String[] anno : curr_anno.relationAnno) {
			String[] rel_arg1_arg2 = anno[1].split(" ");
			String rel  = rel_arg1_arg2[0];
			String arg1 = rel_arg1_arg2[1].split(":")[1];
			String arg2 = rel_arg1_arg2[2].split(":")[1];
			
			if(concept2word.get(arg1)==null || concept2word.get(arg2)==null)
				continue;
			
			String l1 = concept2label.get(arg1);
			String l2 = concept2label.get(arg2);
			
			int arg1index = concept2word.get(arg1).getIdx();
			//if(!l1.equals("Action") && (sen.get(arg1index).getHead().getPOS().equals("IN")/* || sen.get(arg1index).getHead().getPOS().equals("TO")*/) )
			//	arg1index = sen.get(arg1index).getHeadId();
			int arg2index = concept2word.get(arg2).getIdx();
			//if(!l2.equals("Action") && (sen.get(arg2index).getHead().getPOS().equals("IN")/* || sen.get(arg2index).getHead().getPOS().equals("TO")*/) )
			//	arg2index = sen.get(arg2index).getHeadId();		
			
			/** Resolve disagreements first **
			if(l1.equals("Action") && l2.equals("Object") && ((sen.get(arg2index).getPOS().equals("TO") || sen.get(arg2index).getPOS().equals("IN")) && !sen.get(arg2index).getChildren().isEmpty() ))
				arg2index = sen.get(arg2index).getChildren().iterator().next().getIdx();
			if(l1.equals("Object") && ((sen.get(arg1index).getPOS().equals("TO") || sen.get(arg1index).getPOS().equals("IN")) && !sen.get(arg1index).getChildren().isEmpty() ))
				arg1index = sen.get(arg1index).getChildren().iterator().next().getIdx();
			/**/
			
			// makePredicate needs to be executed in word order
			// -> collect all predicates and their arguments first
			/** for S-CASE **/
			
			if(l1.equals("Action") && l2.equals("Object")) {
				retval.put(arg1index, "Action");
			} else if(l1.equals("Actor") && l2.equals("Action")) {
				retval.put(arg2index, "Action");
			} else if(l1.equals("Action") && l2.equals("Property")) {
				retval.put(arg1index, "Action");
			} else if(l2.equals("Property")) {
				retval.put(arg1index, l1.equals("Actor")?"Object":l1);
			}
		}
		return retval;
	}
	
	private List<Relation> getRelations(Sentence sen, SentenceAnnotation curr_anno) {
		List<Relation> retval = new LinkedList<Relation>();
		Map<String,Word> concept2word = new HashMap<String,Word>();
		Map<String,String> concept2label = new HashMap<String,String>();
		List<List<Integer>> wordids;
		
		wordids = new LinkedList<List<Integer>>();
		
		int[] begins = new int[sen.size()-1];
		int[] ends   = new int[sen.size()-1];
		for(int i=0; i<begins.length;i++) {
			Word w = sen.get(i+1);
			begins[i] = w.getBegin();
			ends[i]   = w.getEnd();
		}
		for(String[] anno : curr_anno.conceptAnno) {
			String label       = anno[1].split(" ")[0];
			int startCharacter = Integer.parseInt(anno[1].split(" ")[1]); 
			int endCharacter   = Integer.parseInt(anno[1].split(" ")[2]);
			
			List<Integer> currids = new LinkedList<Integer>();
			for(int i=0; i<begins.length;i++) {
				if(begins[i]>=startCharacter && ends[i]<=endCharacter)
					currids.add(i);
			}
			if(currids.size()==0) {
				System.err.println(sen.toString());
				System.err.println("Error: no matching token found for span from " + startCharacter + ":"+ endCharacter);
				System.err.println("Tokens are:");
				for(int i=0; i<begins.length;i++) {
					System.err.println("  " + begins[i] + ":" + ends[i] + "\t" + sen.get(i+1).getForm());
				}
			} else {
				concept2word.put(anno[0], sen.get(head(sen, currids)+1));
				concept2label.put(anno[0], label);
			}
		}
	
		// mark predicates and collect predicate-argument relationships
		for(String[] anno : curr_anno.relationAnno) {
			String[] rel_arg1_arg2 = anno[1].split(" ");
			String rel  = rel_arg1_arg2[0];
			String arg1 = rel_arg1_arg2[1].split(":")[1];
			String arg2 = rel_arg1_arg2[2].split(":")[1];
			
			if(concept2word.get(arg1)==null || concept2word.get(arg2)==null)
				continue;
		
			String l1 = concept2label.get(arg1);
			String l2 = concept2label.get(arg2);
			
			int arg1index = concept2word.get(arg1).getIdx();
			//if(!l1.equals("Action") && (sen.get(arg1index).getHead().getPOS().equals("IN")/* || sen.get(arg1index).getHead().getPOS().equals("TO")*/) )
			//	arg1index = sen.get(arg1index).getHeadId();
			int arg2index = concept2word.get(arg2).getIdx();
			//if(!l2.equals("Action") && (sen.get(arg2index).getHead().getPOS().equals("IN")/* || sen.get(arg2index).getHead().getPOS().equals("TO")*/) )
			//	arg2index = sen.get(arg2index).getHeadId();		

			/** Resolve disagreements first **
			if(l1.equals("Action") && l2.equals("Object") && ((sen.get(arg2index).getPOS().equals("TO") || sen.get(arg2index).getPOS().equals("IN")) && !sen.get(arg2index).getChildren().isEmpty() ))
				arg2index = sen.get(arg2index).getChildren().iterator().next().getIdx();
			if(l1.equals("Object") && ((sen.get(arg1index).getPOS().equals("TO") || sen.get(arg1index).getPOS().equals("IN")) && !sen.get(arg1index).getChildren().isEmpty() ))
				arg1index = sen.get(arg1index).getChildren().iterator().next().getIdx();
			/**/
			
			// makePredicate needs to be executed in word order
			// -> collect all predicates and their arguments first
			/** for S-CASE **/
			
			if(l1.equals("Action") && l2.equals("Object")) {
				retval.add(new Relation(arg1index, arg2index, "Theme"));
			} else if(l1.equals("Actor") && l2.equals("Action")) {
				retval.add(new Relation(arg2index, arg1index, "Actor"));
			} else if(l1.equals("Action") && l2.equals("Property")) {
				retval.add(new Relation(arg1index, arg2index, "Property"));
			} else if(l2.equals("Property")) {
				retval.add(new Relation(arg1index, arg2index, "Property"));
			}
		}
		return retval;
	}

	public int head(Sentence sen, List<Integer> currids) {
		int head = currids.get(0);
	
		for(int i=1; i<currids.size(); i++) {
			if(currids.get(i)>head)
				head=currids.get(i);
		}
		
		boolean containshead = true;
		while(containshead) {
			int newhead = sen.get(head+1).getHeadId()-1;
			containshead = false;
			for(int i=0; i<currids.size(); i++)
				if(currids.get(i)==newhead)
					containshead = true;
			if(containshead)
				head = newhead;
		}
		
		//if(sen.get(head).getHead()!=null && sen.get(head).getHead().getPOS().equals("IN"))
		//	head = sen.get(head).getHeadId();
		
		return head;
	}

}
