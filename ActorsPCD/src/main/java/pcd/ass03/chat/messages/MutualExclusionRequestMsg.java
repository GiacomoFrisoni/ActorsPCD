package pcd.ass03.chat.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

/**
 * Message sent from a client to the other ones in order to request them the possibility
 * to enter a critical section. 
 */
public final class MutualExclusionRequestMsg implements Serializable {

	private static final long serialVersionUID = -423120451638650777L;
	
	private final ActorRef sender;
	private final int timestamp;
	
	public MutualExclusionRequestMsg(final ActorRef sender, final int timestamp) {
		this.sender = sender;
		this.timestamp = timestamp;
	}
	
	/**
	 * @return the reference to the sender of the request message
	 */
	public ActorRef getSender() {
		return this.sender;
	}
	
	/**
	 * @return the logical time associated to the client that has made the request
	 */
	public int getTimestamp() {
		return this.timestamp;
	}
}