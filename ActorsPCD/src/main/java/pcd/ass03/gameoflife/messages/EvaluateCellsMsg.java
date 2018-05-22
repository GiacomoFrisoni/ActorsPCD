package pcd.ass03.gameoflife.messages;

import java.util.Iterator;

import akka.actor.ActorRef;

public final class EvaluateCellsMsg {
	
	private final Iterator<ActorRef> cellsToEvaluate;
	
	public EvaluateCellsMsg(final Iterator<ActorRef> cellsToEvaluate) {
		this.cellsToEvaluate = cellsToEvaluate;
	}
	
	public Iterator<ActorRef> getCellsToEvaluate() {
		return this.cellsToEvaluate;
	}
	
}
