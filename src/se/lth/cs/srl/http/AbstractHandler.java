package se.lth.cs.srl.http;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class AbstractHandler implements HttpHandler{

	
	private static final Pattern ampPattern=Pattern.compile("&");
	private static final Pattern eqPattern =Pattern.compile("=");
	protected static final String sentenceDataVarName="sentence";
	
	
	protected final AbstractPipeline pipeline;
	
	protected AbstractHandler(AbstractPipeline pipeline){
		this.pipeline=pipeline;
	}
	
	protected String getContent(HttpExchange exchange) throws IOException {
		/*
		 * It would be nice to not assume UTF8 below, but I don't know how to do that.
		 * Maybe escaping is actually done with HTML escapes. I should read up on this.
		 * For now I leave it like this.
		 */
		
		BufferedReader httpInput=new BufferedReader(new InputStreamReader(exchange.getRequestBody(),"UTF-8"));
		StringBuilder in=new StringBuilder();
		String input;
		while((input=httpInput.readLine())!=null){
			in.append(input).append(" ");
		}
		httpInput.close();
		return in.toString().trim();
	}
	
	protected static Map<String,String> contentToVariableMap(String content) throws IOException {
		Map<String,String> ret=new HashMap<String,String>();
		String[] pairs=ampPattern.split(content);
		for(String pair:pairs){
			String[] a=eqPattern.split(pair,2);
			ret.put(URLDecoder.decode(a[0],"UTF-8"),URLDecoder.decode(a[1],"UTF-8"));
		}
		return ret;
	}
	
	protected void sendContent(HttpExchange exchange,String content,String content_type,int responseCode) throws IOException{
		exchange.getResponseHeaders().add("Content-type",content_type);
		byte[] bytes=content.getBytes("UTF-8");
		exchange.sendResponseHeaders(200,bytes.length);
		OutputStream os=new BufferedOutputStream(exchange.getResponseBody());
		os.write(bytes);
		os.close();		
	}
	
	static void dumpHeaders(HttpExchange exchange){
		System.out.println("Headers: ");
		Headers h=exchange.getRequestHeaders();
		for(Entry<String,List<String>> he:h.entrySet()){
			System.out.println(he.getKey());
			for(String v:he.getValue())
				System.out.println("\t"+v);
		}
	}
	
	static String getHeader(HttpExchange exchange,String header){
		Headers h=exchange.getRequestHeaders();
		List<String> values=h.get(header);
		if(values==null || values.size()==0)
			return null;
		return values.get(0);
	}
}
