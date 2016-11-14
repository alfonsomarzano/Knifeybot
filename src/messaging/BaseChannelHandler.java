package messaging;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import logger.Logger;
import processing.ProcManager;
import state.ChannelState;
import utils.Constants;

public class BaseChannelHandler implements IChannelHandler {
	
	private final String channel;
	private ProcManager p;
	private ISender ircSender;
	private IReceiver ircReceiver;
	
	private boolean continueRunning = true;
	
	public BaseChannelHandler(String channel) {
		this.channel = channel;
		ChannelState.registerChannel(channel);
	}

	@Override
	public void run() {

		BlockingQueue<OutgoingMessage> messageToWeb = ChannelState.getMessageToWeb(channel);
		BlockingQueue<OutgoingMessage> messageToIRC = ChannelState.getMessageToIRC(channel);
		BlockingQueue<IncomingMessage> messageToProc = ChannelState.getMessageToProc(channel);
		BlockingQueue<IncomingMessage> messageFromIRC = ChannelState.getMessageFromIRC(channel);
		BlockingQueue<OutgoingMessage> messageFromProc = ChannelState.getMessageFromProc(channel);
		
		Socket socket = null;
		
		try {
	        socket = new Socket(Constants.server, Constants.port);
		} catch (Exception e) {
			Logger.STACK("Error creating socket", e);
			return;
		}
		
		Logger.DEBUG("Creating IRC and processor threads for " + this.getChannel());
		ircSender	= new Sender( socket, messageToIRC);
		ircReceiver = new Receiver( socket, messageFromIRC);
		p = new ProcManager( messageToProc, messageFromProc, this.getChannel());
		
		
		new Thread(ircSender).start();
		new Thread(ircReceiver).start();
		p.start();
		
		Logger.DEBUG("Entering message passing loop");
		
		while (continueRunning) {

			IncomingMessage messageIRCIn = messageFromIRC.poll();

			if (messageIRCIn != null) {
				
				switch(messageIRCIn.getType()) {
					case IRCPING: case IRCINFO: case IRCCHAT: case IRCJOIN: case IRCPART: case COMMAND: case POKEMON:
						ChannelState.newMessageNotify(channel, messageIRCIn);
						messageToProc.add(messageIRCIn); break;
					default:
						break;
				}
				
			}
			
			OutgoingMessage messageProcOut = messageFromProc.poll();

			if (messageProcOut != null) {
				
				switch(messageProcOut.type) {
					case CHAT: case PONG: case RAW:
						System.out.println("Add messageCHAT");
						messageToIRC.add(messageProcOut); break;
					case COMMAND:
						System.out.println("Add messageWEB");
						messageToWeb.add(messageProcOut); break;
					default:
						break;
				}
				
			}
			
			try { Thread.sleep(20); } catch (InterruptedException e) { Logger.STACK("", e); }
			
		}
		
		ircSender.endExecution();
		ircReceiver.endExecution();
		p.endExecution();
		
	}

	@Override
	public String getChannel() { return channel; }

	@Override
	public void terminateExecution() { this.continueRunning = false; }
	
}