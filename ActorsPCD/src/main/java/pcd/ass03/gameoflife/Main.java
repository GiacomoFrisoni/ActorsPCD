package pcd.ass03.gameoflife;

import java.io.File;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import javafx.application.Application;
import javafx.stage.Stage;
import pcd.ass03.gameoflife.actors.GridActor;
import pcd.ass03.gameoflife.actors.ViewActor;
import pcd.ass03.gameoflife.view.View;
import pcd.ass03.gameoflife.view.ViewImpl;

public class Main extends Application {
	
	@Override
	public void start(final Stage primaryStage) {
		final View view = new ViewImpl(primaryStage);
		final Config config = ConfigFactory.parseFile(new File("src/main/java/pcd/ass03/gameoflife/application.conf"));
		final ActorSystem system = ActorSystem.create("GameOfLifeSystem", config);
		final ActorRef viewActor = system.actorOf(ViewActor.props(view), "view");
		final ActorRef gridActor = system.actorOf(GridActor.props(), "grid");
		
		view.setGridActor(gridActor);
		view.setViewActor(viewActor);
		view.show();
		
		/*gridActor.tell(new GridActor.InitGridMsg(5, 5, viewActor), ActorRef.noSender());
		viewActor.tell(new ViewActor.StartVisualizationMsg(), ActorRef.noSender());
		gridActor.tell(new GridActor.StartGameMsg(), ActorRef.noSender());*/
	}
	
	public static void main(final String[] args) {
		launch(args);
	}
}
