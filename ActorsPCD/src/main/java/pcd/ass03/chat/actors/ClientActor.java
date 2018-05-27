package pcd.ass03.chat.actors;

import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pcd.ass03.chat.utilities.ClientKnowledge;
import pcd.ass03.chat.utilities.Pair;

public class ClientActor extends AbstractActor {

	private final ActorSelection registerRef;
	private final ClientKnowledge knowledge;
	
	private final LoggingAdapter log;
	
	public static final class ClientMsg {
		private final ActorRef sender;
		private final String content;
		private final ClientKnowledge knowledge;
		
		public ClientMsg(final ActorRef sender, final String content, final ClientKnowledge knowledge) {
			this.sender = sender;
			this.content = content;
			this.knowledge = knowledge;
		}
		
		public ActorRef getSender() {
			return this.sender;
		}
		
		public String getContent() {
			return this.content;
		}
		
		public ClientKnowledge getKnowledge() {
			return this.knowledge;
		}
	}
	
	public static Props props() {
		return Props.create(ClientActor.class);
	}
	
	public ClientActor() {
		this.knowledge = new ClientKnowledge();
		
		this.log = Logging.getLogger(getContext().getSystem(), this);
		
		// starts and connects the client to the remote server
        this.registerRef = getContext().actorSelection("akka.tcp://ChatSystem@127.0.0.1:4552/user/register");
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(ClientMsg.class, msg -> {
				})
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
	}

}