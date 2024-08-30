package com.yujunyang.intellij.plugin.sonar.gui.settings;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.yujunyang.intellij.plugin.sonar.config.SonarQubeSettings;
import com.yujunyang.intellij.plugin.sonar.config.WorkspaceSettings;
import com.yujunyang.intellij.plugin.sonar.gui.common.UIUtils;
import com.yujunyang.intellij.plugin.sonar.gui.dialog.AddSonarPropertyDialog;
import com.yujunyang.intellij.plugin.sonar.resources.ResourcesLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractSettingsPanel<T extends JBPanel<T>> extends JBPanel<T> {
    JBTable propertiesTable;
    JBTable connectionsTable;

    DefaultTableModel propertiesTableModel;

    protected final Map<String, String> properties = new HashMap<>();
    protected final List<SonarQubeSettings> connections = new ArrayList<>();

    protected void addTableLabel(String text) {
        JLabel connectionsLabel = new JLabel(text);
        connectionsLabel.setAlignmentX(LEFT_ALIGNMENT);
        connectionsLabel.setBorder(JBUI.Borders.emptyBottom(5));
        add(connectionsLabel);
    }

    protected JBTable createTable(String emptyText, TableModel tableModel, ActionGroup actionGroup) {
        JBPanel<ProjectSettingsPanel> tablePanel = new JBPanel<>(new BorderLayout());
        tablePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        tablePanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 200));
        tablePanel.setBorder(JBUI.Borders.customLine(UIUtils.borderColor(), 1));
        tablePanel.setAlignmentX(LEFT_ALIGNMENT);
        add(tablePanel);

        JBScrollPane scrollPane = new JBScrollPane();
        scrollPane.setBorder(JBUI.Borders.customLine(UIUtils.borderColor(), 0, 0, 0, 1));
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        JBTable table = new JBTable();
        table.getEmptyText().setText(emptyText);
        table.setModel(tableModel);
        scrollPane.setViewportView(table);

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("left", actionGroup, false);
        actionToolbar.setTargetComponent(this);
        JComponent actionToolbarComponent = actionToolbar.getComponent();
        actionToolbarComponent.setBorder(JBUI.Borders.empty());
        tablePanel.add(actionToolbarComponent, BorderLayout.EAST);

        return table;
    }

    protected DefaultTableModel createDefaultTableModel(String[] columns) {
        DefaultTableModel tableModel = new DefaultTableModel(0, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableModel.setColumnIdentifiers(columns);
        return tableModel;
    }

    private void addSonarScannerProperty(Pair<String, String> property) {
        properties.put(property.first, property.second);
        propertiesTableModel.addRow(new Object[]{property.first, property.second});

    }

    private void updateSonarScannerProperty(int selectionIndex, Pair<String, String> property) {
        properties.replace(property.first, property.second);

        propertiesTableModel.setValueAt(property.second, selectionIndex, 1);
    }


    protected void initSonarProperties() {
        addTableLabel(ResourcesLoader.getString("settings.sonarScannerProperties.tableTitle"));

        propertiesTableModel = createDefaultTableModel(new String[]{"Name", "Value"});

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new AnAction(ResourcesLoader.getString("settings.action.add"), "", AllIcons.General.Add) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                AddSonarPropertyDialog addSonarPropertyDialog = new AddSonarPropertyDialog((property) -> addSonarScannerProperty(property));
                addSonarPropertyDialog.setExistNames(new ArrayList<>(properties.keySet()));
                addSonarPropertyDialog.show();
            }
        });
        actionGroup.add(new AnAction(ResourcesLoader.getString("settings.action.remove"), "", AllIcons.General.Remove) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(propertiesTable.getSelectionModel().getAnchorSelectionIndex() > -1);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                int selectionIndex = propertiesTable.getSelectionModel().getAnchorSelectionIndex();
                String name = propertiesTableModel.getValueAt(selectionIndex, 0).toString();
                propertiesTableModel.removeRow(selectionIndex);
                properties.remove(name);
                propertiesTableModel.fireTableDataChanged();
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        });
        actionGroup.add(new AnAction(ResourcesLoader.getString("settings.action.edit"), "", AllIcons.Actions.Edit) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(propertiesTable.getSelectionModel().getAnchorSelectionIndex() > -1);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                int selectionIndex = propertiesTable.getSelectionModel().getAnchorSelectionIndex();
                String name = propertiesTableModel.getValueAt(selectionIndex, 0).toString();
                String value = properties.get(name);
                AddSonarPropertyDialog addSonarPropertyDialog = new AddSonarPropertyDialog((property) -> updateSonarScannerProperty(selectionIndex, property));
                addSonarPropertyDialog.setExistNames(new ArrayList<>(properties.keySet()));
                addSonarPropertyDialog.initProperty(name, value);
                addSonarPropertyDialog.show();
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        });

        propertiesTable = createTable(ResourcesLoader.getString("settings.sonarScannerProperties.tableEmpty"), propertiesTableModel, actionGroup);
    }


    public Map<String, String> getProperties() {
        return properties;
    }

    public List<SonarQubeSettings> getConnections() {
        return connections;
    }

    public void reset() {
        properties.clear();
        connections.clear();

        int propertiesTableRowCount = propertiesTableModel.getRowCount();
        for (int i = propertiesTableRowCount - 1; i >= 0; i--) {
            propertiesTableModel.removeRow(i);
        }
        Map<String, String> existProperties = WorkspaceSettings.getInstance().sonarProperties;
        for (Map.Entry<String, String> item : existProperties.entrySet()) {
            properties.put(item.getKey(), item.getValue());
            propertiesTableModel.addRow(new Object[]{item.getKey(), item.getValue()});
        }

    }
}
