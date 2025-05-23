package viewmodel;

import dao.DbConnectivityClass;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.*;
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
import javafx.util.Duration;
import model.Person;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import service.MyLogger;
import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class DB_GUI_Controller implements Initializable {

    @FXML
    public MenuItem editItem, deleteItem, ClearItem, CopyItem;
    @FXML
    private Label statusLabel;
    @FXML
    public MenuItem importCSVItem, exportCSVItem;
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
    @FXML
    private TextField searchField;

    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    private final ObservableList<Person> data = cnUtil.getData();
    private PauseTransition inactivityTimer;

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


            // Setup search
            FilteredList<Person> filteredData = new FilteredList<>(data, p -> true);

            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(person -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    if (person.getFirstName().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else if (person.getLastName().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else if (person.getDepartment().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else if (person.getMajor().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else if (person.getEmail().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    }
                    return false;
                });
            });

            SortedList<Person> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(tv.comparatorProperty());
            tv.setItems(sortedData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Waits till scene is created and starts the inactivity listeners
        Platform.runLater(() -> {
            inactivityListeners(menuBar.getScene());
        });
        setupInactivityTimer();
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
            setStatus("New record added for " + p.getFirstName() + " " + p.getLastName());
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

            if (searchField != null) {
                searchField.clear();
            }
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
        setStatus("Record updated for ID: " + p.getId());
    }

    @FXML
    protected void deleteRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();
        int index = data.indexOf(p);
        cnUtil.deleteRecord(p);
        data.remove(index);
        tv.getSelectionModel().select(index);
        setStatus("Record deleted for ID: " + p.getId());
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

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    @FXML
    protected void importCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        File file = fileChooser.showOpenDialog(menuBar.getScene().getWindow());

        if (file != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] fields = line.split(",");
                    if (fields.length >= 6) {
                        Person p = new Person(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]);
                        cnUtil.insertUser(p);
                        p.setId(cnUtil.retrieveId(p));
                        data.add(p);
                    }
                }
                setStatus("CSV file imported successfully.");
            } catch (IOException e) {
                e.printStackTrace();
                setStatus("Error importing CSV file.");
            }
        }
    }

    @FXML
    protected void exportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to CSV");
        fileChooser.setInitialFileName("users.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(menuBar.getScene().getWindow());

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Person p : data) {
                    String row = String.join(",", p.getFirstName(), p.getLastName(), p.getDepartment(),
                            p.getMajor(), p.getEmail(), p.getImageURL());
                    writer.write(row);
                    writer.newLine();
                }
                setStatus("CSV file exported successfully.");
            } catch (IOException e) {
                e.printStackTrace();
                setStatus("Error exporting CSV file.");
            }
        }
    }
    private void setupInactivityTimer() {
        inactivityTimer = new PauseTransition(Duration.seconds(300)); // 5 minute timer
        inactivityTimer.setOnFinished(event -> logoutDueToInactivity());
    }

    public void inactivityListeners(Scene scene) {
        scene.setOnMouseMoved(event -> resetInactivityTimer());
        scene.setOnMouseClicked(event -> resetInactivityTimer());
        scene.setOnKeyPressed(event -> resetInactivityTimer());
        scene.setOnKeyTyped(event -> resetInactivityTimer());
        inactivityTimer.playFromStart();
    }

    private void resetInactivityTimer() {
        inactivityTimer.playFromStart();
    }

    private void logoutDueToInactivity() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").getFile());
            Stage window = (Stage) menuBar.getScene().getWindow();
            window.setScene(scene);
            window.show();

            if (searchField != null) {
                searchField.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGenerateReport(ActionEvent event) {
        generatePdfReport();  // <- This is your method to create the PDF and alert the user
    }

    private Map<String, Integer> countStudentsByMajor(List<Person> students) {
        Map<String, Integer> majorCounts = new HashMap<>();
        for (Person s : students) {
            String major = s.getMajor(); // adjust getter if different
            majorCounts.put(major, majorCounts.getOrDefault(major, 0) + 1);
        }
        return majorCounts;
    }

    private void generatePdfReport() {
        try {
            List<Person> studentList = tv.getItems(); // Or your backing list
            Map<String, Integer> majorCounts = countStudentsByMajor(studentList);

            PDDocument document = new PDDocument();
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            contentStream.newLineAtOffset(100, 700); // X, Y Position
            contentStream.showText("Student Count by Major:");
            contentStream.newLine(); // Move down a bit

            // Set a smaller font for the list
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(0, -20); // Move down again

            for (Map.Entry<String, Integer> entry : majorCounts.entrySet()) {
                String line = entry.getKey() + ": " + entry.getValue();
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -20); // Move down for the next line
            }

            contentStream.endText();
            contentStream.close();

            // Save to Desktop
            String desktopPath = System.getProperty("user.home") + "/Desktop/Student_Major_Report.pdf";
            document.save(desktopPath);
            document.close();

            showAlert("Report generated", "PDF report saved as Student_Major_Report.pdf");

        } catch (IOException e) {
            showAlert("Error", "Failed to generate report: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // No header
        alert.setContentText(message);
        alert.showAndWait();
    }
}