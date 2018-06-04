package pcd.ass03.chat.messages.client;

import java.io.Serializable;

import pcd.ass03.chat.messages.BroadcastMsg;

/**
 * Message sent to the clients when the actor that has mutual exclusion lost it.
 */
public final class LostMutualExclusionMsg implements BroadcastMsg, Serializable {
	
	private static final long serialVersionUID = -423120451638650777L;
	
}