package pl.mjedynak.idea.plugins.builder.writer.impl;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.CodeInsightUtil;
import com.intellij.codeInsight.generation.PsiElementClassMember;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiFieldImpl;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.lang.WordUtils;
import pl.mjedynak.idea.plugins.builder.writer.BuilderWriter;

import java.util.List;

public class BuilderWriterImpl implements BuilderWriter {
    @Override
    public void writeBuilder(final Project project, final List<PsiElementClassMember> classMembers, final PsiDirectory targetDirectory, final String className, final PsiClass psiClassFromEditor) {
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Computable<PsiElement>() {
                    public PsiElement compute() {
                        return createBuilder(project, classMembers, targetDirectory, className, psiClassFromEditor);
                    }
                });
            }
        }, "Create Builder", this);
    }

    private PsiElement createBuilder(final Project project, List<PsiElementClassMember> classMembers, PsiDirectory targetDirectory, final String className, PsiClass psiClassFromEditor) {
        try {
            IdeDocumentHistory.getInstance(project).includeCurrentPlaceAsChangePlace();
            PsiClass targetClass = getBuilderPsiClass(project, classMembers, targetDirectory, className, psiClassFromEditor);
            navigateToClassAndPositionCursor(project, targetClass);
            return targetClass;
        } catch (IncorrectOperationException e) {
            showErrorMessage(project, className);
            e.printStackTrace();
            return null;
        }
    }

    private PsiClass getBuilderPsiClass(Project project, List<PsiElementClassMember> classMembers, PsiDirectory targetDirectory, String className, PsiClass psiClassFromEditor) {
        PsiClass targetClass = JavaDirectoryService.getInstance().createClass(targetDirectory, className);
        PsiElementFactory psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();

        String srcClassName = psiClassFromEditor.getName();
        String srcClassFieldName = WordUtils.uncapitalize(srcClassName);

        PsiField srcClassNameField = psiElementFactory.createFieldFromText("private " + srcClassName + " " + srcClassFieldName + ";", psiClassFromEditor);
        targetClass.add(srcClassNameField);

        for (PsiElementClassMember classMember : classMembers) {
            targetClass.add(classMember.getPsiElement());
        }

        PsiMethod constructor = psiElementFactory.createConstructor();
        constructor.getModifierList().setModifierProperty("private", true);
        targetClass.add(constructor);

        PsiMethod staticMethod = psiElementFactory.createMethodFromText(
                "public static " + className + " a" + srcClassName + "() { return new " + className + "();}", psiClassFromEditor);
        targetClass.add(staticMethod);

        for (PsiElementClassMember classMember : classMembers) {
            PsiFieldImpl psiField = (PsiFieldImpl) classMember.getPsiElement();
            String fieldName = psiField.getName();
            String fieldType = psiField.getType().getPresentableText();
            String fieldNameUppercase = WordUtils.capitalize(fieldName);
            PsiMethod method = psiElementFactory.createMethodFromText(
                    "public " + className + " with" + fieldNameUppercase + "(" + fieldType + " " + fieldName + ") { this." + fieldName + " = " + fieldName + "; return this; }", psiField);
            targetClass.add(method);
        }

        StringBuilder buildMethodText = new StringBuilder();
        buildMethodText.append("public " + srcClassName + " build() { " + srcClassFieldName + " = new " + srcClassName + "();  ");
        for (PsiElementClassMember classMember : classMembers) {
            PsiFieldImpl psiField = (PsiFieldImpl) classMember.getPsiElement();
            String fieldName = psiField.getName();
            String fieldNameUppercase = WordUtils.capitalize(fieldName);
            buildMethodText.append(srcClassFieldName + ".set" + fieldNameUppercase + "(" + fieldName + ");");
        }
        buildMethodText.append("return " + srcClassFieldName + ";}");
        PsiMethod buildMethod = psiElementFactory.createMethodFromText(buildMethodText.toString(), psiClassFromEditor);
        targetClass.add(buildMethod);
        return targetClass;
    }

    private void navigateToClassAndPositionCursor(Project project, PsiClass targetClass) {
        CodeInsightUtil.positionCursor(project, targetClass.getContainingFile(), targetClass.getLBrace());
    }

    private void showErrorMessage(final Project project, final String className) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                Messages.showErrorDialog(project,
                        CodeInsightBundle.message("intention.error.cannot.create.class.message", className),
                        CodeInsightBundle.message("intention.error.cannot.create.class.title"));
            }
        });
    }
}