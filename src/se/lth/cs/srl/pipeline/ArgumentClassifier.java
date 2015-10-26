package se.lth.cs.srl.pipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import se.lth.cs.srl.Learn;
import se.lth.cs.srl.Parse;
import se.lth.cs.srl.corpus.ArgMap;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.features.Feature;
import se.lth.cs.srl.features.FeatureSet;
import uk.ac.ed.inf.srl.ml.Model;
import uk.ac.ed.inf.srl.ml.liblinear.Label;

public class ArgumentClassifier extends ArgumentStep {

	private static final String FILEPREFIX = "ac_";

	private List<String> argLabels;

	private Map<String, List<String>> roles;

	public ArgumentClassifier(FeatureSet fs, List<String> argLabels) {
		super(fs);
		this.argLabels = argLabels;
		if (Parse.parseOptions != null && Parse.parseOptions.framenetdir != null)
			roles = createLexicon(Parse.parseOptions.framenetdir + "/frame/");
		else if (Learn.learnOptions != null && Learn.learnOptions.framenetdir != null)
			roles = createLexicon(Learn.learnOptions.framenetdir + "/frame/");
	}

	private Map<String, List<String>> createLexicon(String lexicondir) {
		Map<String, List<String>> retval = new HashMap<String, List<String>>();
		File[] files = new File(lexicondir).listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}
		});

		BufferedReader br = null;
		for (File f : files) {
			try {
				br = new BufferedReader(new FileReader(f));
				String framename = f.getName().replaceAll("\\..*", "");
				String line = "";
				List<String> FEs = new LinkedList<String>();
				while ((line = br.readLine()) != null) {
					if (!line.contains("<FE "))
						continue;
					String FE = line.replaceAll(".*name=\"", "").replaceAll(
							"\".*", "");
					FEs.add(FE);
				}
				retval.put(framename, FEs);
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

		return retval;
	}

	@Override
	public void extractInstances(Sentence s) {
		for (Predicate pred : s.getPredicates()) {
			String POSPrefix = getPOSPrefix(pred.getPOS());
			if (POSPrefix == null) {
				if (Learn.learnOptions.skipNonMatchingPredicates) {
					continue;
				} else {
					POSPrefix = featureSet.POSPrefixes[0];
				}
			}

			/** System.err.println(POSPrefix + " " + pred.getForm());/ **/

			for (Word arg : pred.getArgMap().keySet()) {

				/**
				 * Collection<Integer> indices = new TreeSet<Integer>();
				 * Map<Integer, Double> nonbinFeats = new TreeMap<Integer,
				 * Double>();
				 * collectRestrictedFeatures(pred,arg,POSPrefix,indices,
				 * nonbinFeats, new String[]{ "ArgPOS", "Position", // 3
				 * "ArgDeprel", "PredPOS", "RightPOS", "PredParentPOS",
				 * "LeftPOS", // 47 "ArgWord", "PredLemma", "PredParentWord",
				 * "PredLemmaSense", "ChildDepSet", // 40 "ChildPOSSet", // 47
				 * "LeftSiblingPOS", "RightSiblingPOS", "DeprelPath" }); /
				 **/

				// what this step should actually be doing...
				/**/super.addInstance(pred, arg);/**/
			}
		}
	}

	private void collectRestrictedFeatures(Predicate pred, Word arg,
			String POSPrefix, Collection<Integer> indices,
			Map<Integer, Double> nonbinFeats, String[] strings) {

		if (POSPrefix != null) {
			String label = pred.getArgMap().get(arg);
			if (label != null/* && label.matches("A[0-5]") */) {
				if (arg.getPOS().equals("IN") || arg.getPOS().equals("TO"))
					System.err.print(label
							+ " "
							+ arg.getForm()
							+ (arg.getChildren().size() > 0 ? "."
									+ arg.getChildren().iterator().next()
											.getForm() : ""));
				else
					System.err.print(label + " " + arg.getForm());

				Integer offset = 0;
				for (String s : strings) {
					indices = new TreeSet<Integer>();
					for (Feature f : featureSet.get(POSPrefix)) {
						if (f.getName().equals(s)) {
							f.addFeatures(indices, nonbinFeats, pred, arg,
									offset, false);
							offset += f.size(false);
						}
					}

					if (indices.size() > 0) {
						int c = 0;
						for (Integer i : indices) {
							if (c++ > 0)
								System.err.print(".");
							else
								System.err.print(" ");
							System.err.print(i);
						}
					} else {
						System.err.print(" 0");
					}
				}
				System.err.println();
			}
		}

		return;
	}

	@Override
	public void parse(Sentence s) {
		int changes = 1;
		while (changes > 0) {
			changes = 0;
			for (Predicate pred : s.getPredicates()) {
				Map<Word, String> argMap = pred.getArgMap();
				/** if(pred.getCandSenses()<2) { **/
				for (Word arg : argMap.keySet()) {
					if ((Parse.parseOptions != null && Parse.parseOptions.framenetdir ==null)
							|| ((Learn.learnOptions != null && Learn.learnOptions.framenetdir==null))) {
						Integer label = super.classifyInstance(pred, arg);
						if (!argLabels.get(label).equals(argMap.get(arg)))
							changes++;
						argMap.put(arg, argLabels.get(label));
					} else {
						// modified
						String POSPrefix = getPOSPrefix(pred.getPOS());
						if (POSPrefix == null) {
							POSPrefix = featureSet.POSPrefixes[0];
						}
						Model m = models.get(POSPrefix);
						Collection<Integer> indices = new TreeSet<Integer>();
						Map<Integer, Double> nonbinFeats = new TreeMap<Integer, Double>();
						collectFeatures(pred, arg, POSPrefix, indices,
								nonbinFeats);
						List<uk.ac.ed.inf.srl.ml.liblinear.Label> labels = m
								.classifyProb(indices, nonbinFeats);

						for (uk.ac.ed.inf.srl.ml.liblinear.Label l : labels) {
							// uk.ac.ed.inf.srl.ml.liblinear.Label l =
							// labels.get(0);
							String tmp = argLabels.get(l.getLabel());
							// System.err.println(pred.getSense());
							if (!roles.containsKey(pred.getSense())) {
								if (!tmp.equals(argMap.get(arg)))
									changes++;
								argMap.put(arg, tmp);
								System.err.println("Frame not found: "
										+ pred.getSense());
								break;
							}
							if (roles.get(pred.getSense()).contains(tmp)) {
								if (!tmp.equals(argMap.get(arg)))
									changes++;
								argMap.put(arg, tmp);
								// scores[i] += l.getProb();
								break;
							}
						}
					}

				}
			}

			/**
			 * }
			 * 
			 * System.err.println("I got here somehow! =)");
			 * 
			 * double[] scores = new double[pred.getCandSenses()];
			 * List<Map<Word,String>> argmaps= new
			 * ArrayList<Map<Word,String>>(pred.getCandSenses()); for(int
			 * i=scores.length-1; i>=0; i--) { scores[i] = 0.0; argmaps.add(new
			 * HashMap<Word, String>()); }
			 * 
			 * String POSPrefix=getPOSPrefix(pred.getPOS());
			 * if(POSPrefix==null){ POSPrefix=featureSet.POSPrefixes[0]; } Model
			 * m=models.get(POSPrefix);
			 * 
			 * 
			 * for(int i=scores.length-1; i>=0; i--) { String sense =
			 * pred.getCandSense(i); pred.setSense(sense); Map<Word,String> map
			 * = argmaps.get(i);
			 * 
			 * for(Word arg:pred.getCandArgMap(i).keySet()){ Collection<Integer>
			 * indices = new TreeSet<Integer>(); Map<Integer, Double>
			 * nonbinFeats = new TreeMap<Integer, Double>();
			 * collectFeatures(pred,arg,POSPrefix,indices, nonbinFeats);
			 * List<se.lth.cs.srl.ml.liblinear.Label> labels =
			 * m.classifyProb(indices, nonbinFeats);
			 * 
			 * //for(se.lth.cs.srl.ml.liblinear.Label l : labels) {
			 * se.lth.cs.srl.ml.liblinear.Label l = labels.get(0); String tmp =
			 * argLabels.get(l.getLabel()); //
			 * if(roles.get(sense).contains(tmp)) { map.put(arg, tmp); scores[i]
			 * += l.getProb(); // break; // } //} }
			 * scores[i]*=Math.pow(scores[i],1.0/(double)map.size());
			 * scores[i]*=pred.getCandSenseScore(i); }
			 * 
			 * pred.setArgMap(argmaps.get(0)); for(int i=1; i<scores.length;
			 * i++) { if(scores[i] > scores[0]) {
			 * pred.setArgMap(argmaps.get(i));
			 * pred.setSense(pred.getCandSense(i)); } }
			 **/

		}
	}

	@Override
	protected Integer getLabel(Predicate pred, Word arg) {
		return argLabels.indexOf(pred.getArgMap().get(arg));
	}

	@Override
	public void prepareLearning() {
		super.prepareLearning(FILEPREFIX);
	}

	public void prepareLearning(int i) {
		super.prepareLearning(FILEPREFIX + i);
	}

	@Override
	protected String getModelFileName() {
		return FILEPREFIX + ".models";
	}

	// List<ArgMap> beamSearch(Predicate pred,List<ArgMap> candidates,int
	// beamSize){
	// String POSPrefix=super.getPOSPrefix(pred.getPOS());
	// if(POSPrefix==null)
	// POSPrefix=super.featureSet.POSPrefixes[0]; //TODO fix me. or discard
	// examples with wrong POS-tags
	// Model model=models.get(POSPrefix);
	// Map<Word,List<Label>> wordLabelMapping=new HashMap<Word,List<Label>>();
	// int minSize=999;
	// int maxSize=-1;
	// for(ArgMap argMap:candidates){ //Start by computing the probabilities for
	// the labels for all arguments involved so we dont do this more than once
	// for the same argument
	// for(Word arg:argMap.keySet()){
	// if(!wordLabelMapping.containsKey(arg)){ //Compute and add the
	// probabilities for this
	// Collection<Integer> indices=super.collectIndices(pred, arg, POSPrefix,
	// new TreeSet<Integer>());
	// List<Label> probs=model.classifyProb(indices);
	// wordLabelMapping.put(arg,probs);
	// }
	// }
	// }
	// ArrayList<ArgMap> ret=new ArrayList<ArgMap>();
	// for(ArgMap argMap:candidates){ //Then iterate over each candidate and
	// generate the beamSize best labelings of this candidate.
	//
	// }
	// return ret;
	// }

	List<ArgMap> beamSearch(Predicate pred, List<ArgMap> candidates,
			int beamSize) {
		ArrayList<ArgMap> ret = new ArrayList<ArgMap>();
		String POSPrefix = super.getPOSPrefix(pred.getPOS());
		if (POSPrefix == null)
			POSPrefix = super.featureSet.POSPrefixes[0]; // TODO fix me. or
															// discard examples
															// with wrong
															// POS-tags
		Model model = models.get(POSPrefix);
		for (ArgMap argMap : candidates) { // Candidates from AI module
			ArrayList<ArgMap> branches = new ArrayList<ArgMap>();
			branches.add(argMap);
			SortedSet<ArgMap> newBranches = new TreeSet<ArgMap>(
					ArgMap.REVERSE_PROB_COMPARATOR);
			for (Word arg : argMap.keySet()) { // TODO we can optimize this
												// severely by not computing the
												// labels for the same arg more
												// than once.
				Collection<Integer> indices = new TreeSet<Integer>();
				Map<Integer, Double> nonbinFeats = new HashMap<Integer, Double>();
				super.collectFeatures(pred, arg, POSPrefix, indices,
						nonbinFeats);
				List<Label> probs = model.classifyProb(indices, nonbinFeats);
				for (ArgMap branch : branches) { // Study this branch
					for (int i = 0; i < beamSize && i < probs.size(); ++i) { // and
																				// create
																				// k
																				// new
																				// branches
																				// with
																				// current
																				// arg
						Label label = probs.get(i);
						ArgMap newBranch = new ArgMap(branch);
						newBranch.put(arg, argLabels.get(label.getLabel()),
								label.getProb());
						newBranches.add(newBranch);
					}
				}
				branches.clear();
				Iterator<ArgMap> it = newBranches.iterator();
				for (int i = 0; i < beamSize && it.hasNext(); ++i) {
					ArgMap cur = it.next();
					branches.add(cur);
				}
				newBranches.clear();
			}
			// When this loop finishes, we have the best 4 in branches
			for (int i = 0, size = branches.size(); i < beamSize && i < size; ++i) {
				ArgMap cur = branches.get(i);
				cur.setLblProb(cur.getProb());
				cur.resetProb();
				ret.add(cur);
			}
		}
		return ret;
	}
}
