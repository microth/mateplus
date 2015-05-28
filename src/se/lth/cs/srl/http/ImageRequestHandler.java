package se.lth.cs.srl.http;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ImageRequestHandler implements HttpHandler {

	private ImageCache imageCache;
	
	public ImageRequestHandler(ImageCache imageCache) {
		this.imageCache=imageCache;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		URI u=exchange.getRequestURI();
		String path=u.getPath();
		String key=path.substring(path.lastIndexOf("/")+1,path.length()-4);
		ByteArrayOutputStream baos=(ByteArrayOutputStream) imageCache.getObject(key);
		byte[] bytes=baos.toByteArray();
		exchange.getResponseHeaders().add("Content-type","image/png");
		exchange.sendResponseHeaders(200,bytes.length);
		OutputStream os=new BufferedOutputStream(exchange.getResponseBody());
		os.write(bytes);
		os.close();
	}

}
