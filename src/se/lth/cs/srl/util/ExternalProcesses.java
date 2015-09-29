package se.lth.cs.srl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

public class ExternalProcesses {
	private Map<String, Double[]> initialvecs = null;
	private Map<String, Double[]> squaredgrads = null;
	private Map<String, Integer> vocab = null;
	String glovedir = null;
	
	public static String runProcess(String command, String text) {
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
	
	public ExternalProcesses(String glovedir) {
		this.glovedir = glovedir;
		
		System.err.println("Reading initial word vectors ...");
		initialvecs = new HashMap<String, Double[]>();
		squaredgrads = new HashMap<String, Double[]>();
		vocab = new HashMap<String, Integer>();
		
		BufferedReader br1 = null;
		BufferedReader br2 = null;
		BufferedReader br3 = null;
		try {
			br1 = new BufferedReader(new FileReader(new File(glovedir+"/vectors.txt")));
			br2 = new BufferedReader(new FileReader(new File(glovedir+"/gradsq.txt")));
			br3 = new BufferedReader(new FileReader(new File(glovedir+"/vocab.txt")));
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
	
	public Map<String, Double[]> createvecs(Annotation document) {
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
}
