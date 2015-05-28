package se.lth.cs.srl.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;

import se.lth.cs.srl.options.HttpOptions;

import com.sun.net.httpserver.HttpServer;

public class HttpPipeline {

	private static HttpServer server;	
	public static final Date serverStart=new Date();
	public static final ImageCache imageCache=new ImageCache(1000*60*60,1000*60*60,1000);
	
	
	private HttpPipeline(AbstractPipeline pipeline,HttpOptions options) throws IOException{
		DefaultHandler defaultHandler=pipeline.getDefaultHandler();
		server=HttpServer.create(new InetSocketAddress(options.port),0);
		server.createContext("/",defaultHandler);
		server.start();
		System.out.println("Server up and listening on port "+options.port);
		System.out.println("Setting up pipeline");
		ParseRequestHandler parseHandler=new ParseRequestHandler(defaultHandler, pipeline);
		server.createContext("/parse",parseHandler);
		ParserStatusHandler statusHandler=new ParserStatusHandler(pipeline);
		server.createContext("/status",statusHandler);
		ImageRequestHandler imgHandler=new ImageRequestHandler(imageCache);
		server.createContext("/img",imgHandler);
	}
	
	private static HttpPipeline h;
	public static void setupHttpPipeline(HttpOptions options,AbstractPipeline pipeline){
		if(h!=null)
			throw new Error("don't go here twice");
		try {
			h=new HttpPipeline(pipeline,options);
		} catch(Exception e){
			System.out.println("Caught exception while setting up server, exiting.");
			e.printStackTrace(System.out);
			System.out.println();
			if(server!=null){
				server.stop(0);
			}
		}
	}

}
