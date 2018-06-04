package pcd.ass03.chat.messages;

import java.io.Serializable;

/**
 * Message sent to the clients when the actor that has mutual exclusion lost it.
 */
public final class LostMutualExclusionMsg implements BroadcastMsg, Serializable {
	
	private static final long serialVersionUID = -423120451638650777L;
	
}