package pl.mjedynak.idea.plugins.builder.psi;

import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;

import java.util.List;

public class PsiFieldsModifier {

    static final String FINAL = "final";

    public void modifyFields(List<PsiField> psiFieldsForSetters, List<PsiField> psiFieldsForConstructor,
                             PsiClass builderClass, PsiElementFactory elementFactory) {
        for (PsiField psiFieldsForSetter : psiFieldsForSetters) {
            removeModifiersAndInitialize(psiFieldsForSetter, builderClass, elementFactory);
        }
        for (PsiField psiFieldForConstructor : psiFieldsForConstructor) {
            removeModifiersAndInitialize(psiFieldForConstructor, builderClass, elementFactory);
        }
    }

    public void modifyFieldsForInnerClass(List<PsiField> allFileds, PsiClass innerBuilderClass, PsiElementFactory elementFactory) {
        for (PsiField field : allFileds) {
            removeModifiersAndInitialize(field, innerBuilderClass, elementFactory);
        }
    }

    private void removeModifiersAndInitialize(PsiField psiField, PsiClass builderClass, PsiElementFactory elementFactory) {
        PsiElement copy = psiField.copy();
        removeAnnotationsFromElement(copy);
        removeFinalModifierFromElement(copy);
        removeComments(copy);
        addInitialization(copy, elementFactory);
        builderClass.add(copy);
    }

    private void addInitialization(PsiElement psiElement, PsiElementFactory elementFactory) {
        if (psiElement instanceof PsiField) {
            PsiField psiField = ((PsiField) psiElement);

            PsiExpression psiInitializer = null;

            if (PsiType.INT.equals(psiField.getType()) || "Integer".equals(psiField.getType().getPresentableText())
                    || PsiType.SHORT.equals(psiField.getType()) || "Short".equals(psiField.getType().getPresentableText())
                    || PsiType.BYTE.equals(psiField.getType()) || "Byte".equals(psiField.getType().getPresentableText())) {
                psiInitializer = elementFactory.createExpressionFromText("1", psiField);
            } else if (PsiType.LONG.equals(psiField.getType()) || "Long".equals(psiField.getType().getPresentableText())) {
                psiInitializer = elementFactory.createExpressionFromText("1L", psiField);
            } else if (PsiType.BOOLEAN.equals(psiField.getType()) || "Boolean".equals(psiField.getType().getPresentableText())) {
                psiInitializer = elementFactory.createExpressionFromText("false", psiField);
            } else if (PsiType.FLOAT.equals(psiField.getType()) || "Float".equals(psiField.getType().getPresentableText())) {
                psiInitializer = elementFactory.createExpressionFromText("2.4f", psiField);
            } else if (PsiType.DOUBLE.equals(psiField.getType()) || "Double".equals(psiField.getType().getPresentableText())) {
                psiInitializer = elementFactory.createExpressionFromText("1.4d", psiField);
            } else if ("String".equals(psiField.getType().getPresentableText())) {
                psiInitializer = elementFactory.createExpressionFromText("RandomStringUtils.randomAlphabetic(5)", psiField);
            } else if ("Date".equals(psiField.getType().getPresentableText())) {
                psiInitializer = elementFactory.createExpressionFromText("new Date()", psiField);
            } else if ("LocalDateTime".equals(psiField.getType().getPresentableText())) {
                psiInitializer = elementFactory.createExpressionFromText("LocalDateTime.now()", psiField);
            } else if ("UUID".equals(psiField.getType().getPresentableText())) {
                psiInitializer = elementFactory.createExpressionFromText("UUID.randomUUID()", psiField);
            } else if (psiField.getType().getPresentableText().startsWith("Set")) {
                psiInitializer = elementFactory.createExpressionFromText("new HashSet<>()", psiField);
            } else if (psiField.getType().getPresentableText().startsWith("List")) {
                psiInitializer = elementFactory.createExpressionFromText("new ArrayList<>()", psiField);
            } else if (psiField.getType().getPresentableText().startsWith("Map")) {
                psiInitializer = elementFactory.createExpressionFromText("new HashMap<>()", psiField);
            } else if (psiField.getType().getPresentableText().contains("[]")) {
                String presentableText = psiField.getType().getPresentableText();
                psiInitializer = elementFactory.createExpressionFromText(
                        String.format("new %s", presentableText.replace("[]", "[5]")), psiField);
            }

            if (psiInitializer != null) {
                psiField.setInitializer(psiInitializer);
            }
        }
    }

    private void removeComments(PsiElement psiElement) {
        if (psiElement instanceof PsiField) {
            PsiDocComment docComment = ((PsiField) psiElement).getDocComment();
            if (docComment != null) {
                docComment.delete();
            }
        }
    }

    private void removeFinalModifierFromElement(PsiElement psiElement) {
        if (psiElement instanceof PsiField) {
            PsiModifierList modifierList = ((PsiField) psiElement).getModifierList();
            if (modifierList != null && modifierList.hasExplicitModifier(FINAL)) {
                modifierList.setModifierProperty(FINAL, false);
            }
        }
    }

    private void removeAnnotationsFromElement(PsiElement psiElement) {
        if (psiElement instanceof PsiField) {
            PsiModifierList modifierList = ((PsiField) psiElement).getModifierList();
            if (modifierList != null) {
                deleteAnnotationsFromModifierList(modifierList);
            }
        }
    }

    private void deleteAnnotationsFromModifierList(PsiModifierList modifierList) {
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            annotation.delete();
        }
    }
}
