package pcd.ass03.chat.utilities;

import java.util.Set;

import akka.actor.ActorRef;

public interface ClientKnowledge {
	
	void addNewClient(ActorRef client, Set<ActorRef> clientsToRelate);
	
	void addNewClients(Set<ActorRef> clients, Set<ActorRef> clientsToRelate);
	
	void deleteClient(ActorRef client);
	
	void addMessage(ActorRef clientSender, ActorRef clientReceiver);
	
	int getNumberOfMessagesSent(ActorRef clientSender, ActorRef clientReceiver);
	
	Set<Pair<ActorRef, ActorRef>> getClientsRelations();
	
	void maximize(ClientKnowledge otherKnowledge);
	
}
