package pcd.ass03.gameoflife.view;

import java.awt.Point;
import java.util.Map;

import akka.actor.ActorRef;

public interface View {
	void show();
	void close();
	
	void setGridActor(ActorRef gridActor);
	void setViewActor(ActorRef  viewActor);
	
	void drawCells(Map<Point, Boolean> cells);
	void reset();
}
