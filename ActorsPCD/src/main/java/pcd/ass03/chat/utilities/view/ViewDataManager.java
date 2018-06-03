package pcd.ass03.chat.utilities.view;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ViewDataManager {
	
	private static ViewDataManager singleton;

	private ObservableList<TextFlow> messages = FXCollections.observableArrayList();
	private ObservableList<String> clients = FXCollections.observableArrayList();
	
	private BooleanProperty isLoggedIn = new SimpleBooleanProperty(false);
	
	
	private ViewDataManager() {

	}
	
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
		Platform.runLater(() -> {
			final TextFlow flow = new TextFlow();
			final Text usernameText = new Text(username + ": ");
			usernameText.setStyle("-fx-font-weight: bold");
			final Text messageText = new Text(message);
			
			flow.getChildren().addAll(usernameText, messageText);
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
}
