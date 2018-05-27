package pcd.ass03.chat.utilities;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import akka.actor.ActorRef;

public class ClientKnowledge implements Comparable<ClientKnowledge> {

	private Map<Pair<ActorRef, ActorRef>, Integer> knowledge;
	
	public ClientKnowledge() {
		this.knowledge = new HashMap<>();
	}
	
	public ClientKnowledge(final ClientKnowledge knowledge) {
		this.knowledge = new HashMap<>(knowledge.getKnowledge());
	}
	
	public int getNumberOfMessagesSent(final ActorRef sender, final ActorRef receiver) {
		return this.knowledge.get(new Pair<>(sender, receiver));
	}
	
	public Map<Pair<ActorRef, ActorRef>, Integer> getKnowledge() {
		return Collections.unmodifiableMap(this.knowledge);
	}
	
	@Override
	public int compareTo(ClientKnowledge otherKnowledge) {
		final Set<Pair<ActorRef, ActorRef>> commonKeysKnowledge = new HashSet<>(this.knowledge.keySet());
		commonKeysKnowledge.retainAll(otherKnowledge.getKnowledge().keySet());
		if (commonKeysKnowledge.stream()
				.allMatch(p -> this.knowledge.get(p) > otherKnowledge.getKnowledge().get(p))) {
			return 1;
		} else if (commonKeysKnowledge.stream()
				.allMatch(p -> this.knowledge.get(p) < otherKnowledge.getKnowledge().get(p))) {
			return -1;
		} else {
			return 0;
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((knowledge == null) ? 0 : knowledge.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof ClientKnowledge
				&& this.knowledge.equals(((ClientKnowledge)obj).knowledge);
	}
	
}
