package viewmodel;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import service.UserSession;
import java.util.prefs.Preferences;

public class SignUpController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ChoiceBox<String> privilegesChoiceBox;

    @FXML
    public void initialize() {
        // Create privileges choice box options
        privilegesChoiceBox.getItems().addAll("STUDENT", "ADMIN", "GUEST");
        privilegesChoiceBox.setValue("STUDENT"); // Default value
    }

    public void createNewAccount(ActionEvent actionEvent) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String privileges = privilegesChoiceBox.getValue();

        if (username.isEmpty() || password.isEmpty() || privileges == null) {
            showAlert(Alert.AlertType.ERROR, "Missing Information", "Please fill in all fields.");
            return;
        }

        // Save user session and store in Preferences
        UserSession.getInstance(username, password, privileges);

        // Optionally, you can manually save to Preferences here again if needed:
        Preferences userPreferences = Preferences.userRoot();
        userPreferences.put("USERNAME", username);
        userPreferences.put("PASSWORD", password); // In real apps, NEVER store plaintext passwords!
        userPreferences.put("PRIVILEGES", privileges);

        showAlert(Alert.AlertType.INFORMATION, "Account Created", "Account successfully created for user: " + username);

        // After signup, return to login screen
        goBack(actionEvent);
    }

    public void goBack(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
