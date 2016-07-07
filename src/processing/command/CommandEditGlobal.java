package processing.command;

import java.util.concurrent.BlockingQueue;

import channel.ChannelManager;
import messaging.OutgoingMessage;
import messaging.OutgoingMessage.OutType;
import processing.CommandBase;
import users.PermissionClass;

public class CommandEditGlobal extends CommandBase {

	@Override
	public boolean isValid(BlockingQueue<OutgoingMessage> listOut) {
		if (((ProcCommand)parent).getCommand(getToken("alias"), getUser()) == null)	return false;
																									return true;
	}

	@Override
	public boolean execute(BlockingQueue<OutgoingMessage> listOut) {
		
		ChannelManager.addCommand(parent.channel, getToken("alias"), getToken("..."));
		return listOut.add(new OutgoingMessage(OutType.CHAT, getUser() + ", global response \"" + getToken("alias") + "\" edited", parent.channel));
		
	}

	@Override public String getPermissionString() { return "command.editglobal"; }
	@Override public PermissionClass getPermissionClass() { return PermissionClass.Mod; }
	@Override public String getFormatString() 				{ return ":kcommand editglobal <alias> ..."; }
	@Override public String getHelpString() 				{ return "This command edits a response \"...\" when you type :<alias> - This command will be active for every user"; }
	
	

}