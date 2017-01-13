package state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.mb3364.twitch.api.Twitch;
import com.mb3364.twitch.api.handlers.ChannelResponseHandler;
import com.mb3364.twitch.api.handlers.StreamResponseHandler;
import com.mb3364.twitch.api.models.Channel;
import com.mb3364.twitch.api.models.Stream;

import logger.Logger;
import messaging.IncomingMessage;
import messaging.OutgoingMessage;

public class ChannelState {

	private static Map<String, ChannelState> channels = new HashMap<String, ChannelState>();
	
	protected Map<String, Long> newSubs = new HashMap<String, Long>();
	protected ArrayList<IncomingMessage> messagesIn = new ArrayList<IncomingMessage>();
	private long lastChannelMessage = 0;
	
	private String channelName;
	private Twitch twitchAPI;
	private Channel twitchChannel;
	private Stream stream;
	private long lastStreamStart = 0;
	private long lastStreamEnd = 0;
	
	private BlockingQueue<IncomingMessage> messageFromIRC = 	new LinkedBlockingQueue<IncomingMessage>();
	private BlockingQueue<IncomingMessage> messageToProc = 		new LinkedBlockingQueue<IncomingMessage>();
	private BlockingQueue<OutgoingMessage> messageFromProc = 	new LinkedBlockingQueue<OutgoingMessage>();
	private BlockingQueue<OutgoingMessage> messageToIRC = 		new LinkedBlockingQueue<OutgoingMessage>();
	private BlockingQueue<OutgoingMessage> messageToWeb = 		new LinkedBlockingQueue<OutgoingMessage>();
	
	private static int loopCounter = 0;
	
	private ChannelState() {
		twitchAPI = new Twitch();
		twitchAPI.setClientId("h6aj63iy3hapxofqywj8qk7uoeey5ww");
	}
	
	public static synchronized void updateStreamObjects(String channel) {
		
		loopCounter++;
		
		if (loopCounter > 15) loopCounter = 0; else return;
		
		
		Logger.TRACE("Updating Stream State Object for " + channel);
		
		ChannelState state = channels.get(channel);
		state.updateObject();

		
		boolean streamLive = (state.stream == null ? false : state.stream.isOnline());
		
		if (streamLive) {
			state.lastStreamEnd = 0;
			
			if (state.lastStreamStart == 0) {
				state.lastStreamStart = System.currentTimeMillis();
			}
			
		} else if (state.lastStreamEnd == 0) {
			state.lastStreamEnd = System.currentTimeMillis();
		}
		
		if (System.currentTimeMillis() - state.lastStreamEnd > 1000*60*30) {
			state.lastStreamStart = 0;
		}
		
	}
	
	public static synchronized boolean isStreamLive(String channel) {
			
		ChannelState state = channels.get(channel);
		return (state.stream == null ? false : state.stream.isOnline());
		
		
	}
	
	public static synchronized int getUptimeMinutes(String channel) {
		long lastStart = channels.get(channel).lastStreamStart;
		if (lastStart > 0) {
			return (int) ((System.currentTimeMillis() - lastStart) / (1000*60));
		}
		return 0;
	}
	
	private synchronized void updateObject() {
		
		twitchAPI.streams().get(channelName, new StreamResponseHandler() {
		    @Override
		    public void onSuccess(Stream s) { stream = s; }

			@Override
			public void onFailure(Throwable arg0) { Logger.WARNING("failed to get channel info"); }

			@Override
			public void onFailure(int arg0, String arg1, String arg2) { Logger.WARNING("failed to get channel info"); }
		});
		twitchAPI.channels().get(channelName, new ChannelResponseHandler() {
		    @Override
		    public void onSuccess(Channel c) { twitchChannel = c; }

			@Override
			public void onFailure(Throwable arg0) { Logger.WARNING("failed to get channel info"); }

			@Override
			public void onFailure(int arg0, String arg1, String arg2) { Logger.WARNING("failed to get channel info"); }
		});
		
	}
	
	public static synchronized void registerChannel(String channelName) {
		Logger.INFO("Registering ChannelState for " + channelName);
		ChannelState c = new ChannelState();
		
		c.channelName = channelName;
		
		channels.put(channelName, c);
		
	}
	
	public static synchronized int getNewSubCount (String channelName) {
		Logger.INFO("Getting subcount for " + channelName);
		return channels.get(channelName).newSubs.size();
	}
	
	public static synchronized void newMessageNotify (String channelName, IncomingMessage message) {
		channels.get(channelName).messagesIn.add(message);
		channels.get(channelName).lastChannelMessage = System.currentTimeMillis();
	}
	
	public static synchronized long getLastMessageTimeAgo (String channelName) {
		long time = channels.get(channelName).lastChannelMessage;
		return System.currentTimeMillis() - time;
	}
	
	public static synchronized ArrayList<IncomingMessage> retrieveMessages (String channelName) {
		@SuppressWarnings("unchecked")
		ArrayList<IncomingMessage> messages = (ArrayList<IncomingMessage>) channels.get(channelName).messagesIn.clone();
		channels.get(channelName).messagesIn.clear();
		return messages;
	}

	public static synchronized Set<String> getChannelList() {
		return channels.keySet();
	}
	
	public static synchronized String getCurrentGame(String channel) {
		if (channels.get(channel) != null && channels.get(channel).twitchChannel != null) {
			return channels.get(channel).twitchChannel.getGame();
		}
		return "";
	}
	
	public static synchronized BlockingQueue<OutgoingMessage> getMessageFromProc(String channelName) { return channels.get(channelName).messageFromProc; }
	public static synchronized BlockingQueue<OutgoingMessage> getMessageToIRC(String channelName) { return channels.get(channelName).messageToIRC; }
	public static synchronized BlockingQueue<IncomingMessage> getMessageToProc(String channelName) { 	return channels.get(channelName).messageToProc; }
	public static synchronized BlockingQueue<IncomingMessage> getMessageFromIRC(String channelName) { 	return channels.get(channelName).messageFromIRC; }
	public static synchronized BlockingQueue<OutgoingMessage> getMessageToWeb(String channelName) { return channels.get(channelName).messageToWeb; }

	

}