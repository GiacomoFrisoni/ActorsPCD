package pcd.ass03.chat.actors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * This actor represents the chat register.
 * It is known by all clients at start.
 *
 */
public class RegisterActor extends AbstractActor {

	private final Map<ActorRef, String> clientRefs;
	
	private final LoggingAdapter log;
	
	
	/**
	 * Message received when a new client tries to join the chat.
	 */
	public static final class ClientLoginMsg implements Serializable {
		
		private static final long serialVersionUID = -9000555631707665945L;
		
		private final ActorRef clientRef;
		private final String username;
		
		public ClientLoginMsg (final ActorRef clientRef, final String username) {
			this.clientRef = clientRef;
			this.username = username;
		}

		/**
		 * @return the reference to the client actor that wants to join the chat
		 */
		public ActorRef getClientRef() {
			return clientRef;
		}

		/**
		 * @return the username of the client actor that wants to join the chat
		 */
		public String getUsername() {
			return username;
		}	
	}

	/**
	 * Message received when a client exits the chat.
	 */
	public static final class ClientLogoutMsg implements Serializable {
		
		private static final long serialVersionUID = -6642450939531203193L;
		
		private final ActorRef clientRef;
		
		public ClientLogoutMsg(final ActorRef clientRef) {
			this.clientRef = clientRef;
		}
		
		/**
		 * @return the reference to the client actor that has left the chat
		 */
		public ActorRef getClientRef() {
			return this.clientRef;
		}
	}
	
	
	/**
	 * Creates Props for a register actor.
	 * 
	 * @return a Props for creating register actor, which can then be further configured
	 */
	public static Props props() {
		return Props.create(RegisterActor.class);
	}
	
	/**
	 * Creates a register actor.
	 */
	public RegisterActor() {
		this.clientRefs = new HashMap<>();
		
		this.log = Logging.getLogger(getContext().getSystem(), this);
		
		System.out.println("Waiting for clients to join...");
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				// A new client has just logged in!
				.match(ClientLoginMsg.class, loginMsg -> {
					// Tells the joined client actor about all existing ones
					loginMsg.getClientRef().tell(new ClientActor.ExistingLoggedInClientsMsg(this.clientRefs), ActorRef.noSender());
					// Watches the new client actor for dying connection or disconnect
					getContext().watch(loginMsg.getClientRef());
					// Tells all the actors that there is a new joined client
					this.clientRefs.forEach((clientRef, username) -> {
						clientRef.tell(new ClientActor.LoggedInClientMsg(loginMsg.getClientRef(), loginMsg.getUsername()), ActorRef.noSender());
					});
					// Registers the new arrival
					this.clientRefs.put(loginMsg.getClientRef(), loginMsg.getUsername());
					
					final StringBuilder builder = new StringBuilder();
					builder.append("[IN] New client connected: " + loginMsg.getClientRef() + "(" + loginMsg.getUsername() +")");
					builder.append("\nClients connected:");
					this.clientRefs.forEach((clientRef, username) -> { builder.append("\n" + clientRef + "(" + username + ")"); });
					builder.append("\n");
					System.out.println(builder.toString());
				})
				// A client has just logged out!
				.match(ClientLogoutMsg.class, logoutMsg -> {
					removeActor(logoutMsg.getClientRef());
				})
				// A client died
				.match(Terminated.class, terminatedMsg -> {
					removeActor(terminatedMsg.getActor());
				})
				.matchAny(msg -> log.info("Received unknown message: " + msg))			
				.build();
	}
	
	
	/**
	 * Removes the actor from the matrix and update all existing actors there is no more the eliminated one
	 * @param actorRef
	 * 		Actor that logged out (or died in some way)
	 */
	private void removeActor(final ActorRef actorRef) {
		//First delete it from its matrix
		this.clientRefs.remove(actorRef);
		
		//Then tell all remaining actor that someone has logged out
		this.clientRefs.forEach((actor, username) -> {
			actor.tell(new ClientActor.LoggedOutClientMsg(actorRef), ActorRef.noSender());
		});
		
		final StringBuilder builder = new StringBuilder();
		builder.append("[OUT] New client disconnected: " + actorRef);
		builder.append("\nClients connected:");
		this.clientRefs.forEach((actor, username) -> { builder.append("\n" + actor + "(" + username + ")"); });
		builder.append("\n");
		System.out.println(builder.toString());
	}

}
