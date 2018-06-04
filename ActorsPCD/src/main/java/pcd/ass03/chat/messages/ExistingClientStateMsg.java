package pcd.ass03.chat.messages;

import java.io.Serializable;

/**
 * Message sent from an existing client to a new one when it discovers that it has joined
 * the chat.</br>With this message the new client will understand if there is already someone
 * in critical section.
 */
public final class ExistingClientStateMsg implements Serializable {
	
	private static final long serialVersionUID = 9188409704306424081L;
	
	private final boolean isClientInCS; 
	
	public ExistingClientStateMsg(final boolean isClientInCS) {
		this.isClientInCS = isClientInCS;
	}
	
	/**
	 * @return true if the already logged client is in critical section, false otherwise
	 */
	public boolean isClientInCriticalSection() {
		return this.isClientInCS;
	}
}