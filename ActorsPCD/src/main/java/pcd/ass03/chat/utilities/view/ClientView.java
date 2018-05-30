package pcd.ass03.chat.utilities.view;

import java.io.File;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import pcd.ass03.chat.actors.ClientActor;
import pcd.ass03.gameoflife.view.MessageUtils;

public class ClientView extends BorderPane {
	
	private static final int WIDTH = 800;
	private static final int HEIGHT = 400;
	private static final String TITLE = "Chat with Actors - Giacomo Frisoni & Marcin Pabich";
	private static final String LOGIN = "LOGIN";
	private static final String LOGOUT = "LOGOUT";
	
	private final Stage stage;
	private final ObservableList<String> observableList;
	private ActorRef client;
	private ActorSystem system;
	private boolean isLoggedIn = false;
	 
	@FXML private TextField username, message;
	@FXML private Button login, send;
	@FXML private ListView<String> listview;
	
	public ClientView(final Stage stage) {
		this.stage = stage;
		this.observableList = FXCollections.observableArrayList();
		
		loadView();
		setDimensions();	
		setActionListeners();
		setOnLoginStatus();
	}
	
	public void show() {
		this.stage.show();
	}
	
	private void loadView() {
		final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ClientView.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		
        try {
            fxmlLoader.load();
            
            final Scene scene = new Scene(this);
    		
    		this.stage.setOnCloseRequest(e -> {
    			this.stage.close();
    	        Platform.exit();
    	        System.exit(0);
    		});

    		this.stage.setScene(scene);
    		this.stage.setTitle(TITLE);
    		this.stage.getIcons().addAll(
    				new Image(("file:res/icon_chat_16x16.png")),
    				new Image(("file:res/icon_chat_32x32.png")),
    				new Image(("file:res/icon_chat_64x64.png")));

        } catch (Exception exception) {
        	MessageUtils.showFXMLException(this.toString(), exception.getMessage());
        }
	}
	
	private void setDimensions() {
		this.setWidth(WIDTH);
    	this.setHeight(HEIGHT);
    	this.setMinWidth(WIDTH);
    	this.setMinHeight(HEIGHT);
    	this.stage.setMinWidth(WIDTH);
    	this.stage.setMinHeight(HEIGHT);
	}
	
	private void setOnLoginStatus() {
		this.username.setDisable(false);
		this.login.setText(LOGIN);
		this.message.setDisable(true);
		this.send.setDisable(true);
		isLoggedIn = false;
	}
	
	private void setOnActiveStatus() {
		this.username.setDisable(true);
		this.login.setText(LOGOUT);
		this.message.setDisable(false);
		this.send.setDisable(false);
		isLoggedIn = true;
	}
	
	private void setActionListeners() {
		this.login.setOnMouseClicked(e -> {
			if (isLoggedIn) {
				logout();
			} else {
				login();
			}
		});
		
		this.send.setOnMouseClicked(e -> {
			sendMessage();
		});
		
		this.message.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				sendMessage();
			}
		});
		
		this.listview.setItems(this.observableList);
	}

	private void logout() {
		if (destroyActor()) {
			setOnLoginStatus();
		}	
	}
	
	private void login() {
		if (createActor()) {
			setOnActiveStatus();
		}
	}
	
	private void sendMessage() {
		if (!this.message.getText().isEmpty()) {
			this.message.getStyleClass().remove("empty-message");
			this.client.tell(new ClientActor.SendingRequestMsg(this.message.getText()), ActorRef.noSender());
			this.message.clear();
		} else {
			this.message.getStyleClass().add("empty-message");
		}
	}
	
	private boolean createActor() {
		//Toggle the error class
		this.username.getStyleClass().remove("empty-message");
		
		//Check if it's OK
		if (!this.username.getText().isEmpty()) {
			//Generate system and actor
			final File file = new File("src/main/java/pcd/ass03/chat/client.conf");
			System.out.println("client.conf");
			System.out.println("Exists?" + file.exists());
			System.out.println("Absolute path?" + file.getAbsolutePath());
			System.out.println("Name?" + file.getName());
			final Config config = ConfigFactory.parseFile(file);
			this.system = ActorSystem.create("ClientSystem", config);
			this.client = system.actorOf(ClientActor.props(this.username.getText()), "client");
			
			return true;
			
		} else {
			this.username.getStyleClass().add("empty-message");
			return false;
		}	
	}
	
	private boolean destroyActor() {
		if (this.system != null) {
			if (this.client != null) {
				this.system.stop(this.client);
				return true;
			}
		}
		
		return false;
	}
}
