package pcd.ass03.chat.view;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ViewDataManager {
	
	private static ViewDataManager singleton;

	private ObservableList<String> messages = FXCollections.observableArrayList();
	private ObservableList<String> clients = FXCollections.observableArrayList();
	
	
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
	public ObservableList<String> getMessagesProperty() {
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
	 * Add a message
	 * @param username
	 * 		Username of the client that's sending the message
	 * @param message
	 * 		Content of the message
	 */
	public void addMessage(final String username, final String message) {
		Platform.runLater(() -> {
			this.messages.add(username + ": " + message);
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
}
