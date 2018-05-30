package pcd.ass03.chat;

import javafx.application.Application;
import javafx.stage.Stage;
import pcd.ass03.chat.utilities.view.ClientView;

public class MainClient extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		final ClientView view = new ClientView(primaryStage);
		view.show();
	}
	
	public static void main(final String[] args) {
		launch(args);
	}

}
