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

package com.yujunyang.intellij.plugin.sonar.extensions;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.yujunyang.intellij.plugin.sonar.config.ProjectSettings;
import com.yujunyang.intellij.plugin.sonar.core.AbstractIssue;
import com.yujunyang.intellij.plugin.sonar.core.AnalyzeState;
import com.yujunyang.intellij.plugin.sonar.core.SeverityType;
import com.yujunyang.intellij.plugin.sonar.service.ProblemCacheService;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class SonarIssueExternalAnnotator extends ExternalAnnotator<SonarIssueExternalAnnotator.AnnotationContext, SonarIssueExternalAnnotator.AnnotationContext> {

    @Override
    public void apply(@NotNull PsiFile file, AnnotationContext annotationResult, @NotNull AnnotationHolder holder) {
        final Project project = file.getProject();

        if (!AnalyzeState.get(project).isIdle()) {
            return;
        }

        final ProblemCacheService cacheService = ProblemCacheService.getInstance(project);
        final Map<PsiFile, List<AbstractIssue>> issues = cacheService.getIssues();

        if (issues.containsKey(file)) {
            addAnnotation(project, file, issues.get(file), holder);
        }

    }

    @Override
    public AnnotationContext collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        return collectInformation(file);
    }

    @Override
    public AnnotationContext collectInformation(@NotNull PsiFile file) {
        return new AnnotationContext();
    }

    @Override
    @Nullable
    public AnnotationContext doAnnotate(AnnotationContext collectedInfo) {
        return collectedInfo;
    }

    private static void addAnnotation(Project project, PsiFile psiFile, List<AbstractIssue> issues, AnnotationHolder holder) {
        final ProjectSettings settings = ProjectSettings.getInstance(project);
        issues.stream()
                // 筛选重要的问题 避免过多的代码异味提示干扰重要的修复
                .filter(issue -> settings.isEnabledSeverity(issue.getSeverity())
                        || issue.getType().equals("BUG")
                        || issue.getType().equals("VULNERABILITY")
                        || issue.getType().equals("SECURITY_HOTSPOT")
                )
                .forEach(issue -> {
                    switch (SeverityType.fromName(issue.getSeverity())) {
                        case BLOCKER:
                        case CRITICAL:
                            holder.newAnnotation(HighlightSeverity.ERROR, msg(issue)).range(issue.getTextRange()).create();
                            break;
                        case MAJOR:
                            holder.newAnnotation(HighlightSeverity.WARNING, msg(issue)).range(issue.getTextRange()).create();
                            break;
                        case MINOR:
                            holder.newAnnotation(HighlightSeverity.WEAK_WARNING, msg(issue)).range(issue.getTextRange()).create();
                            break;
                        case INFO:
                            holder.newAnnotation(HighlightSeverity.INFORMATION, msg(issue)).range(issue.getTextRange()).create();
                            break;
                    }
                });

    }

    private static String msg(AbstractIssue issue) {
        return String.format("SonarAnalyzer: %s [%s]", issue.getMsg(), issue.getTypeDesc());
    }


    public static class AnnotationContext {
    }
}
