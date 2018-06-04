package pcd.ass03.chat.view;

import java.io.File;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import pcd.ass03.chat.actors.ClientActor;
import pcd.ass03.chat.messages.client.BroadcastSendingRequestMsg;
import pcd.ass03.chat.messages.client.ChatMsg;
import pcd.ass03.gameoflife.view.MessageUtils;

public class ClientView extends BorderPane {
	
	private static final int WIDTH = 800;
	private static final int HEIGHT = 410;
	private static final String TITLE = "Chat with Actors - Giacomo Frisoni & Marcin Pabich";
	private static final String LOGIN = "LOGIN";
	private static final String LOADING = "LOADING";
	private static final String LOGOUT = "LOGOUT";
	
	private final Stage stage;
	private ActorRef client;
	private ActorSystem system;
	 
	@FXML private TextField username, message;
	@FXML private Button login, send;
	@FXML private ListView<TextFlow> messages;
	@FXML private ListView<String> clients;
	@FXML private ProgressIndicator progress;
	
	/**
	 * Constructor for the view
	 * @param stage
	 * 		primaryStage passed from the start main method
	 */
	public ClientView(final Stage stage) {
		this.stage = stage;
		
		this.loadView();
		this.setDimensions();	
		this.setActionListeners();
		this.setStatusToStart();
	}
	
	/**
	 * Method that shows the view on the screen
	 */
	public void show() {
		this.stage.show();
	}
	
	/**
	 * Load the .fxml file associated with view
	 */
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
	
	/**
	 * Set the correct dimension of the view
	 */
	private void setDimensions() {
		this.setWidth(WIDTH);
    	this.setHeight(HEIGHT);
    	this.setMinWidth(WIDTH);
    	this.setMinHeight(HEIGHT);
    	this.stage.setMinWidth(WIDTH);
    	this.stage.setMinHeight(HEIGHT);
	}
	
	/**
	 * Set the view to pre-login status
	 */
	private void setStatusToStart() {
		Platform.runLater(() -> {
			this.username.setDisable(false);
			this.enableLoading(false);
			this.login.setText(LOGIN);
			this.message.setDisable(true);
			this.send.setDisable(true);
		});		
	}
	
	/**
	 * Disable all controls to check if it can log in
	 */
	private void setStatusToLogginIn() {
		Platform.runLater(() -> {
			this.username.setDisable(true);
			this.enableLoading(true);
			this.message.setDisable(true);
			this.send.setDisable(true);
		});
	}
	
	/**
	 * Login was succesfull, enable the chat
	 */
	private void setStatusToActive() {
		Platform.runLater(() -> {
			this.username.setDisable(true);
			this.enableLoading(false);
			this.login.setText(LOGOUT);
			this.message.setDisable(false);
			this.send.setDisable(false);
		});
	}
	
	private void enableLoading(final boolean enable) {
		this.login.setDisable(enable);
		this.login.setText(LOADING);
		this.progress.setVisible(enable);
		this.progress.setManaged(enable);
	}
	
	/**
	 * Disable all controls to check if it can log out
	 */
	private void setStatusToLogginOut() {
		this.setStatusToLogginIn();
	}
	
	private void checkLoginLogout() {	
		this.enableLoading(true);
		new Thread(() -> {
			if (!ViewDataManager.getInstance().isLoggedInProperty().get()) {
				if (this.createActor()) {
					this.setStatusToLogginIn();
				}
				
			} else {
				if (this.destroyActor()) {
					this.setStatusToLogginOut();
				}			
			}
		}).start();
	}
	
	
	/**
	 * Set all the action listners and bindings for the view
	 */
	private void setActionListeners() {
		
		//Action for login button
		this.login.setOnMouseClicked(e -> {
			checkLoginLogout();
		});
		
		//Action when pressing ENTER in username
		this.username.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				checkLoginLogout();
			}
		});
		
		
		//Action for send button
		this.send.setOnMouseClicked(e -> {
			sendMessage();
		});
		
		//Action when pressing ENTER in messagebox
		this.message.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				sendMessage();
			}
		});
		
		//Bindings
		this.messages.setItems(ViewDataManager.getInstance().getMessagesProperty());
		this.clients.setItems(ViewDataManager.getInstance().getClientsProperty());
		
		//Listener on binding
		ViewDataManager.getInstance().isLoggedInProperty().addListener(listener -> {
			if (ViewDataManager.getInstance().isLoggedInProperty().get()) {
				//User is logged in, activate chat
				this.setStatusToActive();
			} else {
				//User is not logged in, activate login
				this.setStatusToStart();
			}		
		});
	}

	
	/**
	 * Method that send a message to client
	 */
	private void sendMessage() {
		if (!this.message.getText().isEmpty()) {
			this.message.getStyleClass().remove("empty-message");
			this.client.tell(new BroadcastSendingRequestMsg(new ChatMsg(this.message.getText())), ActorRef.noSender());
			this.message.clear();
		} else {
			this.message.getStyleClass().add("empty-message");
		}
	}
	
	/**
	 * Permit to create a new client actor
	 * @return
	 * 		TRUE if creation was successful
	 */
	private boolean createActor() {
		//Toggle the error class
		this.username.getStyleClass().remove("empty-message");
		
		//Check if it's OK
		if (!this.username.getText().isEmpty()) {
			//Generate system and actor
			final File file = new File("src/main/java/pcd/ass03/chat/client.conf");
			final Config config = ConfigFactory.parseFile(file);
			this.system = ActorSystem.create("ClientSystem", config);
			this.client = system.actorOf(ClientActor.props(this.username.getText()), "client");
			
			return true;
		} else {
			this.username.getStyleClass().add("empty-message");
			return false;
		}
	}
	
	/**
	 * Permit to destroy the client actor
	 * @return
	 * 		TRUE if destroy was successful
	 */
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
