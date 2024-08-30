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

import com.intellij.ide.DataManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.yujunyang.intellij.plugin.sonar.config.ProjectSettings;
import com.yujunyang.intellij.plugin.sonar.config.WorkspaceSettings;
import com.yujunyang.intellij.plugin.sonar.core.SeverityType;
import com.yujunyang.intellij.plugin.sonar.extensions.ApplicationSettingsConfigurable;
import com.yujunyang.intellij.plugin.sonar.resources.ResourcesLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

public class ProjectSettingsPanel extends AbstractSettingsPanel<ProjectSettingsPanel> {
    private final Project project;
    private ComboBox<String> connectionNameComboBox;
    private JBCheckBox inheritedFromApplicationCheckBox;
    private ComboBox<SeverityType> severityComboBox;

    public ProjectSettingsPanel(Project project) {
        this.project = project;
        init();
    }

    public String getConnectionName() {
        if (connectionNameComboBox.getSelectedItem() == null) {
            return null;
        }
        return connectionNameComboBox.getSelectedItem().toString();
    }

    public boolean isInheritedFromApplication() {
        return inheritedFromApplicationCheckBox.isSelected();
    }


    private void init() {
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(layout);

        initConnectionName();
        add(Box.createVerticalStrut(15));
        initSonarProperties();
        inheritedFromApplicationCheckBox = new JBCheckBox(ResourcesLoader.getString("settings.project.checkboxLabel"));
        inheritedFromApplicationCheckBox.setAlignmentX(LEFT_ALIGNMENT);
        add(inheritedFromApplicationCheckBox);
        // 新增 问题级别过滤配置
        add(Box.createVerticalStrut(15));
        initSeverityComboBox();

        // 这个不用主动调用，settings窗口打开时就会调用重写的reset方法，内部就是下面的reset
        // reset();
    }

    private void initSeverityComboBox() {
        JBPanel<ProjectSettingsPanel> panel = new JBPanel<>(new BorderLayout());
        panel.setAlignmentX(LEFT_ALIGNMENT);
        add(panel);
        panel.add(new JBLabel(ResourcesLoader.getString("settings.project.severityComboBoxLabel") + " "), BorderLayout.WEST);

        severityComboBox = new ComboBox<>(SeverityType.values());
        severityComboBox.setEditable(false);
        panel.add(severityComboBox, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
    }

    private void initConnectionName() {
        JBPanel<ProjectSettingsPanel> panel = new JBPanel<>(new BorderLayout());
        panel.setAlignmentX(LEFT_ALIGNMENT);
        add(panel);
        panel.add(new JBLabel(ResourcesLoader.getString("settings.project.connectionBindComboBoxLabel") + " "), BorderLayout.WEST);

        connectionNameComboBox = new ComboBox<>(WorkspaceSettings.getInstance().sonarQubeConnections.stream().map(n -> n.name).toArray(String[]::new));
        connectionNameComboBox.setEditable(false);
        panel.add(connectionNameComboBox, BorderLayout.CENTER);

        JButton button = new JButton(ResourcesLoader.getString("settings.project.goBackButton"));
        JComponent that = this;

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Settings allSettings = Settings.KEY.getData(DataManager.getInstance().getDataContext(that));
                if (allSettings != null) {
                    final ApplicationSettingsConfigurable globalConfigurable = allSettings.find(ApplicationSettingsConfigurable.class);
                    if (globalConfigurable != null) {
                        allSettings.select(globalConfigurable);
                    }
                } else {
                    ApplicationSettingsConfigurable globalConfigurable = new ApplicationSettingsConfigurable();
                    ShowSettingsUtil.getInstance().editConfigurable(that, globalConfigurable);
                }
            }
        });
        panel.add(button, BorderLayout.EAST);

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
    }


    public void reset() {
        super.reset();
        ProjectSettings projectSettings = ProjectSettings.getInstance(project);
        connectionNameComboBox.setSelectedItem(projectSettings.getSonarQubeConnectionName());
        severityComboBox.setSelectedItem(projectSettings.getSeverityType());
        inheritedFromApplicationCheckBox.setSelected(projectSettings.inheritedFromApplication);

        Map<String, String> existProperties = projectSettings.sonarProperties;
        for (Map.Entry<String, String> item : existProperties.entrySet()) {
            properties.put(item.getKey(), item.getValue());
            propertiesTableModel.addRow(new Object[]{item.getKey(), item.getValue()});
        }
    }

    public SeverityType getSeverityType() {
        Object selectedItem = severityComboBox.getSelectedItem();
        if (null == selectedItem) {
            return SeverityType.ANY;
        }
        return SeverityType.valueOf(selectedItem.toString());
    }
}
