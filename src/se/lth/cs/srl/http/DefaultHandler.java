package se.lth.cs.srl.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import se.lth.cs.srl.languages.Language.L;

import com.sun.net.httpserver.HttpExchange;

public class DefaultHandler extends AbstractHandler {

	protected final Map<String,String> pages=new HashMap<String,String>();
	private final String HTML_HEAD;
	private final String HTML_TAIL;
	
	public DefaultHandler(L currentL,AbstractPipeline pipeline){
		super(pipeline);
		setupPages(currentL); //Note this is a bit nasty, with the pages being static, this being an object setting the language stuff, but we want the pages to be static. Or rather, this is TODO: make the handler non-static and use the object instead.
		HTML_HEAD=pipeline.getHTMLHead();
		HTML_TAIL=pipeline.getHTMLTail();
	}
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
//		if(HttpPipeline.ready){
			servePage("default",exchange);
//		} else {
//			servePage("notReady",exchange);
//		}
	}
	
	private void servePage(String pageName,HttpExchange exchange) throws IOException{
		getContent(exchange); //We dont care about this, but need to read it to be on the safe side
		String content;
		if(pageName.equals("default")){
			content=HTML_HEAD+
					pipeline.HTML_TOP_EXTRA+
					pages.get(pageName)+
					pipeline.HTML_BOTTOM_EXTRA+
					HTML_TAIL;
		} else if(pageName.equals("notReady")){
			content=HTML_HEAD+
					pages.get(pageName)
					+HTML_TAIL;
		} else {
			return; //This is just wrong. We shouldn't enter here.
		}
		sendContent(exchange,content,"text/html; charset=utf-8",200);
	}
	
	private void setupPages(L currentL) {
		pages.put("default",pipeline.getParseInterfaceHTML(currentL));		
		pages.put("notReady",
				  "Pipeline is not loaded yet, please be patient. (Shouldn't be longer than 1-2 minutes, roughly)\n");
	}
}
