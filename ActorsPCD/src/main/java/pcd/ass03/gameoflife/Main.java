package pcd.ass03.gameoflife;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import pcd.ass03.gameoflife.actors.GridActor;

public class Main {
	public static void main(String[] args) throws Exception {
		final ActorSystem system = ActorSystem.create("GameOfLifeSystem");
		final ActorRef gridActor = system.actorOf(GridActor.props(4, 4), "Grid");
		gridActor.tell("play", ActorRef.noSender());
	}
}
