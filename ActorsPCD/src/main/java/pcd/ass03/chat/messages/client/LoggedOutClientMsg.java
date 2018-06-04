package pcd.ass03.chat.messages.client;

import java.io.Serializable;

import akka.actor.ActorRef;
import pcd.ass03.chat.messages.BroadcastMsg;

/**
 * Message sent from the register to the client when one of the clients just logged out.</br>
 * With this message the client will delete its internal references to the logged out client.
 */
public class LoggedOutClientMsg implements BroadcastMsg, Serializable {

	private static final long serialVersionUID = -211710011896901456L;

	private final ActorRef clientRef;
	
	public LoggedOutClientMsg(final ActorRef clientRef) {
		this.clientRef = clientRef;
	}

	/**
	 * @return the reference to the client that has logged out from the chat
	 */
	public ActorRef getClientRef() {
		return this.clientRef;
	}
}