/*
 * Copyright 2021 Yu Junyang
 * https://github.com/lowkeyfish
 *
 * This file is part of Sonar Intellij plugin.
 *
 * Sonar Intellij plugin is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Sonar Intellij plugin is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sonar Intellij plugin.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.yujunyang.intellij.plugin.sonar.gui.settings;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.yujunyang.intellij.plugin.sonar.config.SonarQubeSettings;
import com.yujunyang.intellij.plugin.sonar.config.WorkspaceSettings;
import com.yujunyang.intellij.plugin.sonar.gui.common.UIUtils;
import com.yujunyang.intellij.plugin.sonar.gui.dialog.AddSonarQubeConnectionDialog;
import com.yujunyang.intellij.plugin.sonar.resources.ResourcesLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationSettingsPanel extends AbstractSettingsPanel<ApplicationSettingsPanel> {

    private DefaultTableModel connectionsTableModel;
    private ComboBox<String> uiLanguagesComboBox;

    public ApplicationSettingsPanel() {
        init();
    }

    @Nullable
    public String getUILanguageLocale() {
        Object selectedItem = uiLanguagesComboBox.getSelectedItem();
        if (null == selectedItem) {
            return null;
        }
        return UIUtils.getLocaleByLanguageDesc(String.valueOf(selectedItem));
    }

    private void init() {
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(layout);

        initUILanguages();
        add(Box.createVerticalStrut(15));
        initConnections();
        add(Box.createVerticalStrut(15));
        initSonarProperties();

        // 这个不用主动调用，settings窗口打开时就会调用重写的reset方法，内部就是下面的reset
        // reset();
    }

    private void initUILanguages() {
        JBPanel<ApplicationSettingsPanel> panel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        panel.add(new JBLabel(ResourcesLoader.getString("settings.uiLanguages.label") + " "));

        uiLanguagesComboBox = new ComboBox<>();
        panel.add(uiLanguagesComboBox);
        uiLanguagesComboBox.setEditable(false);
        UIUtils.getAllUILanguagesDesc().forEach(n -> uiLanguagesComboBox.addItem(n));
        uiLanguagesComboBox.setSelectedItem(UIUtils.getLanguageDescByLocale(WorkspaceSettings.getInstance().uiLanguageLocale));

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));

        add(panel);
    }

    private void initConnections() {
        addTableLabel(ResourcesLoader.getString("settings.sonarQubeConnections.tableTitle"));

        connectionsTableModel = createDefaultTableModel(new String[]{"Name", "Url"});


        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new AnAction(ResourcesLoader.getString("settings.action.add"), "", AllIcons.General.Add) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                AddSonarQubeConnectionDialog addSonarQubeConnectionDialog = new AddSonarQubeConnectionDialog((sonarQubeSettings) -> addSonarQubeConnection(sonarQubeSettings));
                addSonarQubeConnectionDialog.setExistNames(connections.stream().map(n -> n.name).collect(Collectors.toList()));
                addSonarQubeConnectionDialog.show();
            }
        });
        actionGroup.add(new AnAction(ResourcesLoader.getString("settings.action.remove"), "", AllIcons.General.Remove) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(connectionsTable.getSelectionModel().getAnchorSelectionIndex() > -1);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                int selectionIndex = connectionsTable.getSelectionModel().getAnchorSelectionIndex();
                connectionsTableModel.removeRow(selectionIndex);
                connections.remove(selectionIndex);
                connectionsTableModel.fireTableDataChanged();
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        });
        actionGroup.add(new AnAction(ResourcesLoader.getString("settings.action.edit"), "", AllIcons.Actions.Edit) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(connectionsTable.getSelectionModel().getAnchorSelectionIndex() > -1);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                int selectionIndex = connectionsTable.getSelectionModel().getAnchorSelectionIndex();
                SonarQubeSettings selectedSonarQubeSettings = connections.get(selectionIndex);
                AddSonarQubeConnectionDialog addSonarQubeConnectionDialog = new AddSonarQubeConnectionDialog((sonarQubeSettings) -> updateSonarQubeConnection(selectionIndex, sonarQubeSettings));
                addSonarQubeConnectionDialog.setExistNames(connections.stream().map(n -> n.name).collect(Collectors.toList()));
                addSonarQubeConnectionDialog.initConnection(selectedSonarQubeSettings.name, selectedSonarQubeSettings.url, selectedSonarQubeSettings.token);
                addSonarQubeConnectionDialog.show();
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        });

        connectionsTable = createTable(ResourcesLoader.getString("settings.sonarQubeConnections.tableEmpty"), connectionsTableModel, actionGroup);
    }

    private void addSonarQubeConnection(SonarQubeSettings sonarQubeSettings) {
        connections.add(sonarQubeSettings);
        connectionsTableModel.addRow(new Object[]{sonarQubeSettings.name, sonarQubeSettings.url});
    }

    private void updateSonarQubeConnection(int selectionIndex, SonarQubeSettings sonarQubeSettings) {
        SonarQubeSettings original = connections.get(selectionIndex);
        original.name = sonarQubeSettings.name;
        original.url = sonarQubeSettings.url;
        original.token = sonarQubeSettings.token;

        connectionsTableModel.setValueAt(sonarQubeSettings.url, selectionIndex, 1);
    }

    public void reset() {
        uiLanguagesComboBox.setSelectedItem(UIUtils.getLanguageDescByLocale(WorkspaceSettings.getInstance().uiLanguageLocale));
        super.reset();
        int connectionsTableRowCount = connectionsTableModel.getRowCount();
        for (int i = connectionsTableRowCount - 1; i >= 0; i--) {
            connectionsTableModel.removeRow(i);
        }
        WorkspaceSettings workspaceSettings = WorkspaceSettings.getInstance();
        Set<SonarQubeSettings> existConnections = workspaceSettings.sonarQubeConnections;
        existConnections.forEach(c -> {
            SonarQubeSettings sqs = new SonarQubeSettings();
            {
                sqs.name = c.name;
                sqs.url = c.url;
                sqs.token = c.token;
            }
            connections.add(sqs);
            connectionsTableModel.addRow(new Object[]{c.name, c.url});
        });
    }
}
