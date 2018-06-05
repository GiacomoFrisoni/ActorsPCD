package pcd.ass03.chat.messages.register;

import java.io.Serializable;

/**
 * Message sent from the register to the client when one of the clients just logged out,
 * but it doesn't release the mutex lock.</br>
 * With this message the client will delete its internal references to the logged out client.
 */
public class LoggedOutWithMutualExclusionMsg implements Serializable {

	private static final long serialVersionUID = -211710011896901456L;

	private final String username;
	
	public LoggedOutWithMutualExclusionMsg(final String username) {
		this.username = username;
	}

	/**
	 * @return the username to the client that has logged out with mutex from the chat
	 */
	public String getClientUsername() {
		return this.username;
	}
}