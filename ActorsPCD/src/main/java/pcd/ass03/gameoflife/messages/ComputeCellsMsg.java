package pcd.ass03.gameoflife.messages;

import java.util.Iterator;

import akka.actor.ActorRef;

public class ComputeCellsMsg {
	
	private final Iterator<ActorRef> cellsToCompute;
	
	public ComputeCellsMsg(final Iterator<ActorRef> cellsToCompute) {
		this.cellsToCompute = cellsToCompute;
	}
	
	public Iterator<ActorRef> getCellsToCompute() {
		return this.cellsToCompute;
	}
	
}
