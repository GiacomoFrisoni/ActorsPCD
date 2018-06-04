package pcd.ass03.chat.messages.client;

import java.io.Serializable;

import pcd.ass03.chat.messages.BroadcastMsg;

/**
 * Message with textual content that a client wants to display in the chat.</br>
 * <i>No sender username is needed since all other clients know it already internally.</i>
 */
public class ChatMsg implements BroadcastMsg, Serializable {

	private static final long serialVersionUID = -211710011896901456L;

	private final String content;
	
	public ChatMsg(final String content) {
		this.content = content;
	}
	
	/**
	 * @return the content of the chat message
	 */
	public String getContent() {
		return this.content;
	}
}