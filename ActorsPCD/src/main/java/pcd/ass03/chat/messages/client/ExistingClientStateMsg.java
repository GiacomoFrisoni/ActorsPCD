package pcd.ass03.chat.messages.client;

import java.io.Serializable;

import akka.actor.ActorRef;

/**
 * Message sent from an existing client to a new one when it discovers that it has joined
 * the chat.</br>With this message the new client will understand if there is already someone
 * in critical section.
 */
public final class ExistingClientStateMsg implements Serializable {
	
	private static final long serialVersionUID = 9188409704306424081L;
	
	private final ActorRef sender;
	private final boolean isClientInCS; 
	
	public ExistingClientStateMsg(final ActorRef sender, final boolean isClientInCS) {
		this.sender = sender;
		this.isClientInCS = isClientInCS;
	}
	
	/**
	 * @return the reference to the sender that has obtained mutual exclusion
	 */
	public ActorRef getSender() {
		return this.sender;
	}
	
	/**
	 * @return true if the already logged client is in critical section, false otherwise
	 */
	public boolean isClientInCriticalSection() {
		return this.isClientInCS;
	}
}