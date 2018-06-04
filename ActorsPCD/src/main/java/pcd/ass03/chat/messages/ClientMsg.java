package pcd.ass03.chat.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

/**
 * Message received from a client with broadcast approach in order to achieve total order
 * delivering.</br><i>The id of a message is composed by the sender reference and an internal
 * number determined by it.</i>
 */
public final class ClientMsg implements Serializable {
	
	private static final long serialVersionUID = 3264368841428605786L;
	
	private final ActorRef sender;
	private final int messageId;
	private final BroadcastMsg message;
	
	public ClientMsg(final ActorRef sender, final int messageId, final BroadcastMsg message) {
		this.sender = sender;
		this.messageId = messageId;
		this.message = message;
	}
	
	/**
	 * @return the reference to the sender of the message
	 */
	public ActorRef getSender() {
		return this.sender;
	}
	
	/**
	 * @return the message to deliver
	 */
	public BroadcastMsg getMessage() {
		return this.message;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + messageId;
		result = prime * result + ((sender == null) ? 0 : sender.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof ClientMsg
				&& this.sender.equals(((ClientMsg)obj).sender)
				&& this.messageId == ((ClientMsg)obj).messageId;
	}
}
