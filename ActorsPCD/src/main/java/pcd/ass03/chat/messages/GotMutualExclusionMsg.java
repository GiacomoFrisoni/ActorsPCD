package pcd.ass03.chat.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

/**
 * Message sent by a client to notify other clients about its entrance into a critical section.
 */
public final class GotMutualExclusionMsg implements BroadcastMsg, Serializable {
	
	private static final long serialVersionUID = -423120451638650777L;
	
	private final ActorRef sender;
	
	public GotMutualExclusionMsg(final ActorRef sender) {
		this.sender = sender;
	}
	
	/**
	 * @return the reference to the sender that has obtained mutual exclusion
	 */
	public ActorRef getSender() {
		return this.sender;
	}
}
