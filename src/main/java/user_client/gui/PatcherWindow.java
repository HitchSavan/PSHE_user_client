package user_client.gui;

import javafx.scene.control.Button;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import javax.swing.JFileChooser;

import org.json.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import patcher.utils.files_utils.Directories;
import patcher.utils.patching_utils.RunCourgette;
import patcher.remote_api.endpoints.ServiceEndpoint;
import patcher.remote_api.entities.VersionEntity;
import patcher.utils.remote_utils.Connector;
import user_client.gui.tabs.local.ApplyTab;
import user_client.gui.tabs.local.GenerateTab;
import user_client.gui.tabs.remote.RemoteApplyTab;
import user_client.gui.tabs.remote.RemoteGenerateTab;
import user_client.gui.tabs.remote.RemoteHistoryTab;
import user_client.utils.AlertWindow;

public class PatcherWindow extends Application {

    String windowName = "PSHE patcher";
    int defaultWindowWidth = 600;
    int defaultWindowHeight = 260;

    Stage primaryStage;
    Scene primaryScene;
    AuthWindow authWindow;

    TabPane fileTabs;
    TabPane remoteTabs;

    ApplyTab applyTab;
    GenerateTab genTab;
    RemoteApplyTab remoteApplyTab;
    RemoteHistoryTab historyTab;
    RemoteGenerateTab remoteGenTab;

    Button modeSwitchButton;
    boolean isFileMode;

    HashMap<TabPane, HashMap<String, Integer>> tabsNames = new HashMap<>();
    VBox applyPatchTabContent;
    VBox genPatchTabContent;
    VBox historyTabContent;
    
    VersionEntity rootVersion = null;
    Button checkoutButton;

    JFileChooser fileChooser;

    Button applyPatchLoginButton;
    Button historyLoginButton;
    Button genPatchLoginButton;

    Path projectPath;
    Path patchPath;
    Path oldProjectPath;
    Path newProjectPath;
    Path patchFolderPath;

    Path remoteProjectPath;
    Path remoteOldProjectPath;
    Path remoteNewProjectPath;
    
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
        applyTab = new ApplyTab(projectPath, patchPath, authWindow.config);
        genTab = new GenerateTab(oldProjectPath, newProjectPath, patchFolderPath, authWindow.config);
        
        fileTabs = new TabPane();
        addTab(fileTabs, "Patching", applyTab);
        addTab(fileTabs, "Generate", genTab);
        
        remoteApplyTab = new RemoteApplyTab();
        applyPatchLoginButton = new Button();
        setupLoginUi(remoteApplyTab, applyPatchLoginButton);
        historyTab = new RemoteHistoryTab();
        historyLoginButton = new Button();
        setupLoginUi(historyTab, historyLoginButton);
        remoteGenTab = new RemoteGenerateTab();
        genPatchLoginButton = new Button();
        setupLoginUi(remoteGenTab, genPatchLoginButton);
        
        remoteTabs = new TabPane();
        addTab(remoteTabs, "Patching", remoteApplyTab);
        addTab(remoteTabs, "History", historyTab);
        addTab(remoteTabs, "Generate", remoteGenTab);
    }

    private void setupLoginUi(Tab tab, Button button) {
        VBox loginpanel = new VBox();
        loginpanel.setAlignment(Pos.CENTER);

        Label loginMessage = new Label("You are not logged in");
        button.setText("Login");

        loginpanel.getChildren().addAll(loginMessage, button);

        tab.setContent(loginpanel);
    }

    private void setupRemoteUi() {
        remoteApplyTab.setupUi(projectPath, patchPath, applyPatchTabContent, authWindow.config);
        historyTab.setupUi(projectPath, historyTabContent, authWindow.config, rootVersion, List.of(remoteApplyTab.patchToRootButton));
        remoteGenTab.setupUi(oldProjectPath, newProjectPath, patchPath, genPatchTabContent, authWindow.config);
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

    private void setupEvents() {
        applyTab.setupEvents(projectPath, patchPath, authWindow.config, authWindow);
        genTab.setupEvents(oldProjectPath, newProjectPath, patchFolderPath, authWindow.config, authWindow);

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
                try {
                    if (!ServiceEndpoint.ping().getBoolean("success")) {
                        AlertWindow.showErrorWindow("Cannot connect to remote server");
                        return;
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                    AlertWindow.showErrorWindow("Cannot connect to remote server");
                    return;
                }

                setupRemoteUi();
                setupRemoteEvents();

                remoteTabs.getTabs().get(tabsNames.get(remoteTabs).get("Generate")).setContent(genPatchTabContent);
                remoteTabs.getTabs().get(tabsNames.get(remoteTabs).get("History")).setContent(historyTabContent);
                remoteTabs.getTabs().get(tabsNames.get(remoteTabs).get("Patching")).setContent(applyPatchTabContent);
            }
        });
    }

    private void setupRemoteEvents() {
        remoteApplyTab.setupEvents(projectPath, patchPath, rootVersion.getVersionString(), authWindow.config, authWindow);
        remoteGenTab.setupEvents(oldProjectPath, newProjectPath, patchPath, rootVersion.getVersionString(), authWindow.config, authWindow);
        historyTab.setupEvents();
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
