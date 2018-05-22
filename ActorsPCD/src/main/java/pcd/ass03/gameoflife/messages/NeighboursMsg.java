package pcd.ass03.gameoflife.messages;

import java.util.HashSet;
import java.util.Set;

import akka.actor.ActorRef;

public final class NeighboursMsg {

	private final Set<ActorRef> neighbours;
	
	public NeighboursMsg(final Set<ActorRef> neighbours) {
		this.neighbours = new HashSet<>(neighbours);
	}
	
	public Set<ActorRef> getNeighbours() {
		return this.neighbours;
	}
	
}
