package se.lth.cs.srl.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import se.lth.cs.srl.features.Feature;
import se.lth.cs.srl.features.FeatureFile;
import se.lth.cs.srl.features.FeatureGenerator;

public class DumpFeatureSet {

	
	public static void main(String[] args) throws ZipException, IOException, ClassNotFoundException{
		boolean includeAllWords=false;
		//File modelFile=new File(args[0]);
		String modelFile=args[0];
		String featureFileName=args[1];
		if(args.length>2 && args[2].equals("true"))
			includeAllWords=true;
		ZipFile zipFile=new ZipFile(modelFile);
		ObjectInputStream ois=new ObjectInputStream(zipFile.getInputStream(zipFile.getEntry("objects")));
		FeatureGenerator fg=(FeatureGenerator) ois.readObject();
		
		
		BufferedReader in=new BufferedReader(new InputStreamReader(zipFile.getInputStream(zipFile.getEntry(featureFileName))));
		Map<String,List<String>> names=FeatureFile.readFile(in);
		Map<String,List<Feature>> features=new HashMap<String,List<Feature>>();
		for(String POSPrefix:names.keySet()){
			List<Feature> list=new ArrayList<Feature>();
			for(String name:names.get(POSPrefix))
				list.add(fg.getCachedFeature(name));
			features.put(POSPrefix,list);
		}

		for(String POSPrefix:features.keySet()){
			System.out.println("Dumping feature set for POSPrefix '"+POSPrefix+"'");
			int offset=0;
			for(Feature f:features.get(POSPrefix)){
				System.out.println("Feature: "+f);
				System.out.println("Starting with offset"+offset);
				Map<String,Integer> map=f.getMap();
				int size=f.size(includeAllWords);
				SortedMap<Integer,String> sortedMap=new TreeMap<Integer,String>();
				for(String s:map.keySet()){
					Integer i=map.get(s);
					if(i<size)
						sortedMap.put(i, s);
				}
				for(Integer i:sortedMap.keySet()){
					System.out.println(offset+i+"\t"+sortedMap.get(i));
				}
				offset+=size;
			}
		}
	}
	
	
	
	
	
}
