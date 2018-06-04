package pcd.ass03.chat.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

/**
 * Message sent from the register to a client when a new one joins the chat.</br>
 * With this message the client will update its internal references.
 */
public class NewLoggedInClientMsg implements BroadcastMsg, Serializable {

	private static final long serialVersionUID = -211710011896901456L;

	private final ActorRef clientRef;
	private final String username;
	
	public NewLoggedInClientMsg(final ActorRef clientRef, final String username) {
		this.clientRef = clientRef;
		this.username = username;
	}

	/**
	 * @return the reference to the client that wants to join the chat
	 */
	public ActorRef getClientRef() {
		return this.clientRef;
	}

	/**
	 * @return the username of the client actor that wants to join the chat
	 */
	public String getUsername() {
		return this.username;
	}	
}
