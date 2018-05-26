package pcd.ass03.gameoflife.view;

import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;

/**
 * This class represents an utility for the visualization of messages inside the view.
 *
 */
public class MessageUtils {
	
	/**
	 * This enumeration represents the possible types of exception
	 * that can happen and the header message that must be shown
	 * for each of them.
	 *
	 */
	public enum ExceptionType {
		
		FXML_EXCEPTION("FXML Loading Exception"),
		THREAD_EXCEPTION("Thread Exception"),
		IO_EXCEPTION("IO Exception"),
		STREAM_EXCEPTION("Stream Exception");
		
		private final String header;
		
		private ExceptionType(String header) {
			this.header = header;
		}
		
		public String getHeader() {
			return this.header;
		}
	}
	
	public static final String ERROR_TITLE = "Error";
	public static final String FXML_ERROR_MESSAGE = "could not be loaded";
	
	/**
	 * Shows a thread-safe (using Platform.runLater) dialog.
	 * 
	 * @param type
	 * 		type of the dialog (info, warning, error)
	 * @param title
	 * 		title of the window when dialog is shown (on the top)
	 * @param header
	 * 		header of the dialog (bottom of the title, near the icon)
	 * @param message
	 * 		message of the dialog
	 * @param additional
	 * 		additional info of the dialog, under the main message
	 * @param action
	 * 		action performed on button pressed
	 */
	public static void showMessage(final AlertType type, final String title, final String header,
			final String message, final String additional, final Consumer<ButtonType> action) {
		Platform.runLater(() ->{
			final Alert alert = new Alert(type);
			alert.setTitle(title);
			alert.setHeaderText(header);
			
			String msg = message;
			
			if (additional != null) {
				if (!additional.isEmpty()) {
					msg += "\n";
					msg += additional;
				}
			}
			
			alert.setContentText(msg);
			System.out.println(msg);
		
			if (action != null) {
				alert.showAndWait().ifPresent(action);
			} else {
				alert.showAndWait();
			}
		});
	}
	
	/**
	 * Shows an FXML exception.
	 * 
	 * @param component
	 * 		the component
	 * @param e
	 * 		the occurred exception
	 */
	public static void showFXMLException(final String component, final String exception) {
		showMessage(AlertType.ERROR, ERROR_TITLE, ExceptionType.FXML_EXCEPTION.getHeader(), component + " " + FXML_ERROR_MESSAGE, exception, new Consumer<ButtonType>() {		
			@Override
			public void accept(ButtonType t) {
				Platform.exit();
				System.exit(0);
			}
		});
	}
	
	/**
	 * Shows an exception.
	 * 
	 * @param exceptionType
	 * 		the type of the exception
	 * @param message
	 * 		the message to display
	 * @param e
	 * 		the occurred exception
	 */
	public static void showExcpetion(final ExceptionType exceptionType, final String message, final Exception e) {
		if (e != null) {
			showMessage(AlertType.ERROR, ERROR_TITLE, exceptionType.getHeader(), message, e.getMessage(), null);
		} else {
			showMessage(AlertType.ERROR, ERROR_TITLE, exceptionType.getHeader(), message, "", null);
		}	
	}
	
}
