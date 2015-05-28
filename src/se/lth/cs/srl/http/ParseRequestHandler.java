package se.lth.cs.srl.http;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

import se.lth.cs.srl.http.AbstractPipeline.SentenceTooLongException;

import com.sun.net.httpserver.HttpExchange;

public class ParseRequestHandler extends AbstractHandler {
	
	private DefaultHandler defaultHandler;
	
	public ParseRequestHandler(DefaultHandler defaultHandler,AbstractPipeline pipeline) throws IOException{
		super(pipeline);
		this.defaultHandler=defaultHandler;
	}
	
	
	private static final Pattern WHITESPACE_ONLY_PATTERN=Pattern.compile("^\\s+$");
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		//If its not a post request, let the default handler deal with it
		if(!exchange.getRequestMethod().equals("POST")){
			defaultHandler.handle(exchange);
			return;
		}
		
		//Get the parameters from the HTML form
		Map<String,String> vars=AbstractHandler.contentToVariableMap(getContent(exchange));
		String inputSentence=vars.get(sentenceDataVarName);
		//Return if we didnt get a proper sentence to parse
		if(inputSentence==null || inputSentence.length()==0 || WHITESPACE_ONLY_PATTERN.matcher(inputSentence).matches()){
			sendContent(exchange,"Error, invalid request.","text/plain",400);
			return;
		}
		
		String userAgent=AbstractHandler.getHeader(exchange, "User-agent");
		String xForwardedFor=AbstractHandler.getHeader(exchange, "X-forwarded-for"); //Header provided by apache mod_proxy
		
		//Parse
		String httpResponse;
		String content_type;
		System.out.println("@Parsing ``"+inputSentence+"'' at "+new Date(System.currentTimeMillis()));
		System.out.println("Requested by "+exchange.getRemoteAddress()+(xForwardedFor==null?"":" (Forwarded for "+xForwardedFor+")")+". Agent: "+userAgent);
		
		try {
			StringPair res=pipeline.parseRequest(inputSentence,vars);
			httpResponse=res.s1;
			content_type=res.s2;
		} catch (SentenceTooLongException e){
			sendContent(exchange,"Sentence too long ("+e.length+" tokens). Request denied.","text/plain",413);
			System.out.println("Aborting, sentence too long: "+e.length+" tokens.");
			return;
		} catch(Throwable t){
			t.printStackTrace();
			content_type="text/plain";
			httpResponse="Server crashed.";
			sendContent(exchange,httpResponse,content_type,500);
			System.err.println("Server crashed. Exiting.");
			System.exit(1);
		}
		//Return the response to the client
		sendContent(exchange,httpResponse,content_type,200);
	}

	static class StringPair {
		final String s1,s2;
		StringPair(String s1,String s2){
			this.s1=s1;
			this.s2=s2;
		}
	}
}
