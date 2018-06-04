package pcd.ass03.chat.messages;

import java.io.Serializable;

/**
 * Message sent from a client as acknowledge for the broadcast one originally sent.</br>
 * It contains the current logical clock value of the receiver.
 */
public final class TimestampClientMsg implements Serializable {

	private static final long serialVersionUID = -423120451638650777L;
	
	private final ClientMsg message;
	private final int logicalTime;
	
	public TimestampClientMsg(final ClientMsg message, final int logicalTime) {
		this.message = message;
		this.logicalTime = logicalTime;
	}
	
	/**
	 * @return the message to which it refers
	 */
	public ClientMsg getMessage() {
		return this.message;
	}
	
	/**
	 * @return the client logical time associated to the message
	 */
	public int getLogicalTime() {
		return this.logicalTime;
	}
}

