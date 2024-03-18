package user_client.gui.tabs.remote;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
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
import patcher.remote_api.endpoints.VersionsEndpoint;
import patcher.remote_api.entities.VersionEntity;
import patcher.utils.patching_utils.RunCourgette;
import user_client.utils.AlertWindow;
import user_client.utils.HistoryTableItem;

public class RemoteHistoryTab extends Tab {
    private Button checkoutButton;
    private TextField checkoutProjectPathField;
    private Button chooseCheckoutProjectButton;
    private CheckBox rememberPathsCheckbox;
    private CheckBox replaceFilesCheckbox;
    private Label activeCourgettesAmount;
    private Label checkoutStatus;
    private VersionEntity checkoutVersion;

    public void setupUi(Path projectPath, VBox historyTabContent, JSONObject config, VersionEntity rootVersion, List<Button> disablingButtons) {
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
            private VersionEntity checkoutVersion;

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

        updateTableContent(table, rootVersion, disablingButtons);
        
        boolean rememberPaths = false;

        projectPath = Paths.get(config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").getString("projectPath"));
        rememberPaths = config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").getBoolean("rememberPaths");

        Label projectPathLabel = new Label("Path to project:");
        projectPathLabel.setPrefSize(105, 25);
        checkoutProjectPathField = new TextField(projectPath.toString());
        checkoutProjectPathField.setEditable(true);
        chooseCheckoutProjectButton = new Button("browse");
        chooseCheckoutProjectButton.setPrefSize(70, 0);

        AnchorPane projectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(projectPathLabel, 5d);
        AnchorPane.setLeftAnchor(checkoutProjectPathField, 5d + projectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(checkoutProjectPathField, 5d + chooseCheckoutProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseCheckoutProjectButton, 5d);
        projectPathPanel.getChildren().addAll(projectPathLabel, checkoutProjectPathField, chooseCheckoutProjectButton);

        rememberPathsCheckbox = new CheckBox("Remember");
        rememberPathsCheckbox.setSelected(rememberPaths);

        replaceFilesCheckbox = new CheckBox("Replace files");
        replaceFilesCheckbox.setSelected(false);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(rememberPathsCheckbox, replaceFilesCheckbox);

        activeCourgettesAmount = new Label("Active Courgette instances:\t0");
        checkoutStatus = new Label("Status: idle");

        historyTabContent = new VBox();
        historyTabContent.setAlignment(Pos.TOP_CENTER);
        historyTabContent.setPadding(new Insets(5));
        historyTabContent.getChildren().addAll(tablePane, projectPathPanel, checkboxPanel,
                activeCourgettesAmount, checkoutStatus);
    }

    private void updateTableContent(ObservableList<HistoryTableItem> versions, VersionEntity rootVersion, List<Button> disablingButtons) {
        Task<Void> task = new Task<>() {
            @Override public Void call() {
                disablingButtons.forEach(button -> {
                    button.setDisable(true);
                });
                checkoutButton.setDisable(true);
                versions.clear();
                JSONObject versionsHistory = null;
                try {
                    versionsHistory = VersionsEndpoint.getHistory();
    
                    if (versionsHistory.getBoolean("success")) {
                        versionsHistory.getJSONArray("versions").forEach(v -> {
                            if (((JSONObject)v).getBoolean("is_root")) {
                                updateRootVersion(rootVersion, (JSONObject)v);
                                versions.add(new HistoryTableItem(rootVersion));
                            } else {
                                versions.add(new HistoryTableItem(new VersionEntity(((JSONObject)v).put("files", new JSONArray()))));
                            }
                        });
                        disablingButtons.forEach(button -> {
                            button.setDisable(false);
                        });
                        checkoutButton.setDisable(false);
                    }
                } catch (IOException e) {
                    AlertWindow.showErrorWindow("Cannot load history");
                    e.printStackTrace();
                }
                return null;
            }
        };

        new Thread(task).start();
    }

    private void updateRootVersion(VersionEntity rootVersion, JSONObject version) {
        rootVersion = new VersionEntity(version.put("files", new JSONArray()));
    }

    private void updateTableContent(TableView<HistoryTableItem> table, VersionEntity rootVersion, List<Button> disablingButtons) {
        ObservableList<HistoryTableItem> versions = table.getItems();
        updateTableContent(versions, rootVersion, disablingButtons);
        table.setItems(versions);
    }

    public void setupEvents() {
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
}