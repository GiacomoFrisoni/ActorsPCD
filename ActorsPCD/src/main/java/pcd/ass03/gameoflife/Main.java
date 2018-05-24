package pcd.ass03.gameoflife;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import pcd.ass03.gameoflife.actors.GridActor;
import pcd.ass03.gameoflife.actors.ViewActor;

public class Main {
	public static void main(String[] args) throws Exception {
		final ActorSystem system = ActorSystem.create("GameOfLifeSystem");
		final ActorRef viewActor = system.actorOf(ViewActor.props(4, 4), "view");
		final ActorRef gridActor = system.actorOf(GridActor.props(4, 4, viewActor), "grid");
		viewActor.tell("play", ActorRef.noSender());
		gridActor.tell("play", ActorRef.noSender());
	}
}
