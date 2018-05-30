package pcd.ass03.chat.actors;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pcd.ass03.chat.utilities.ClientKnowledgeImpl;

/**
 * This actor represents a chat client.
 *
 */
public class ClientActor extends AbstractActorWithStash {

	private final ActorSelection registerRef;
	
	private final String username;
	private ClientKnowledgeImpl knowledge;
	private final Map<ActorRef, String> clientsRefs;
	
	private final LoggingAdapter log;
	
	
	/**
	 * Message sent from the register to the client when a new client joins the chat.</br>
	 * With this message the client will update its internal references to others with this new one.
	 */
	public static final class LoggedInClientMsg implements Serializable {
		
		private static final long serialVersionUID = 4603934889644165530L;
		
		private final ActorRef clientRef;
		private final String username;
		
		public LoggedInClientMsg(final ActorRef clientRef, final String username) {
			this.clientRef = clientRef;
			this.username = username;
		}

		/**
		 * @return the reference to the client that wants to join the chat
		 */
		public ActorRef getClientRef() {
			return this.clientRef;
		}

		/**
		 * @return the username of the actor that wants to join the chat
		 */
		public String getUsername() {
			return this.username;
		}	
	}

	/**
	 * Message sent from the register to a new client when it joins the chat.</br>
	 * With this message the client will set all its internal references to clients already connected.
	 */
	public static final class ExistingLoggedInClientsMsg implements Serializable {
		
		private static final long serialVersionUID = 9188409704306424081L;
		
		private final Map<ActorRef, String> clientRefs;
		
		public ExistingLoggedInClientsMsg(final Map<ActorRef, String> clientRefs) {
			this.clientRefs = clientRefs;
		}
		
		/**
		 * @return the connected clients and their usernames
		 */
		public Map<ActorRef, String> getClientsRefs() {
			return Collections.unmodifiableMap(this.clientRefs);
		}
	}

	/**
	 * Message sent from the register to the client when one of the clients just logged out.</br>
	 * With this message the client will delete the reference to the logged out client.
	 */
	public static final class LoggedOutClientMsg implements Serializable {
		
		private static final long serialVersionUID = -2885971533834333567L;
		
		private final ActorRef clientRef;
		
		public LoggedOutClientMsg(final ActorRef clientRef) {
			this.clientRef = clientRef;
		}
		
		/**
		 * @return the reference for the logged out client
		 */
		public ActorRef getClientRef() {
			return this.clientRef;
		}
	}
	
	/**
	 * Message sent to the client in order to starts its delivering.
	 */
	public static final class SendingRequestMsg implements Serializable {
		
		private static final long serialVersionUID = -1802837382153879964L;
		
		private final String content;
		
		public SendingRequestMsg(final String content) {
			this.content = content;
		}
		
		/**
		 * @return the content of the message
		 */
		public String getContent() {
			return this.content;
		}
	}
	
	/**
	 * Message sent from a client to another client.</br>
	 * <i>No username is needed since all actors knows it already internally</i>
	 */
	public static final class ClientMsg implements Serializable {
		
		private static final long serialVersionUID = 3264368841428605786L;
		
		private final ActorRef sender;
		private final String content;
		private final ClientKnowledgeImpl knowledge;
		
		public ClientMsg(final ActorRef sender, final String content, final ClientKnowledgeImpl knowledge) {
			this.sender = sender;
			this.content = content;
			this.knowledge = knowledge;
		}
		
		/**
		 * @return the reference to the sender of the message
		 */
		public ActorRef getSender() {
			return this.sender;
		}
		
		/**
		 * @return the message content
		 */
		public String getContent() {
			return this.content;
		}
		
		/**
		 * @return knowledge of the sender
		 */
		public ClientKnowledgeImpl getKnowledge() {
			return this.knowledge;
		}
	}

	
	/**
	 * Creates Props for a client actor.
	 * 
	 * @param username
	 * 		the username of the client to be passed to the actor's constructor.
	 * @return a Props for creating client actor, which can then be further configured
	 */
	public static Props props(final String username) {
		return Props.create(ClientActor.class, username);
	}
	
	/**
	 * Creates a client actor.
	 * 
	 * @param username
	 * 		the username of the client
	 */
	public ClientActor(final String username) {
		this.knowledge = new ClientKnowledgeImpl();
		this.clientsRefs = new HashMap<>();
		this.username = username;
		
		this.log = Logging.getLogger(getContext().getSystem(), this);
		
		// Starts and connects the client to the remote server
        this.registerRef = getContext().actorSelection("akka.tcp://ChatSystem@127.0.0.1:4552/user/register");
        this.registerRef.tell(new RegisterActor.ClientLoginMsg(getSelf(), this.username), ActorRef.noSender());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				// Received a sending request in order to start delivering
				.match(SendingRequestMsg.class, msg -> {
					this.clientsRefs.entrySet().forEach(clientRef -> {
						this.knowledge.addMessage(getSelf(), clientRef.getKey());
						clientRef.getKey().tell(new ClientMsg(getSelf(), msg.getContent(), this.knowledge), ActorRef.noSender());
					});
				})
				// Received a new message from another client!
				.match(ClientMsg.class, msg -> {
					/*
					 * - This client should have received all the preceding messages that the sender client sent.
					 * - This client should have received all the messages sent by other clients that the sender client
					 *   knows-of before sending the current message.
					 */
					if ((this.knowledge.getNumberOfMessagesSent(msg.getSender(), getSelf())
							== msg.getKnowledge().getNumberOfMessagesSent(msg.getSender(), getSelf()) - 1)
							|| (this.clientsRefs.keySet().stream()
									.filter(ref -> ref != getSelf())
									.allMatch(ref -> this.knowledge.getNumberOfMessagesSent(ref, getSelf())
											>= msg.getKnowledge().getNumberOfMessagesSent(ref, getSelf())))) {
						// Message received :D
					} else {
						stash();
					}
					// Upon delivery keeps the greatest knowledge
					this.knowledge.maximize(msg.getKnowledge());
				})
				// I'm a new logged client, register is telling me all the existing actors
				.match(ExistingLoggedInClientsMsg.class, existingLoggedInClientsMsg -> {
					this.clientsRefs.clear();
					this.clientsRefs.putAll(existingLoggedInClientsMsg.getClientsRefs());
				})
				// Register is informing me that new client is joining the chat!
				.match(LoggedInClientMsg.class, loggedInClientMsg -> {
					this.clientsRefs.put(loggedInClientMsg.getClientRef(), loggedInClientMsg.getUsername());
				})
				// Register is informing me that a client has left the chat!
				.match(LoggedOutClientMsg.class, loggedOutClientMsg -> {
					this.clientsRefs.remove(loggedOutClientMsg.getClientRef());
				})
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
	}

}