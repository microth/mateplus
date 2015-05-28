package se.lth.cs.srl.options;

import java.io.File;

import se.lth.cs.srl.http.HttpPipeline;

public class HttpOptions extends FullPipelineOptions {

	public int sentenceMaxLength=-1;
	public int port=8081;
	public boolean https=false;

	public File pageTopHTMLFile;
	public File pageBottomHTMLFile;
	
	@Override
	String getSubUsageOptions() {
		return "-port   <int>     the port to use (default 8081)";
	}

	@Override
	int trySubParseArg(String[] args, int ai) {
		if(args[ai].equals("-port")){
			ai++;
			port=Integer.valueOf(args[ai]);
			ai++;
		} else if(args[ai].equals("-https")){ //TODO test that this works, and document this. If people want to use HTTPS for some reason.
			ai++;
			https=true;
		} else if(args[ai].equals("-maxLength")){
			ai++;
			sentenceMaxLength=Integer.parseInt(args[ai]);
			ai++;
		} else if(args[ai].equals("-pageTopHTML")){
			ai++;
			pageTopHTMLFile=new File(args[ai]);
			ai++;
		} else if(args[ai].equals("-pageBottomHTML")){
			ai++;
			pageBottomHTMLFile=new File(args[ai]);
			ai++;
		} else if(args[ai].equals("-nullLS")){
			ai++;
			FullPipelineOptions.NULL_LANGUAGE_NAME=args[ai];
			ai++;
		}
		return ai;
	}

	@Override
	Class<?> getIntendedEntryClass() {
		return HttpPipeline.class;
	}

}
