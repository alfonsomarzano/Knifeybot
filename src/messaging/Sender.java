package messaging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

import logger.Logger;
import utils.TextUtils;

public class Sender implements ISender {

	private BlockingQueue<OutgoingMessage> listOut;
	private BufferedWriter writer;
	private boolean continueRunning = true;
	
	public Sender(Socket socket, BlockingQueue<OutgoingMessage> listOut) {
		
		this.listOut = listOut;
		
		try {
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
		} catch (IOException io) {
			Logger.STACK("Error creating IRC socket writer", io);
		}

	}
	
	@Override
	public synchronized void setSocket(Socket s) { 
		try {
			writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"));
		} catch (IOException io) {
			Logger.STACK("Error creating IRC socket writer", io);
		}
	}
	
	/* (non-Javadoc)
	 * @see messaging.ISender#run()
	 */
	@Override
	public void run() {
		
		while (continueRunning) {
			
			OutgoingMessage message = listOut.poll();
			
			if (message != null) {
				Logger.TRACE(TextUtils.setLength("Snd: " + message.type.toString(), 15) + " - " + message.toString());
				String m = message.toString();
				
				
				try {
					writer.write(m);
					writer.flush();
				} catch (SocketException socket) {
					Logger.STACK("Error writing to IRC socket, reconnecting...", socket);
				} catch (IOException ioex) {
					Logger.STACK("Error writing to IRC socket", ioex);
				}
			}
			
			try { Thread.sleep(3000); } catch (InterruptedException e) { Logger.STACK("", e); }

		}
		
	}

	@Override
	public void endExecution() { continueRunning  = false; }
	
}