package pcd.ass03.chat.messages.register;

import java.io.Serializable;

import akka.actor.ActorRef;

/**
 * Message received when a new client tries to join the chat.
 */
public final class ClientLoginMsg implements Serializable {
	
	private static final long serialVersionUID = -9000555631707665945L;
	
	private final ActorRef clientRef;
	private final String username;
	
	public ClientLoginMsg (final ActorRef clientRef, final String username) {
		this.clientRef = clientRef;
		this.username = username;
	}

	/**
	 * @return the reference to the client actor that wants to join the chat
	 */
	public ActorRef getClientRef() {
		return clientRef;
	}

	/**
	 * @return the username of the client actor that wants to join the chat
	 */
	public String getUsername() {
		return username;
	}	
}