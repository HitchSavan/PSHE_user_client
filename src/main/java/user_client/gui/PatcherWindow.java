package user_client.gui;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JFileChooser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import patcher.utils.files_utils.FileVisitor;
import patcher.utils.data_utils.IntegrityChecker;
import patcher.utils.files_utils.Directories;
import patcher.utils.patching_utils.RunCourgette;
import patcher.remote_api.endpoints.FilesEndpoint;
import patcher.remote_api.endpoints.PatchesEndpoint;
import patcher.remote_api.endpoints.VersionsEndpoint;
import patcher.remote_api.entities.VersionEntity;
import patcher.utils.remote_utils.Connector;
import user_client.utils.CourgetteHandler;
import user_client.utils.AlertWindow;
import user_client.utils.HistoryTableItem;

public class PatcherWindow extends Application {

    String windowName = "PSHE patcher";
    int defaultWindowWidth = 600;
    int defaultWindowHeight = 260;

    Stage primaryStage;
    Scene primaryScene;
    AuthWindow authWindow;

    TabPane fileTabs;
    TabPane remoteTabs;

    Tab applyTab;
    Tab genTab;
    Tab remoteApplyTab;
    Tab historyTab;
    Tab remoteGenTab;

    Button modeSwitchButton;
    boolean isFileMode;

    HashMap<TabPane, HashMap<String, Integer>> tabsNames = new HashMap<>();
    VBox applyPatchTabContent;
    VBox genPatchTabContent;
    VBox historyTabContent;
    TextField checkoutProjectPathField;
    Button chooseCheckoutProjectButton;
    
    VersionEntity checkoutVersion = null;
    VersionEntity rootVersion = null;
    Button checkoutButton;

    TextField patchPathField;
    Button choosePatchButton;

    TextField projectPathField;
    Button chooseProjectButton;

    TextField genPatchPathField;
    Button chooseGenPatchButton;

    TextField oldProjectPathField;
    Button chooseOldProjectButton;

    TextField newProjectPathField;
    Button chooseNewProjectButton;

    TextField remoteProjectPathField;
    Button chooseRemoteProjectButton;

    TextField remoteOldProjectPathField;
    Button chooseRemoteOldProjectButton;

    TextField remoteNewProjectPathField;
    Button chooseRemoteNewProjectButton;

    JFileChooser fileChooser;

    CheckBox rememberApplyPathsCheckbox;
    CheckBox replaceFilesCheckbox;
    CheckBox rememberGenPathsCheckbox;
    CheckBox remoteRememberApplyPathsCheckbox;
    CheckBox remoteReplaceFilesCheckbox;
    CheckBox remoteRememberGenPathsCheckbox;
    CheckBox historyRememberPathsCheckbox;
    CheckBox historyReplaceFilesCheckbox;

    Button applyPatchButton;
    Button genPatchButton;
    Button remoteApplyPatchButton;
    Button remoteGenPatchButton;

    Button genPatchLoginButton;
    Button applyPatchLoginButton;
    Button historyLoginButton;

    Path projectPath;
    Path patchPath;
    Path oldProjectPath;
    Path newProjectPath;
    Path patchFolderPath;

    Path remoteProjectPath;
    Path remoteOldProjectPath;
    Path remoteNewProjectPath;

    Label activeCourgettesApplyAmount;
    Label activeCourgettesGenAmount;
    Label remoteActiveCourgettesApplyAmount;
    Label remoteActiveCourgettesGenAmount;
    Label historyActiveCourgettesAmount;
    Label remoteGenStatus;
    Label remoteApplyStatus;
    Label historyCheckoutStatus;
    
    public static void runApp(String[] args) {
        System.setProperty("javafx.preloader", CustomPreloader.class.getCanonicalName());
        Application.launch(PatcherWindow.class, args);
    }

    @Override
    public void init() throws Exception {
        super.init();
        RunCourgette.unpackCourgette();
        
        Platform.runLater(() -> {
            authWindow = new AuthWindow();
            setupFileUi();
            setupEvents();
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        setupMainWindowUi();
    }

    private void setupFileUi() {
        setupApplyTabUi();
        setupGenTabUi();
        
        fileTabs = new TabPane();
        addTab(fileTabs, "Patching", applyTab);
        addTab(fileTabs, "Generate", genTab);
        
        remoteApplyTab = new Tab();
        applyPatchLoginButton = new Button();
        setupLoginUi(remoteApplyTab, applyPatchLoginButton);
        historyTab = new Tab();
        historyLoginButton = new Button();
        setupLoginUi(historyTab, historyLoginButton);
        genTab = new Tab();
        genPatchLoginButton = new Button();
        setupLoginUi(genTab, genPatchLoginButton);
        
        remoteTabs = new TabPane();
        addTab(remoteTabs, "Patching", remoteApplyTab);
        addTab(remoteTabs, "History", historyTab);
        addTab(remoteTabs, "Generate", genTab);
    }

    private void setupRemoteUi() {
        setupRemoteApplyTabUi();
        setupHistoryTabUi();
        setupRemoteGenTabUi();
    }

    private void setupMainWindowUi() {
        this.primaryStage.setMinWidth(300);
        this.primaryStage.setMinHeight(defaultWindowHeight);

        this.primaryStage.setWidth(defaultWindowWidth);
        this.primaryStage.setHeight(defaultWindowHeight);

        VBox mainPane = new VBox();
        VBox.setVgrow(fileTabs, Priority.ALWAYS);
        VBox.setVgrow(remoteTabs, Priority.ALWAYS);

        modeSwitchButton = new Button("Change to remote mode");
        mainPane.setPadding(new Insets(0, 0, 5, 0));
        mainPane.setAlignment(Pos.CENTER);

        if (!authWindow.config.has("defaultFilemode")) {
            authWindow.config.put("defaultFilemode", true);
        }

        isFileMode = authWindow.config.getBoolean("defaultFilemode");
        
        mainPane.getChildren().addAll(fileTabs, modeSwitchButton);

        switchMode(mainPane, !isFileMode);

        this.primaryScene = new Scene(mainPane);
        this.primaryStage.setScene(primaryScene);

        modeSwitchButton.setOnAction(e -> {
            switchMode(mainPane, isFileMode);
        });
        
        this.primaryStage.setOnCloseRequest(e -> {
            authWindow.saveConfig();
            Directories.deleteDirectory("tmp");
            System.exit(0);
        });

        this.primaryStage.show();
    }

    private void addTab(TabPane tabbedPane, String tabName, Tab newTab) {
        newTab.setText(tabName);
        newTab.setClosable(false);
        tabbedPane.getTabs().add(newTab);
        if (!tabsNames.containsKey(tabbedPane))
            tabsNames.put(tabbedPane, new HashMap<>());
        tabsNames.get(tabbedPane).put(tabName, tabbedPane.getTabs().size()-1);
    }

    private void setupApplyTabUi() {
        boolean rememberPaths = false;
        boolean replaceFiles = false;

        projectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchingInfo").getString("projectPath"));
        patchPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchingInfo").getString("patchPath"));
        rememberPaths = authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchingInfo").getBoolean("rememberPaths");
        replaceFiles = authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchingInfo").getBoolean("replaceFiles");

        Label projectPathLabel = new Label("Path to project:");
        projectPathLabel.setPrefSize(105, 25);
        projectPathField = new TextField(projectPath.toString());
        projectPathField.setEditable(true);
        chooseProjectButton = new Button("browse");
        chooseProjectButton.setPrefSize(70, 0);

        AnchorPane projectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(projectPathLabel, 5d);
        AnchorPane.setLeftAnchor(projectPathField, 5d + projectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(projectPathField, 5d + chooseProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseProjectButton, 5d);
        projectPathPanel.getChildren().addAll(projectPathLabel, projectPathField, chooseProjectButton);

        Label patchPathLabel = new Label("Path to patch:");
        patchPathLabel.setPrefSize(105, 25);
        patchPathField = new TextField(patchPath.toString());
        patchPathField.setEditable(true);
        choosePatchButton = new Button("browse");
        choosePatchButton.setPrefSize(70, 0);

        AnchorPane patchPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(patchPathLabel, 5d);
        AnchorPane.setLeftAnchor(patchPathField, 5d + patchPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(patchPathField, 5d + choosePatchButton.getPrefWidth());
        AnchorPane.setRightAnchor(choosePatchButton, 5d);
        patchPathPanel.getChildren().addAll(patchPathLabel, patchPathField, choosePatchButton);

        rememberApplyPathsCheckbox = new CheckBox("Remember");
        rememberApplyPathsCheckbox.setSelected(rememberPaths);
        replaceFilesCheckbox = new CheckBox("Replace old files");
        replaceFilesCheckbox.setSelected(replaceFiles);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(rememberApplyPathsCheckbox, replaceFilesCheckbox);

        applyPatchButton = new Button("Patch");
        applyPatchButton.setPrefSize(60, 0);

        activeCourgettesApplyAmount = new Label("Active Courgette instances:\t0");

        VBox tabContent = new VBox();
        tabContent.setAlignment(Pos.TOP_CENTER);
        tabContent.setPadding(new Insets(5));
        tabContent.getChildren().addAll(projectPathPanel, patchPathPanel,
                checkboxPanel, applyPatchButton, activeCourgettesApplyAmount);

        applyTab = new Tab();
        applyTab.setContent(tabContent);
    }

    private void setupRemoteApplyTabUi() {
        boolean rememberPaths = false;

        remoteProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").getString("projectPath"));
        rememberPaths = authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").getBoolean("rememberPaths");

        Label projectPathLabel = new Label("Path to project:");
        projectPathLabel.setPrefSize(105, 25);
        remoteProjectPathField = new TextField(remoteProjectPath.toString());
        remoteProjectPathField.setEditable(true);
        chooseRemoteProjectButton = new Button("browse");
        chooseRemoteProjectButton.setPrefSize(70, 0);

        AnchorPane projectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(projectPathLabel, 5d);
        AnchorPane.setLeftAnchor(remoteProjectPathField, 5d + projectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(remoteProjectPathField, 5d + chooseRemoteProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseRemoteProjectButton, 5d);
        projectPathPanel.getChildren().addAll(projectPathLabel, remoteProjectPathField, chooseRemoteProjectButton);

        remoteRememberApplyPathsCheckbox = new CheckBox("Remember");
        remoteRememberApplyPathsCheckbox.setSelected(rememberPaths);

        remoteReplaceFilesCheckbox = new CheckBox("Replace files");
        remoteReplaceFilesCheckbox.setSelected(false);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(remoteRememberApplyPathsCheckbox, remoteReplaceFilesCheckbox);

        remoteApplyPatchButton = new Button("Patch to latest version");
        remoteApplyPatchButton.setPrefSize(150, 0);
        remoteApplyPatchButton.setDisable(true);

        remoteActiveCourgettesApplyAmount = new Label("Active Courgette instances:\t0");
        remoteApplyStatus = new Label("Status: idle");

        applyPatchTabContent = new VBox();
        applyPatchTabContent.setAlignment(Pos.TOP_CENTER);
        applyPatchTabContent.setPadding(new Insets(5));
        applyPatchTabContent.getChildren().addAll(projectPathPanel, checkboxPanel,
                remoteApplyPatchButton, remoteActiveCourgettesApplyAmount, remoteApplyStatus);
    }

    private void customiseFactory(TableColumn<HistoryTableItem, Object> columnCel) {
        columnCel.setCellFactory(column -> {
            return new TableCell<HistoryTableItem, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String strItem = item.toString();
                        setText(strItem);
                        HistoryTableItem tableItem = getTableView().getItems().get(getIndex());


                        if (tableItem.getIsRoot()) {
                            Font font = Font.font(getFont().getName(), FontWeight.BOLD, FontPosture.REGULAR, getFont().getSize());
                            setFont(font);
                        } else {
                            Font font = Font.font(getFont().getName(), FontWeight.NORMAL, FontPosture.REGULAR, getFont().getSize());
                            setFont(font);
                        }
                    }
                }
            };
        });
    }

    private void updateTableContent(ObservableList<HistoryTableItem> versions) {
        Task<Void> task = new Task<>() {
            @Override public Void call() {
                remoteApplyPatchButton.setDisable(true);
                checkoutButton.setDisable(true);
                versions.clear();
                JSONObject versionsHistory = null;
                try {
                    versionsHistory = VersionsEndpoint.getHistory();
    
                    if (versionsHistory.getBoolean("success")) {
                        versionsHistory.getJSONArray("versions").forEach(v -> {
                            if (((JSONObject)v).getBoolean("is_root")) {
                                rootVersion = new VersionEntity(((JSONObject)v).put("files", new JSONArray()));
                                versions.add(new HistoryTableItem(rootVersion));
                                remoteApplyPatchButton.setDisable(false);
                            } else {
                                versions.add(new HistoryTableItem(new VersionEntity(((JSONObject)v).put("files", new JSONArray()))));
                            }
                        });
                    }
                } catch (IOException e) {
                    AlertWindow.showErrorWindow("Cannot load history");
                    e.printStackTrace();
                }
                checkoutButton.setDisable(false);
                return null;
            }
        };

        new Thread(task).start();
    }
    private void updateTableContent(TableView<HistoryTableItem> table) {
        ObservableList<HistoryTableItem> versions = table.getItems();
        updateTableContent(versions);
        table.setItems(versions);
    }

    private void setupHistoryTabUi() {
        String[] columnNames = {"Version", "Date", "Files amount", "Total size"};

        ObservableList<HistoryTableItem> versions = FXCollections.observableArrayList();

        TableView<HistoryTableItem> table = new TableView<>();
        table.setItems(versions);

        TableColumn<HistoryTableItem, Object> versionColumn = new TableColumn<>(columnNames[0]);
        versionColumn.setCellValueFactory(new PropertyValueFactory<>("versionString"));
        customiseFactory(versionColumn);
        table.getColumns().add(versionColumn);
        versionColumn.setMinWidth(90);

        TableColumn<HistoryTableItem, Object> dateColumn = new TableColumn<>(columnNames[1]);
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        customiseFactory(dateColumn);
        table.getColumns().add(dateColumn);

        TableColumn<HistoryTableItem, Object> filesColumn = new TableColumn<>(columnNames[2]);
        filesColumn.setCellValueFactory(new PropertyValueFactory<>("filesCount"));
        customiseFactory(filesColumn);
        table.getColumns().add(filesColumn);

        TableColumn<HistoryTableItem, Object> sizeColumn = new TableColumn<>(columnNames[3]);
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("totalSize"));
        customiseFactory(sizeColumn);
        table.getColumns().add(sizeColumn);
        sizeColumn.setMinWidth(100);

        checkoutButton = new Button("Checkout");
        checkoutButton.setMinSize(70, 0);
        checkoutButton.setDisable(true);

        TableView.TableViewSelectionModel<HistoryTableItem> selectionModel = table.getSelectionModel();
        selectionModel.selectedItemProperty().addListener(new ChangeListener<HistoryTableItem>() {
            @Override
            public void changed(ObservableValue<? extends HistoryTableItem> val, HistoryTableItem oldVal, HistoryTableItem newVal) {
                if (newVal != null) {
                    checkoutVersion = newVal.getVersion();
                    checkoutButton.setDisable(false);
                }
            }
        });

        HBox tablePane = new HBox();
        HBox.setHgrow(table, Priority.ALWAYS);
        tablePane.setAlignment(Pos.CENTER_LEFT);
        tablePane.setPadding(new Insets(5));
        tablePane.getChildren().addAll(table, checkoutButton);

        updateTableContent(table);
        
        boolean rememberPaths = false;

        remoteProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").getString("projectPath"));
        rememberPaths = authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").getBoolean("rememberPaths");

        Label projectPathLabel = new Label("Path to project:");
        projectPathLabel.setPrefSize(105, 25);
        checkoutProjectPathField = new TextField(remoteProjectPath.toString());
        checkoutProjectPathField.setEditable(true);
        chooseCheckoutProjectButton = new Button("browse");
        chooseCheckoutProjectButton.setPrefSize(70, 0);

        AnchorPane projectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(projectPathLabel, 5d);
        AnchorPane.setLeftAnchor(checkoutProjectPathField, 5d + projectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(checkoutProjectPathField, 5d + chooseCheckoutProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseCheckoutProjectButton, 5d);
        projectPathPanel.getChildren().addAll(projectPathLabel, checkoutProjectPathField, chooseCheckoutProjectButton);

        historyRememberPathsCheckbox = new CheckBox("Remember");
        historyRememberPathsCheckbox.setSelected(rememberPaths);

        historyReplaceFilesCheckbox = new CheckBox("Replace files");
        historyReplaceFilesCheckbox.setSelected(false);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(historyRememberPathsCheckbox, historyReplaceFilesCheckbox);

        historyActiveCourgettesAmount = new Label("Active Courgette instances:\t0");
        historyCheckoutStatus = new Label("Status: idle");

        historyTabContent = new VBox();
        historyTabContent.setAlignment(Pos.TOP_CENTER);
        historyTabContent.setPadding(new Insets(5));
        historyTabContent.getChildren().addAll(tablePane, projectPathPanel, checkboxPanel,
                historyActiveCourgettesAmount, historyCheckoutStatus);
    }

    private void setupRemoteGenTabUi() {

        boolean rememberPaths = false;

        remoteOldProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("oldProjectPath"));
        remoteNewProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("newProjectPath"));

        Label oldProjectPathLabel = new Label("Path to old version:");
        oldProjectPathLabel.setPrefSize(135, 25);
        remoteOldProjectPathField = new TextField(remoteOldProjectPath.toString());
        remoteOldProjectPathField.setEditable(true);
        chooseRemoteOldProjectButton = new Button("browse");
        chooseRemoteOldProjectButton.setPrefSize(70, 0);

        AnchorPane oldProjectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(oldProjectPathLabel, 5d);
        AnchorPane.setLeftAnchor(remoteOldProjectPathField, 5d + oldProjectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(remoteOldProjectPathField, 5d + chooseRemoteOldProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseRemoteOldProjectButton, 5d);
        oldProjectPathPanel.getChildren().addAll(oldProjectPathLabel, remoteOldProjectPathField, chooseRemoteOldProjectButton);

        Label newProjectPathLabel = new Label("Path to new version:");
        newProjectPathLabel.setPrefSize(135, 25);
        remoteNewProjectPathField = new TextField(remoteNewProjectPath.toString());
        remoteNewProjectPathField.setEditable(true);
        chooseRemoteNewProjectButton = new Button("browse");
        chooseRemoteNewProjectButton.setPrefSize(70, 0);

        AnchorPane newProjectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(newProjectPathLabel, 5d);
        AnchorPane.setLeftAnchor(remoteNewProjectPathField, 5d + newProjectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(remoteNewProjectPathField, 5d + chooseRemoteNewProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseRemoteNewProjectButton, 5d);
        newProjectPathPanel.getChildren().addAll(newProjectPathLabel, remoteNewProjectPathField, chooseRemoteNewProjectButton);

        remoteRememberGenPathsCheckbox = new CheckBox("Remember");
        remoteRememberGenPathsCheckbox.setSelected(rememberPaths);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(remoteRememberGenPathsCheckbox);

        remoteGenPatchButton = new Button("Create patch");
        remoteGenPatchButton.setPrefSize(110, 0);

        remoteActiveCourgettesGenAmount = new Label("Active Courgette instances:\t0");

        genPatchTabContent = new VBox();
        genPatchTabContent.setAlignment(Pos.TOP_CENTER);
        genPatchTabContent.setPadding(new Insets(5));
        genPatchTabContent.getChildren().addAll(oldProjectPathPanel, newProjectPathPanel,
                checkboxPanel, remoteGenPatchButton, remoteActiveCourgettesGenAmount);
    }

    private void setupGenTabUi() {
        boolean rememberPaths = false;

        rememberPaths = authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getBoolean("rememberPaths");

        oldProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("oldProjectPath"));
        newProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("newProjectPath"));
        patchFolderPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("patchPath"));

        Label oldProjectPathLabel = new Label("Path to old version:");
        oldProjectPathLabel.setPrefSize(135, 25);
        oldProjectPathField = new TextField(oldProjectPath.toString());
        oldProjectPathField.setEditable(true);
        chooseOldProjectButton = new Button("browse");
        chooseOldProjectButton.setPrefSize(70, 0);

        AnchorPane oldProjectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(oldProjectPathLabel, 5d);
        AnchorPane.setLeftAnchor(oldProjectPathField, 5d + oldProjectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(oldProjectPathField, 5d + chooseOldProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseOldProjectButton, 5d);
        oldProjectPathPanel.getChildren().addAll(oldProjectPathLabel, oldProjectPathField, chooseOldProjectButton);

        Label newProjectPathLabel = new Label("Path to new version:");
        newProjectPathLabel.setPrefSize(135, 25);
        newProjectPathField = new TextField(newProjectPath.toString());
        newProjectPathField.setEditable(true);
        chooseNewProjectButton = new Button("browse");
        chooseNewProjectButton.setPrefSize(70, 0);

        AnchorPane newProjectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(newProjectPathLabel, 5d);
        AnchorPane.setLeftAnchor(newProjectPathField, 5d + newProjectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(newProjectPathField, 5d + chooseNewProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseNewProjectButton, 5d);
        newProjectPathPanel.getChildren().addAll(newProjectPathLabel, newProjectPathField, chooseNewProjectButton);

        Label genPatchPathLabel = new Label("Path to patch folder:");
        genPatchPathLabel.setPrefSize(135, 25);
        genPatchPathField = new TextField(patchFolderPath.toString());
        genPatchPathField.setEditable(true);
        chooseGenPatchButton = new Button("browse");
        chooseGenPatchButton.setPrefSize(70, 0);

        AnchorPane patchPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(genPatchPathLabel, 5d);
        AnchorPane.setLeftAnchor(genPatchPathField, 5d + genPatchPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(genPatchPathField, 5d + chooseGenPatchButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseGenPatchButton, 5d);
        patchPathPanel.getChildren().addAll(genPatchPathLabel, genPatchPathField, chooseGenPatchButton);

        rememberGenPathsCheckbox = new CheckBox("Remember");
        rememberGenPathsCheckbox.setSelected(rememberPaths);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(rememberGenPathsCheckbox);

        genPatchButton = new Button("Create patch");
        genPatchButton.setPrefSize(110, 0);

        activeCourgettesGenAmount = new Label("Active Courgette instances:\t0");

        VBox genTabContent = new VBox();
        genTabContent.setAlignment(Pos.TOP_CENTER);
        genTabContent.setPadding(new Insets(5));
        genTabContent.getChildren().addAll(oldProjectPathPanel, newProjectPathPanel,
                patchPathPanel, checkboxPanel, genPatchButton, activeCourgettesGenAmount);

        genTab = new Tab();
        genTab.setContent(genTabContent);
    }

    private void setupLoginUi(Tab tab, Button button) {
        VBox loginpanel = new VBox();
        loginpanel.setAlignment(Pos.CENTER);

        Label loginMessage = new Label("You are not logged in");
        button.setText("Login");

        loginpanel.getChildren().addAll(loginMessage, button);

        tab.setContent(loginpanel);
    }

    private void setupEvents() {
        choosePatchButton.setOnAction(e -> {
            choosePath(patchPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        chooseGenPatchButton.setOnAction(e -> {
            choosePath(genPatchPathField, JFileChooser.DIRECTORIES_ONLY);
        });
        chooseProjectButton.setOnAction(e -> {
            choosePath(projectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        chooseNewProjectButton.setOnAction(e -> {
            choosePath(newProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        chooseOldProjectButton.setOnAction(e -> {
            choosePath(oldProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        applyPatchButton.setOnAction(e -> {
            applyPatchButton.setDisable(true);
            projectPath = Paths.get(projectPathField.getText());
            patchPath = Paths.get(patchPathField.getText());
            Path tmpProjectPath = projectPath.getParent().resolve("patched_tmp").resolve(projectPath.getFileName());

            if (!authWindow.config.getJSONObject(RunCourgette.os).has("localPatchingInfo")) {
                authWindow.config.getJSONObject(RunCourgette.os).put("localPatchingInfo", new JSONObject());
            }
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchingInfo").put("projectPath", projectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchingInfo").put("patchPath", patchPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchingInfo").put("rememberPaths", rememberApplyPathsCheckbox.isSelected());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchingInfo").put("replaceFiles", replaceFilesCheckbox.isSelected());
            authWindow.saveConfig();

            FileVisitor fileVisitor = null;
            try {
                fileVisitor = new FileVisitor();
            } catch (IOException e1) {
                AlertWindow.showErrorWindow("Cannot walk project file tree");
                e1.printStackTrace();
            }

            List<Path> oldFiles = null;
            List<Path> patchFiles = null;

            try {
                oldFiles = fileVisitor.walkFileTree(projectPath);
                patchFiles = fileVisitor.walkFileTree(patchPath);
            } catch (IOException e1) {
                AlertWindow.showErrorWindow("Cannot walk project file tree");
                e1.printStackTrace();
            }

            Path relativePatchPath;
            Path newPath;
            Path oldPath;
            byte[] emptyData = {0};
    
            for (Path patchFile: patchFiles) {
                relativePatchPath = patchPath.relativize(patchFile);
                newPath = tmpProjectPath.resolve(relativePatchPath.toString().equals("") ?
                        Paths.get("..", "..", "..", tmpProjectPath.getParent().getFileName().toString(),
                                tmpProjectPath.getFileName().toString()).toString() :
                        relativePatchPath.toString().substring(0, relativePatchPath.toString().length() - "_patch".length())).normalize();
                oldPath = projectPath.resolve(relativePatchPath.toString().equals("") ? "" :
                        relativePatchPath.toString().substring(0, relativePatchPath.toString().length() - "_patch".length())).normalize();

                if (!oldFiles.contains(oldPath)) {
                    try {
                        oldPath.getParent().toFile().mkdirs();
                        Files.createFile(oldPath);
                        Files.write(oldPath, emptyData);
                    } catch (IOException e1) {
                        AlertWindow.showErrorWindow("Cannot create patch file");
                        e1.printStackTrace();
                        return;
                    }
                }
    
                try {
                    Files.createDirectories(newPath.getParent());
                } catch (IOException e1) {
                    AlertWindow.showErrorWindow("Cannot create patch files directory");
                    e1.printStackTrace();
                    return;
                }
                new CourgetteHandler().applyPatch(oldPath.toString(), newPath.toString(), patchFile.toString(),
                        replaceFilesCheckbox.isSelected(), activeCourgettesApplyAmount, false);
            }
            applyPatchButton.setDisable(false);
        });
        genPatchButton.setOnAction(e -> {
            genPatchButton.setDisable(true);
            oldProjectPath = Paths.get(oldProjectPathField.getText());
            newProjectPath = Paths.get(newProjectPathField.getText());
            patchFolderPath = Paths.get(genPatchPathField.getText());

            if (!authWindow.config.getJSONObject(RunCourgette.os).has("localPatchCreationInfo")) {
                authWindow.config.getJSONObject(RunCourgette.os).put("localPatchCreationInfo", new JSONObject());
            }
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchCreationInfo").put("patchPath", patchFolderPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchCreationInfo").put("oldProjectPath", oldProjectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchCreationInfo").put("newProjectPath", newProjectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchCreationInfo").put("rememberPaths", rememberGenPathsCheckbox.isSelected());
            authWindow.saveConfig();

            FileVisitor fileVisitor = null;
            try {
                fileVisitor = new FileVisitor();
            } catch (IOException e1) {
                AlertWindow.showErrorWindow("Cannot walk project file tree");
                e1.printStackTrace();
            }

            ArrayList<Path> oldFiles = new ArrayList<>();
            ArrayList<Path> newFiles = new ArrayList<>();

            try {
                oldFiles = new ArrayList<>(fileVisitor.walkFileTree(oldProjectPath));
                newFiles = new ArrayList<>(fileVisitor.walkFileTree(newProjectPath));
            } catch (IOException e1) {
                AlertWindow.showErrorWindow("Cannot walk project file tree");
                e1.printStackTrace();
            }
            
            generatePatch(oldProjectPath, newProjectPath, oldFiles, newFiles, "forward", activeCourgettesGenAmount);
            generatePatch(newProjectPath, oldProjectPath, newFiles, oldFiles, "backward", activeCourgettesGenAmount);
            genPatchButton.setDisable(false);
        });

        applyPatchLoginButton.setOnAction(e -> {
            if (authWindow.isShowing())
                authWindow.hide();
            else
                authWindow.show();
        });
        genPatchLoginButton.setOnAction(e -> {
            if (authWindow.isShowing())
                authWindow.hide();
            else
                authWindow.show();
        });
        historyLoginButton.setOnAction(e -> {
            if (authWindow.isShowing())
                authWindow.hide();
            else
                authWindow.show();
        });

        authWindow.btnConnect.setOnAction(e -> {
            authWindow.userLogin = authWindow.loginField.getText();
            authWindow.userPassword = authWindow.passField.getText();
            authWindow.urlApi = authWindow.urlField.getText();

            if (!authWindow.config.has("userInfo")) {
                authWindow.config.put("userInfo", new JSONObject());
            }
            authWindow.config.getJSONObject("userInfo").put("login", authWindow.userLogin);
            authWindow.config.getJSONObject("userInfo").put("pass", authWindow.userPassword);
            authWindow.config.getJSONObject("userInfo").put("url", authWindow.urlApi);
            authWindow.saveConfig();
            authWindow.updateAccessRights();
            authWindow.hide();
            
            if (authWindow.curAccess == AuthWindow.ACCESS.ADMIN) {
                Connector.setBaseUrl(authWindow.urlApi);

                setupRemoteUi();
                setupRemoteEvents();

                remoteTabs.getTabs().get(tabsNames.get(remoteTabs).get("Generate")).setContent(genPatchTabContent);
                remoteTabs.getTabs().get(tabsNames.get(remoteTabs).get("History")).setContent(historyTabContent);
                remoteTabs.getTabs().get(tabsNames.get(remoteTabs).get("Patching")).setContent(applyPatchTabContent);
            }
        });
    }

    private void setupRemoteEvents() {
        chooseRemoteProjectButton.setOnAction(e -> {
            choosePath(remoteProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        chooseRemoteNewProjectButton.setOnAction(e -> {
            choosePath(remoteNewProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        chooseRemoteOldProjectButton.setOnAction(e -> {
            choosePath(remoteOldProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        checkoutButton.setOnAction(e -> {
            checkoutButton.setDisable(true);
            if (checkoutVersion != null) {
                // TODO: CHECKOUT PLACEHOLDER
                System.out.print("Checkout to version ");
                System.out.println(checkoutVersion.getVersionString());
            } else {
                System.out.println("No version selected");
            }
            checkoutButton.setDisable(false);
        });

        remoteApplyPatchButton.setOnAction(e -> {
            checkoutToVersion(Paths.get(remoteProjectPathField.getText()), remoteReplaceFilesCheckbox.isSelected(),
                    rootVersion.getVersionString(), remoteApplyStatus, remoteActiveCourgettesApplyAmount, remoteApplyPatchButton);
        });

        remoteGenPatchButton.setOnAction(e -> {
            remoteGenPatchButton.setDisable(true);
            remoteOldProjectPath = Paths.get(remoteOldProjectPathField.getText());
            remoteNewProjectPath = Paths.get(remoteNewProjectPathField.getText());
            Path patchFolderPath = remoteNewProjectPath.getParent().resolve("tmp_patch");

            if (!authWindow.config.getJSONObject(RunCourgette.os).has("remotePatchCreationInfo")) {
                authWindow.config.getJSONObject(RunCourgette.os).put("remotePatchCreationInfo", new JSONObject());
            }
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("remotePatchCreationInfo").put("oldProjectPath", remoteOldProjectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("remotePatchCreationInfo").put("newProjectPath", remoteNewProjectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("remotePatchCreationInfo").put("rememberPaths", remoteRememberGenPathsCheckbox.isSelected());
            authWindow.saveConfig();

            FileVisitor fileVisitor = null;
            try {
                fileVisitor = new FileVisitor(remoteNewProjectPath);
            } catch (IOException e1) {
                AlertWindow.showErrorWindow("Cannot walk project file tree");
                e1.printStackTrace();
            }

            ArrayList<Path> oldFiles = new ArrayList<>();
            ArrayList<Path> newFiles = new ArrayList<>();

            try {
                oldFiles = new ArrayList<>(fileVisitor.walkFileTree(remoteOldProjectPath));
                newFiles = new ArrayList<>(fileVisitor.walkFileTree(remoteNewProjectPath));
            } catch (IOException e1) {
                AlertWindow.showErrorWindow("Cannot walk project file tree");
                e1.printStackTrace();
            }
            
            generatePatch(patchFolderPath, remoteOldProjectPath, remoteNewProjectPath, oldFiles, newFiles, "forward", activeCourgettesGenAmount);
            generatePatch(patchFolderPath, remoteNewProjectPath, remoteOldProjectPath, newFiles, oldFiles, "backward", activeCourgettesGenAmount);

            // TODO: implement upload
            Directories.deleteDirectory(patchFolderPath);
            remoteGenPatchButton.setDisable(false);
        });
    }

    private void checkoutToVersion(Path projectPath, boolean replaceFiles, String toVersion,
            Label statusLabel, Label courgettesAmountLabel, Button button) {
        button.setDisable(true);
        StringBuffer checkoutDump = new StringBuffer();

        Path tmpProjectPath = projectPath.getParent().resolve("patched_tmp").resolve(projectPath.getFileName());
        Path tmpPatchPath = projectPath.getParent().resolve("patch_tmp").resolve(projectPath.getFileName());

        if (!authWindow.config.getJSONObject(RunCourgette.os).has("remotePatchingInfo")) {
            authWindow.config.getJSONObject(RunCourgette.os).put("remotePatchingInfo", new JSONObject());
        }
        authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").put("projectPath", projectPath.toString());
        authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").put("rememberPaths", remoteRememberApplyPathsCheckbox.isSelected());
        authWindow.saveConfig();

        String currentVersion = null;
        if (Files.exists(projectPath.resolve("config.json"))) {
            File file = new File(projectPath.resolve("config.json").toString());
            String content;
            try {
                content = new String(Files.readAllBytes(Paths.get(file.toURI())));
                currentVersion = new JSONObject(content).getString("currentVersion");
            } catch (IOException ee) {
                AlertWindow.showErrorWindow("Cannot open project config file");
                ee.printStackTrace();
                return;
            }
        }

        Map<String, String> params = new HashMap<>();
        params.put("v_from", currentVersion);
        params.put("v_to", toVersion);

        Task<Void> task = new Task<>() {
            @Override public Void call() throws InterruptedException, IOException {

                Instant start = Instant.now();
                AtomicLong counter = new AtomicLong(0);
                JSONObject response = null;
                try {
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: getting patch sequence from " + params.get("v_from") + " to " + params.get("v_to"));
                    });
                    response = VersionsEndpoint.getSwitch(params);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                ArrayList<Path> subfolderSequence = new ArrayList<>();

                Map<Path, ArrayList<Map<String, String>>> patchParams = new HashMap<>();

                counter.set(0);
                response.getJSONArray("files").forEach(fileItem -> {
                    JSONObject file = (JSONObject)fileItem;
                    file.getJSONArray("patches").forEach(patchItem -> {
                        JSONObject patch = (JSONObject)patchItem;
                        try {
                            Path subfolderPath = tmpPatchPath.resolve(
                                    "from_" + patch.getString("version_from") + "_to_" + patch.getString("version_to"));
                            if (!subfolderSequence.contains(subfolderPath)) {
                                subfolderSequence.add(subfolderPath);
                                patchParams.put(subfolderPath, new ArrayList<>());
                            }

                            patchParams.get(subfolderPath).add(new HashMap<>(
                                Map.of(
                                    "v_from", patch.getString("version_from"),
                                    "v_to", patch.getString("version_to"),
                                    "file_location", file.getString("location")
                                )
                            ));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                });

                FileVisitor fileVisitor = new FileVisitor();

                ArrayList<Path> oldFiles = new ArrayList<>();

                oldFiles = new ArrayList<>(fileVisitor.walkFileTree(projectPath));
                checkoutDump.append("old files amount ").append(oldFiles.size()).append(System.lineSeparator());

                Path relativePatchPath;
                Path newPath;
                Path oldPath;
                byte[] emptyData = {0};

                ArrayList<CourgetteHandler> threads = new ArrayList<>();

                CourgetteHandler.setMAX_THREADS_AMOUNT(30);
                CourgetteHandler.setMAX_ACTIVE_COURGETTES_AMOUNT(20);

                for (Path folder: subfolderSequence) {
                    counter.addAndGet(patchParams.get(folder).size());
                }

                final long patchesAmount = counter.get();
                CourgetteHandler.setTotalThreadsAmount((int)patchesAmount);

                counter.set(0);
                for (Path folder: subfolderSequence) {
                    for (Map<String, String> patchParam: patchParams.get(folder)) {
                        relativePatchPath = Paths.get(patchParam.get("file_location"));
                        Path patchFile = folder.resolve(relativePatchPath.toString());
                        newPath = tmpProjectPath.resolve(relativePatchPath.toString().equals("") ?
                                Paths.get("..", "..", "..", tmpProjectPath.getFileName().toString()) :
                                relativePatchPath).normalize();
                        oldPath = projectPath.resolve(relativePatchPath).normalize();
                        String statusStr = relativePatchPath.toString();
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: downloading " + statusStr);
                        });
                        PatchesEndpoint.getFile(folder.resolve(relativePatchPath), patchParam);

                        if (!oldFiles.contains(oldPath)) {
                            try {
                                oldPath.getParent().toFile().mkdirs();
                                Files.createFile(oldPath);
                                Files.write(oldPath, emptyData);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
            
                        try {
                            Files.createDirectories(newPath.getParent());
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        checkoutDump.append("\tpatching ").append(patchFile).append(System.lineSeparator());
                        CourgetteHandler thread = new CourgetteHandler();
                        thread.applyPatch(oldPath.toString(), newPath.toString(), patchFile.toString(),
                                false, courgettesAmountLabel, false);
                        threads.add(thread);
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: patching " + folder.relativize(patchFile).toString());
                            courgettesAmountLabel.setText("Active Courgette instances:\t"
                                    + CourgetteHandler.activeCount() + "" +
                                    System.lineSeparator() + "Files remains:\t" +
                                    (patchesAmount - counter.getAndIncrement()));
                        });
                        while (CourgetteHandler.activeCount() >= CourgetteHandler.getMAX_THREADS_AMOUNT()) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                AlertWindow.showErrorWindow("Cannot handle max courgette threads amount");
                                e.printStackTrace();
                            }
                        }
                        CourgetteHandler.decreaseTotalThreadsAmount();
                    }

                    for (CourgetteHandler thread: threads) {
                        thread.join();
                    }

                    // Directories.deleteDirectory(folder);
                }
                // if (tmpPatchPath.getParent().endsWith("patch_tmp"))
                //     Directories.deleteDirectory(tmpPatchPath.getParent());
                
                Path patchedProjectPath = null;
                if (!replaceFiles) {
                    patchedProjectPath = tmpProjectPath;

                    Files.copy(projectPath.resolve(".psheignore"),
                            tmpProjectPath.resolve(".psheignore"), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    patchedProjectPath = projectPath;
                }

                JSONObject updatedConfig = new JSONObject().put("currentVersion", toVersion);
                try {
                    Directories.saveJSONFile(patchedProjectPath.resolve("config.json"), updatedConfig);
                } catch (JSONException | IOException e1) {
                    AlertWindow.showErrorWindow("Cannot update project config file");
                    e1.printStackTrace();
                }

                Map<Path, Path> patchedFiles = new HashMap<>();
                for (Path filePath: fileVisitor.walkFileTree(patchedProjectPath)) {
                    patchedFiles.put(patchedProjectPath.relativize(filePath), filePath);
                }

                Platform.runLater(() -> {
                    statusLabel.setText("Status: checking project integrity, this can take awhile");
                });
                Map<String, ArrayList<Path>> integrityResult = IntegrityChecker.checkRemoteIntegrity(patchedFiles, projectPath, toVersion);
                counter.set(0);
                checkoutDump.append("removed files amount ").append(integrityResult.get("deleted").size()).append(System.lineSeparator());
                for (Path file: integrityResult.get("deleted")) {
                    checkoutDump.append("deleted ").append(file).append(System.lineSeparator());
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: deleting " + file.toString());
                        courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                                System.lineSeparator() + "Files remains:\t" +
                                (integrityResult.get("deleted").size() - counter.getAndIncrement()));
                    });
                    Files.deleteIfExists(file);
                }
                counter.set(0);
                checkoutDump.append("failed integrity files amount ").append(integrityResult.get("failed").size()).append(System.lineSeparator());
                for (Path file: integrityResult.get("failed")) {
                    checkoutDump.append("\tre-download ").append(file).append(System.lineSeparator());
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: downloading " + file.toString());
                        courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                                System.lineSeparator() + "Files remains:\t" +
                                (integrityResult.get("failed").size() - counter.getAndIncrement()));
                    });
                    
                    FilesEndpoint.getRoot(file, Map.of("location", file.toString()));
                    if (!toVersion.equals(rootVersion.getVersionString())) {
                        // TODO: checkout from root file to toVersion
                    }
                }
                counter.set(0);
                checkoutDump.append("unchanged files amount ").append(integrityResult.get("unchanged").size()).append(System.lineSeparator());
                for (Path oldFile: integrityResult.get("unchanged")) {
                    checkoutDump.append("\tmoving unchanged files ").append(oldFile).append(System.lineSeparator());
                    System.out.println("moving " + oldFile.toString() + " to "
                            + patchedProjectPath.resolve(projectPath.relativize(oldFile)));
                    String statusStr = patchedProjectPath.toString();
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: moving " + oldFile.toString() + " to "
                                + Paths.get(statusStr, projectPath.relativize(oldFile).toString()));
                        courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                                System.lineSeparator() + "Files remains:\t" +
                                (integrityResult.get("unchanged").size() - counter.getAndIncrement()));
                    });

                    Files.copy(oldFile, patchedProjectPath.resolve(projectPath.relativize(oldFile)), StandardCopyOption.REPLACE_EXISTING);
                }
                counter.set(0);
                checkoutDump.append("missing files amount ").append(integrityResult.get("missing").size()).append(System.lineSeparator());
                for (Path remoteFile: integrityResult.get("missing")) {
                    checkoutDump.append("\tre-download ").append(remoteFile).append(System.lineSeparator());
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: downloading " + remoteFile.toString());
                        courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                                System.lineSeparator() + "Files remains:\t" +
                                (integrityResult.get("missing").size() - counter.getAndIncrement()));
                    });
                    
                    FilesEndpoint.getRoot(patchedProjectPath.resolve(remoteFile), Map.of("location", remoteFile.toString()));
                    if (!toVersion.equals(rootVersion.getVersionString())) {
                        // TODO: checkout from root file to toVersion
                    }
                }

                CourgetteHandler.setTotalThreadsAmount(0);
                if (replaceFiles) {
                    counter.set(0);
                    List<Path> totalPatchedFiles = fileVisitor.walkFileTree(patchedProjectPath);
                    String pathString = patchedProjectPath.toString();
                    for (Path file: totalPatchedFiles) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: updating " + Paths.get(pathString).relativize(file).toString());
                            courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                                    System.lineSeparator() + "Files remains:\t" +
                                    (totalPatchedFiles.size() - counter.getAndIncrement()));
                        });
                        try {
                            Files.copy(file, projectPath.resolve(patchedProjectPath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    counter.set(0);
                    for (Path file: integrityResult.get("deleted")) {
                        Path targetFile = projectPath.resolve(Paths.get(pathString).relativize(file));
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: deleting " + file.toString());
                            courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                                    System.lineSeparator() + "Files remains:\t" +
                                    (integrityResult.get("deleted").size() - counter.getAndIncrement()));
                        });
                        Files.deleteIfExists(targetFile);
                    }
                }

                Platform.runLater(() -> {
                    courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                            System.lineSeparator() + "Files remains:\t0");
                });

                Instant finish = Instant.now();
                StringBuilder str = new StringBuilder("Status: done ");
                str.append(ChronoUnit.MINUTES.between(start, finish));
                str.append(" mins ");
                str.append(ChronoUnit.SECONDS.between(start, finish) - ChronoUnit.MINUTES.between(start, finish)*60);
                str.append(" secs");
                Platform.runLater(() -> {
                    statusLabel.setText(str.toString());
                });

                button.setDisable(false);
                
                BufferedWriter writer = new BufferedWriter(new FileWriter("dump.txt"));
                writer.write(checkoutDump.toString());
                writer.close();

                return null;
            }
        };
        new Thread(task).start();
    }

    private void generatePatch(Path patchFolderPath, Path oldProjectPath, Path newProjectPath, ArrayList<Path> oldFiles,
            ArrayList<Path> newFiles, String patchSubfolder, Label updatingComponent) {
        Path relativeOldPath;
        Path newPath;
        Path patchFile;
        byte[] emptyData = {0};
        for (Path oldFile: oldFiles) {
            relativeOldPath = oldProjectPath.relativize(oldFile);
            newPath = newProjectPath.resolve(relativeOldPath).normalize();
            patchFile = patchFolderPath.resolve(patchSubfolder)
                    .resolve((relativeOldPath.toString().equals("") ? oldFile.getFileName() : relativeOldPath.toString()) + "_patch").normalize();

            if (oldFile.toFile().length() <= 1 || newPath.toFile().length() <= 1) {
                continue;
            }

            try {
                Files.createDirectories(patchFile.getParent());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            new CourgetteHandler().generatePatch(oldFile.toString(), newPath.toString(),
                    patchFile.toString(), updatingComponent, false);
        }

        Path relativeNewPath;
        Path oldPath;
        for (Path newFile: newFiles) {
            relativeNewPath = newProjectPath.relativize(newFile);
            oldPath = oldProjectPath.resolve(relativeNewPath).normalize();
            patchFile = patchFolderPath.resolve(patchSubfolder).resolve(relativeNewPath.toString() + "_patch").normalize();

            if (!oldFiles.contains(oldPath)) {
                try {
                    oldPath.getParent().toFile().mkdirs();
                    Files.createFile(oldPath);
                    Files.write(oldPath, emptyData);
                    Files.createDirectories(patchFile.getParent());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                new CourgetteHandler().generatePatch(oldPath.toString(), newFile.toString(),
                        patchFile.toString(),updatingComponent, false);
            }
        }
    }
    private void generatePatch(Path oldProjectPath, Path newProjectPath, ArrayList<Path> oldFiles,
            ArrayList<Path> newFiles, String patchSubfolder, Label updatingComponent) {
        generatePatch(patchFolderPath, oldProjectPath, newProjectPath, oldFiles, newFiles, patchSubfolder, updatingComponent);
    }

    private void choosePath(TextField field, int mode) {
        choosePath(field, mode, Paths.get(field.getText()));
    }

    private void choosePath(TextField field, int mode, Path defaultPath) {
        fileChooser = new JFileChooser();
        if (defaultPath.getParent() != null) {
            fileChooser.setCurrentDirectory(defaultPath.getParent().toFile());
        }
        fileChooser.setFileSelectionMode(mode);
        int option = fileChooser.showOpenDialog(null);
        if(option == JFileChooser.APPROVE_OPTION){
           File file = fileChooser.getSelectedFile();
           field.setText(file.getAbsolutePath());
        }
    }

    private void switchMode(VBox pane, boolean isFileMode) {
        if (isFileMode) {
            pane.getChildren().set(0, remoteTabs);
            modeSwitchButton.setText("Change to file mode");
            this.primaryStage.setTitle(windowName + " - REMOTE MODE");
            this.isFileMode = false;
        } else {
            pane.getChildren().set(0, fileTabs);
            modeSwitchButton.setText("Change to remote mode");
            this.primaryStage.setTitle(windowName + " - FILE MODE");
            this.isFileMode = true;
        }
        authWindow.config.put("defaultFilemode", this.isFileMode);
    }
}
