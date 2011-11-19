package pl.mjedynak.idea.plugins.builder.psi;

import com.intellij.codeInsight.generation.PsiElementClassMember;
import com.intellij.psi.PsiField;

import java.util.List;

public interface PsiFieldSelector {

    List<PsiElementClassMember> selectFieldsToIncludeInBuilder(List<PsiField> psiFields);
}
