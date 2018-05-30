package pcd.ass03.chat.actors;

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

public class ClientActor extends AbstractActorWithStash {

	private final ActorSelection registerRef;
	
	private final String username;
	private ClientKnowledgeImpl knowledge;
	private final Map<ActorRef, String> clientsRefs;
	
	private final LoggingAdapter log;
	
	/**
	 * Message sent from register to client when a new actor join the chat. </br>
	 * With this message client will update its internal references to others with this new one.
	 */
	public static final class LoggedInClientMsg {
		private final ActorRef actorRef;
		private final String username;
		
		public LoggedInClientMsg (final ActorRef actorRef, final String username) {
			this.actorRef = actorRef;
			this.username = username;
		}

		/**
		 * Get the reference to actor that wants to join the chat
		 * @return
		 * 		reference to actor that wants to join the chat
		 */
		public ActorRef getActorRef() {
			return actorRef;
		}

		/**
		 * Get the username of the actor that wants to join the chat
		 * @return
		 * 		username of the actor that wants to join the chat
		 */
		public String getUsername() {
			return username;
		}	
	}

	/**
	 * Message sent from register to client when it joins the chat. </br>
	 * With this message client will set all internal references to others already connected.
	 */
	public static final class ExistingLoggedInClientsMsg {
		private final Map<ActorRef, String> actorRefs;
		
		public ExistingLoggedInClientsMsg (final Map<ActorRef, String> actorRefs) {
			this.actorRefs = actorRefs;
		}
		
		/**
		 * Get the map of all actors and their usernames
		 * @return
		 * 		map of all actors and their usernames
		 */
		public Map<ActorRef, String> getActorRefs() {
			return Collections.unmodifiableMap(this.actorRefs);
		}
	}

	/**
	 * Message sent from register to client when one of clients just logged out. </br>
	 * With this message client will delete the reference to the logged out client.
	 */
	public static final class LoggedOutClientMsg {
		private final ActorRef actorRef;
		
		public LoggedOutClientMsg(final ActorRef actorRef) {
			this.actorRef = actorRef;
		}
		
		/**
		 * Get the reference for the logged out client
		 * @return
		 * 		reference for the logged out client
		 */
		public ActorRef getActorRef() {
			return this.actorRef;
		}
	}
	
	public static final class SendingRequestMsg {
		private final String content;
		
		public SendingRequestMsg(final String content) {
			this.content = content;
		}
		
		public String getContent() {
			return this.content;
		}
	}
	
	/**
	 * Message sent from client to client. </br>
	 * <i>No username is needed since all actors knows it already internally</i>
	 */
	public static final class ClientMsg {
		private final ActorRef sender;
		private final String content;
		private final ClientKnowledgeImpl knowledge;
		
		public ClientMsg(final ActorRef sender, final String content, final ClientKnowledgeImpl knowledge) {
			this.sender = sender;
			this.content = content;
			this.knowledge = knowledge;
		}
		
		/**
		 * Get the reference to the sender of the message
		 * @return
		 * 		reference to the sender of the message
		 */
		public ActorRef getSender() {
			return this.sender;
		}
		
		/**
		 * Get the message content
		 * @return
		 * 		message content
		 */
		public String getContent() {
			return this.content;
		}
		
		/**
		 * Get the knowledge of the sender
		 * @return
		 * 		knowledge of the sender
		 */
		public ClientKnowledgeImpl getKnowledge() {
			return this.knowledge;
		}
	}

	
	
	public static Props props(final String username) {
		return Props.create(ClientActor.class);
	}
	
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
				.match(SendingRequestMsg.class, msg -> {
					this.clientsRefs.entrySet().forEach(
						clientRef -> clientRef.getKey().tell(new ClientMsg(getSelf(), msg.getContent(), this.knowledge), ActorRef.noSender())
					);
				})
				//Received a new message from another client!
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
				//I'm a new entry, register is telling me all the existing actors
				.match(ExistingLoggedInClientsMsg.class, existingLoggedInClientsMsg -> {
					this.clientsRefs.clear();
					this.clientsRefs.putAll(existingLoggedInClientsMsg.getActorRefs());
				})
				//Register is informing me that new client is joining the chat!
				.match(LoggedInClientMsg.class, loggedInClientMsg -> {
					this.clientsRefs.put(loggedInClientMsg.getActorRef(), loggedInClientMsg.getUsername());
				})
				//Register is informing me that a client has left the chat!
				.match(LoggedOutClientMsg.class, loggedOutClientMsg -> {
					this.clientsRefs.remove(loggedOutClientMsg.getActorRef());
				})
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
	}

}