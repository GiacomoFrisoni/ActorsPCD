package pcd.ass03.chat.utilities;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import akka.actor.ActorRef;

public class ClientKnowledgeImpl implements ClientKnowledge {

	private Map<Pair<ActorRef, ActorRef>, Integer> knowledge;
	
	public ClientKnowledgeImpl() {
		this.knowledge = new HashMap<>();
	}
	
	@Override
	public void addNewClient(final ActorRef client, final Set<ActorRef> clientsToRelate) {
		for (final ActorRef relatedClient : clientsToRelate) {
			this.knowledge.put(new Pair<>(client, relatedClient), 0);
			this.knowledge.put(new Pair<>(relatedClient, client), 0);
		}
	}
	
	@Override
	public void addNewClients(final Set<ActorRef> clients, final Set<ActorRef> clientsToRelate) {
		for (final ActorRef client : clients) {
			addNewClient(client, clientsToRelate);
		}
	}
	
	@Override
	public void addMessage(final ActorRef clientSender, final ActorRef clientReceiver) {
		final Pair<ActorRef, ActorRef> relationToUpdate = new Pair<>(clientSender, clientReceiver);
		if (!this.knowledge.containsKey(relationToUpdate)) {
			throw new IllegalArgumentException("Unknown clients relation");
		}
		this.knowledge.put(relationToUpdate, this.knowledge.get(relationToUpdate) + 1);
	}
	
	@Override
	public void deleteClient(final ActorRef client) {
		this.knowledge.entrySet().removeIf(entry -> entry.getKey().getFirst().equals(client) || entry.getKey().getSecond().equals(client));
	}
	
	@Override
	public int getNumberOfMessagesSent(final ActorRef clientSender, final ActorRef clientReceiver) {
		final Pair<ActorRef, ActorRef> relationToAnalyze = new Pair<>(clientSender, clientReceiver);
		if (!this.knowledge.containsKey(relationToAnalyze)) {
			throw new IllegalArgumentException("Unknown clients relation");
		}
		return this.knowledge.get(relationToAnalyze);
	}
	
	@Override
	public Set<Pair<ActorRef, ActorRef>> getClientsRelations() {
		return Collections.unmodifiableSet(this.knowledge.keySet());
	}

	@Override
	public void maximize(final ClientKnowledge otherKnowledge) {
		if (!this.getClientsRelations().equals(otherKnowledge.getClientsRelations())) {
			throw new IllegalArgumentException("The specified knowledges refers to different client interactions");
		}
		this.knowledge.replaceAll((relation, messages) -> Math.max(this.knowledge.get(relation),
				otherKnowledge.getNumberOfMessagesSent(relation.getFirst(), relation.getSecond())));
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
		return obj instanceof ClientKnowledgeImpl
				&& this.knowledge.equals(((ClientKnowledgeImpl)obj).knowledge);
	}
	
}
