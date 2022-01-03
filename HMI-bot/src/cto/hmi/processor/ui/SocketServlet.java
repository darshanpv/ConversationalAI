package cto.hmi.processor.ui;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class SocketServlet extends WebSocketServlet {

	private static final long serialVersionUID = 4642631783015520258L;

	// TODO Auto-generated method stub
	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.register(WebSocketInterface.class);
	}

}