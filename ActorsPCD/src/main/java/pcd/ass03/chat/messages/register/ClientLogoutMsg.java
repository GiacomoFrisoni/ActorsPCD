package pcd.ass03.chat.messages.register;

import java.io.Serializable;

import akka.actor.ActorRef;

/**
 * Message received when a client exits the chat.
 */
public final class ClientLogoutMsg implements Serializable {
	
	private static final long serialVersionUID = -6642450939531203193L;
	
	private final ActorRef clientRef;
	
	public ClientLogoutMsg(final ActorRef clientRef) {
		this.clientRef = clientRef;
	}
	
	/**
	 * @return the reference to the client actor that has left the chat
	 */
	public ActorRef getClientRef() {
		return this.clientRef;
	}
}