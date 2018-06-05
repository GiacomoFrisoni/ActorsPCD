package pcd.ass03.chat;

import java.io.File;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import javafx.application.Application;
import javafx.stage.Stage;
import pcd.ass03.chat.view.ClientView;

public class MainClient extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		//Generate system
		final File file = new File("src/main/java/pcd/ass03/chat/client.conf");
		final Config config = ConfigFactory.parseFile(file);
		final ActorSystem system = ActorSystem.create("ClientSystem", config);
		
		//Create view
		final ClientView view = new ClientView(primaryStage, system);
		view.show();
	}
	
	public static void main(final String[] args) {
		launch(args);
	}

}
