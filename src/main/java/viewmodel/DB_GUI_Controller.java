package viewmodel;

import dao.DbConnectivityClass;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Person;
import service.MyLogger;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

public class DB_GUI_Controller implements Initializable {

    @FXML
    public MenuItem editItem, deleteItem, ClearItem, CopyItem;
    @FXML
    public Button clearBtn, addBtn, deleteBtn, editBtn;
    @FXML
    TextField first_name, last_name, department, email, imageURL;
    @FXML
    private ComboBox<Major> major;
    @FXML
    ImageView img_view;
    @FXML
    MenuBar menuBar;
    @FXML
    private TableView<Person> tv;
    @FXML
    private TableColumn<Person, Integer> tv_id;
    @FXML
    private TableColumn<Person, String> tv_fn, tv_ln, tv_department, tv_major, tv_email;
    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    private final ObservableList<Person> data = cnUtil.getData();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
            tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
            tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
            tv_department.setCellValueFactory(new PropertyValueFactory<>("department"));
            tv_major.setCellValueFactory(new PropertyValueFactory<>("major"));
            tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));
            tv.setItems(data);

            // Disable buttons initially
            editBtn.setDisable(true);
            deleteBtn.setDisable(true);
            addBtn.setDisable(true);
            editItem.setDisable(true);
            deleteItem.setDisable(true);
            CopyItem.setDisable(true);

            // Enable when a row is selected
            tv.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                boolean noSelection = newSelection == null;
                editBtn.setDisable(noSelection);
                deleteBtn.setDisable(noSelection);
                addBtn.setDisable(noSelection);
                editItem.setDisable(noSelection);
                deleteItem.setDisable(noSelection);
                CopyItem.setDisable(noSelection);
            });

            first_name.textProperty().addListener((obs, oldVal, newVal) -> validateFormFields());
            last_name.textProperty().addListener((obs, oldVal, newVal) -> validateFormFields());
            department.textProperty().addListener((obs, oldVal, newVal) -> validateFormFields());
            major.setItems(FXCollections.observableArrayList(Major.values()));
            major.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> validateFormFields());
            email.textProperty().addListener((obs, oldVal, newVal) -> validateFormFields());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void validateFormFields() {
        boolean validFirst = validateName(first_name);
        boolean validLast = validateName(last_name);
        boolean validDept = validateDepartment(department);
        boolean validMajor = major.getValue() != null;
        boolean validEmail = validateEmail(email);

        boolean allValid = validFirst && validLast && validDept && validMajor && validEmail;
        addBtn.setDisable(!allValid);
    }

    private boolean validateName(TextField tf) {
        String regex = "^[A-Z][a-z]+(?:\\s[A-Z][a-z]+)*$"; // e.g., John Doe
        boolean valid = tf.getText().matches(regex);
        tf.setStyle(valid ? null : "-fx-border-color: red; -fx-border-width: 2px;");
        return valid;
    }

    private boolean validateDepartment(TextField tf) {
        String regex = "^[A-Za-z]{2,}(\\s[A-Za-z]{2,})*$"; // General words
        boolean valid = tf.getText().matches(regex);
        tf.setStyle(valid ? null : "-fx-border-color: red; -fx-border-width: 2px;");
        return valid;
    }

    private boolean validateEmail(TextField tf) {
        String regex = "^[\\w.-]+@[\\w.-]+\\.\\w{2,}$";
        boolean valid = tf.getText().matches(regex);
        if (!valid) {
            tf.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else {
            tf.setStyle(null);
        }
        return valid;
    }

    @FXML
    protected void addNewRecord() {
        Major selectedMajor = major.getValue();
            Person p = new Person(first_name.getText(), last_name.getText(), department.getText(),
                    selectedMajor.name(), email.getText(), imageURL.getText());
            cnUtil.insertUser(p);
            cnUtil.retrieveId(p);
            p.setId(cnUtil.retrieveId(p));
            data.add(p);
            clearForm();

    }

    @FXML
    protected void clearForm() {
        first_name.setText("");
        last_name.setText("");
        department.setText("");
        major.setValue(null);
        email.setText("");
        imageURL.setText("");
        first_name.setStyle(null);
        last_name.setStyle(null);
        department.setStyle(null);
        major.setStyle(null);
        email.setStyle(null);
    }

    @FXML
    protected void logOut(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").getFile());
            Stage window = (Stage) menuBar.getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void closeApplication() {
        System.exit(0);
    }

    @FXML
    protected void displayAbout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/about.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 500);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void editRecord() {
        Major selectedMajor = major.getValue();
        Person p = tv.getSelectionModel().getSelectedItem();
        int index = data.indexOf(p);
        Person p2 = new Person(index + 1, first_name.getText(), last_name.getText(), department.getText(),
                selectedMajor.name(), email.getText(),  imageURL.getText());
        cnUtil.editUser(p.getId(), p2);
        data.remove(p);
        data.add(index, p2);
        tv.getSelectionModel().select(index);
    }

    @FXML
    protected void deleteRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();
        int index = data.indexOf(p);
        cnUtil.deleteRecord(p);
        data.remove(index);
        tv.getSelectionModel().select(index);
    }

    @FXML
    protected void showImage() {
        File file = (new FileChooser()).showOpenDialog(img_view.getScene().getWindow());
        if (file != null) {
            img_view.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    protected void addRecord() {
        showSomeone();
    }

    @FXML
    protected void selectedItemTV(MouseEvent mouseEvent) {
        Person p = tv.getSelectionModel().getSelectedItem();
        first_name.setText(p.getFirstName());
        last_name.setText(p.getLastName());
        department.setText(p.getDepartment());
        major.setValue(Major.valueOf(p.getMajor()));
        email.setText(p.getEmail());
        imageURL.setText(p.getImageURL());
    }

    public void lightTheme(ActionEvent actionEvent) {
        try {
            Scene scene = menuBar.getScene();
            Stage stage = (Stage) scene.getWindow();
            stage.getScene().getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
            System.out.println("light " + scene.getStylesheets());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void darkTheme(ActionEvent actionEvent) {
        try {
            Stage stage = (Stage) menuBar.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/darkTheme.css").toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showSomeone() {
        Dialog<Results> dialog = new Dialog<>();
        dialog.setTitle("New User");
        dialog.setHeaderText("Please specify…");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField textField1 = new TextField("Name");
        TextField textField2 = new TextField("Last Name");
        TextField textField3 = new TextField("Email ");
        ObservableList<Major> options =
                FXCollections.observableArrayList(Major.values());
        ComboBox<Major> comboBox = new ComboBox<>(options);
        comboBox.getSelectionModel().selectFirst();
        dialogPane.setContent(new VBox(8, textField1, textField2,textField3, comboBox));
        Platform.runLater(textField1::requestFocus);
        dialog.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return new Results(textField1.getText(),
                        textField2.getText(), comboBox.getValue());
            }
            return null;
        });
        Optional<Results> optionalResult = dialog.showAndWait();
        optionalResult.ifPresent((Results results) -> {
            MyLogger.makeLog(
                    results.fname + " " + results.lname + " " + results.major);
        });
    }

    private static enum Major {Business, CSC, CPIS, English}

    private static class Results {

        String fname;
        String lname;
        Major major;

        public Results(String name, String date, Major venue) {
            this.fname = name;
            this.lname = date;
            this.major = venue;
        }
    }

}