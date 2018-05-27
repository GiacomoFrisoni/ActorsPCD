package pcd.ass03.chat.utilities;

import java.util.HashMap;
import java.util.Map;

import akka.actor.ActorRef;

public class ClientKnowledge {

	private Map<Pair<ActorRef, ActorRef>, Integer> knowledge;
	
	public ClientKnowledge() {
		this.knowledge = new HashMap<>();
	}
	
	
}
