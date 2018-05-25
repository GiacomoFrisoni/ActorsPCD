package pcd.ass03.gameoflife.view;

import java.io.IOException;
import java.util.Optional;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

public class ViewUtils {
	
	/**
	 * Load the FXML file for a node. If loading is unsuccessful program will exit automatically, showing a message error
	 * @param node
	 * 		node that will be the root and controller od the FXML
	 * @param fileName
	 * 		name of the .fxml file to load
	 */
	public static Optional<String> loadFXML(final Node node, final String fileName) {
		final FXMLLoader loader = new FXMLLoader(node.getClass().getResource(fileName));
		loader.setRoot(node);
		loader.setController(node);
		
		try {
			loader.load();
			return Optional.empty();
		} catch (IOException e) {
			return Optional.of(e.getMessage());
		} 
	}
}
