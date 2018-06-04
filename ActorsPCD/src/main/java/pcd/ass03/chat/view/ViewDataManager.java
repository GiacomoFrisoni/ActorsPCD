package pcd.ass03.chat.view;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ViewDataManager {
	
	//Static types of message
	private final static String LOGIN_MESSAGE = "has joined to the chat!";
	private final static String LOGOUT_MESSAGE = "has left the chat!";
	private final static String MUTEX_LOCK_MESSAGE = "got the mutex!";
	private final static String MUTEX_UNLOCK_MESSAGE = "released the mutex!";
	private final static String SEPARATOR = "----";
	
	//Enum to check what type of message I'm going to show
	public enum MessageType {
		LOGIN(LOGIN_MESSAGE), LOGOUT(LOGOUT_MESSAGE), MUTEX_LOCK(MUTEX_LOCK_MESSAGE), MUTEX_UNLOCK(MUTEX_UNLOCK_MESSAGE);
			
		private final String message;
		
		private MessageType(final String message) {
			this.message = message;
		}
		
		public String getMessage() {
			return this.message;
		}
	}

	
	private static ViewDataManager singleton;

	private ObservableList<TextFlow> messages = FXCollections.observableArrayList();
	private ObservableList<String> clients = FXCollections.observableArrayList();
	
	private BooleanProperty isLoggedIn = new SimpleBooleanProperty(false);
	
	
	private ViewDataManager() { }
	
	
	/**
	 * This method returns the DataManager.
	 * If the DataManager is null it creates a new one on the first call (thread-safe).
	 * @return the data manager.
	 */
	public static ViewDataManager getInstance() {
		if (singleton == null ) {
			singleton = new ViewDataManager();
		}
		
		return singleton;
	}
	
	
	/**
	 * Get the property of list of all messages, ready to bind
	 * @return
	 * 		Property representing the list of all messages
	 */
	public ObservableList<TextFlow> getMessagesProperty() {
		return this.messages;
	}
	
	/**
	 * Get the property of all clients connected, ready to bind
	 * @return
	 * 		Property representing the list of all messages
	 */
	public ObservableList<String> getClientsProperty() {
		return this.clients;
	}
	
	/**
	 * Tell if user is already logged in
	 * @return
	 * 		Property representing if user is already logged in
	 */
	public BooleanProperty isLoggedInProperty () {
		return this.isLoggedIn;
	}
	
	
	/**
	 * Add a message
	 * @param username
	 * 		Username of the client that's sending the message
	 * @param message
	 * 		Content of the message
	 */
	public void addMessage(final String username, final String message) {	
		final Text usernameText = createText(username + ": ", true, false);
		final Text messageText = createText(message, false, false);
		final TextFlow flow = createTextFlow(usernameText, messageText);
		
		Platform.runLater(() -> {
			this.messages.add(flow);
		});	
	}
	
	/**
	 * Add an info message
	 * @param username
	 * 		Username that is doing the action
	 * @param messageType
	 * 		Type of the message: login, logout, mutex_lock, mutext_unlock. 
	 */
	public void addInfoMessage(final String username, final MessageType messageType) {
		final Text separator = createText(SEPARATOR, false, true);
		final Text usernameText = createText(username, true, true);
		final Text infoText = createText(messageType.getMessage() + " ", false, true);
		final TextFlow flow = createTextFlow(separator, usernameText, infoText, separator);
		
		Platform.runLater(() -> {
			this.messages.add(flow);
		});	
	}
	
	/**
	 * Add a client
	 * @param client
	 * 		Username of the client
	 */
	public void addClient(final String client) {
		Platform.runLater(() -> {
			this.clients.add(client);
		});	
	}
	
	/**
	 * Remove a client
	 * @param client
	 * 		Client to remove 
	 */
	public void removeClient(final String client) {
		Platform.runLater(() -> {
			this.clients.remove(client);
		});	
	}
	
	/**
	 * Set the client as logged in or out
	 * @param value
	 * 		TRUE: logged in, FALSE: logged out
	 */
	public void setLogged(final boolean value) {
		Platform.runLater(() -> {
			this.isLoggedIn.setValue(value);
		});	
	}
	
	
	
	/*
	 * Create a flow of stylized texts
	 * @param texts
	 * 		Text to put toghether
	 * @return
	 * 		A TextFlow object composed of all texts
	 */
	private TextFlow createTextFlow(final Text... texts) {
		return new TextFlow(texts);
	}
	
	/*
	 * Create a Text from a string, stylizing it properly
	 * @param str
	 * 		String to convert to Text
	 * @param isBold
	 * 		TRUE: string will be also bold, else normal (or only ITALIC)
	 * @param isItalic
	 * 		TRUE: string will be also italic, else normal (or only BOLD)
	 * @return
	 * 		Text object
	 */
	private Text createText(final String str, final boolean isBold, final boolean isItalic) {
		final Text text = new Text(str);
		if (isBold) text.setStyle("-fx-font-weight: bold");
		if (isItalic) text.setStyle("-fx-font-style: italic;");
		return text;
	}
}
