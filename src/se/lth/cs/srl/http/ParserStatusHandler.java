package se.lth.cs.srl.http;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.sun.net.httpserver.HttpExchange;

public class ParserStatusHandler extends AbstractHandler {

	private static final DateFormat dateformat=new SimpleDateFormat();
	
	public ParserStatusHandler(AbstractPipeline pl){
		super(pl);
	}
		
	public void handle(HttpExchange exchange) throws IOException {
		getContent(exchange); //We dont care about this, but need to read it to be on the safe side
		
		StringBuilder ret=new StringBuilder("Web demo status, ");
		ret.append("server started ").append(dateformat.format(HttpPipeline.serverStart));
		ret.append('\n').append("Demo class: ").append(pipeline.getClass().getCanonicalName());
		ret.append('\n').append("Sentence max length: ").append(pipeline.sentenceMaxLength);
		ret.append("\n\n");

		ret.append(pipeline.getStatusString());
		sendContent(exchange,ret.toString().trim(),"text/plain",200);
	}
}
