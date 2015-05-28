package se.lth.cs.srl.http;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

public class ImageCache {

	private Map<String,Entry> cache;
	private Random generator;
	private CleanUpThread cleanUpThread;
	private LinkedList<Entry> entryQueue;
	private int maximumSize;
	
	private long entryLifeTime;
	
	public ImageCache(long entryLifeTime,long cleanUpCycleTime,int maximumSize){
		this.entryLifeTime=entryLifeTime;
		this.maximumSize=maximumSize;
		cache=new HashMap<String,Entry>();
		generator=new Random();
		cleanUpThread=new CleanUpThread(cleanUpCycleTime);
		entryQueue=new LinkedList<Entry>();
		cleanUpThread.start();
	}
	
	public synchronized Object getObject(String key){
		Entry e=cache.get(key);
		if(e!=null){
			return e.object;
		} else {
			return null;
		}
	}
	
	public synchronized String addObject(Object obj){
		String key=Integer.toString(getRandomKey());
		Entry e=new Entry(key,obj);
		cache.put(key, e);
		entryQueue.add(e);
		if(getSize()>maximumSize){
			cleanUpThread.interrupt();
		}
		return key;
	}
	
	private int getRandomKey(){
		int i;
		do {
			i=generator.nextInt();
		} while(cache.containsKey(Integer.toString(i)));
		return i;
	}
	
	public synchronized void killCleanUpThread(){
		cleanUpThread.kill();
		cleanUpThread.interrupt();
	}
	
	public synchronized int getSize(){
		int cacheSize=cache.size();
		int queueSize=entryQueue.size();
		if(cacheSize!=queueSize){
			throw new Error("Cache and queue don't have equal size in "+ImageCache.class.getName()+", there must be an error in the implementation!");
		}
		return cacheSize;
	}
	
	
	private class Entry {
		private long birth;
		private long earliestTimeOfDeath;
		private String key;
		private Object object;
		
		public Entry(String key,Object value){
			birth=System.currentTimeMillis();
			earliestTimeOfDeath=birth+entryLifeTime;
			this.key=key;
			this.object=value;
		}
	}
	
	private class CleanUpThread extends Thread {

		private long cleanUpCycleTime;
		private boolean alive;
		
		public CleanUpThread(long cleanUpCycleTime) {
			this.cleanUpCycleTime=cleanUpCycleTime;
			alive=true;
		}
		
		public void kill() {
			alive=false;
		}

		public void run(){
			while(alive){
				while(!entryQueue.isEmpty()){
					Entry first=entryQueue.getFirst();
					if(first.earliestTimeOfDeath<System.currentTimeMillis() || entryQueue.size()>maximumSize){
						Entry e=entryQueue.removeFirst();
						cache.remove(e.key);
					} else {
						break;
					}
				}
				try {
					Thread.sleep(cleanUpCycleTime);
				} catch (InterruptedException e) {
					//Do nothing.
				}
			}
		}
	}
}
