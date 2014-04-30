/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.completion;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.util.regex.Pattern;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.test.InnerTestClasses;
import org.jetbrains.jet.test.TestMetadata;

import org.jetbrains.jet.completion.AbstractJvmSmartCompletionTest;

/** This class is generated by {@link org.jetbrains.jet.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("idea/testData/completion/smart")
public class JvmSmartCompletionTestGenerated extends AbstractJvmSmartCompletionTest {
    public void testAllFilesPresentInSmart() throws Exception {
        JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), "org.jetbrains.jet.generators.tests.TestsPackage", new File("idea/testData/completion/smart"), Pattern.compile("^(.+)\\.kt$"), true);
    }
    
    @TestMetadata("AnonymousObject1.kt")
    public void testAnonymousObject1() throws Exception {
        doTest("idea/testData/completion/smart/AnonymousObject1.kt");
    }
    
    @TestMetadata("AnonymousObject2.kt")
    public void testAnonymousObject2() throws Exception {
        doTest("idea/testData/completion/smart/AnonymousObject2.kt");
    }
    
    @TestMetadata("AnonymousObjectForJavaInterface.kt")
    public void testAnonymousObjectForJavaInterface() throws Exception {
        doTest("idea/testData/completion/smart/AnonymousObjectForJavaInterface.kt");
    }
    
    @TestMetadata("AnyExpected.kt")
    public void testAnyExpected() throws Exception {
        doTest("idea/testData/completion/smart/AnyExpected.kt");
    }
    
    @TestMetadata("AutoCastedType.kt")
    public void testAutoCastedType() throws Exception {
        doTest("idea/testData/completion/smart/AutoCastedType.kt");
    }
    
    @TestMetadata("AutoCastedTypeWithQualifier.kt")
    public void testAutoCastedTypeWithQualifier() throws Exception {
        doTest("idea/testData/completion/smart/AutoCastedTypeWithQualifier.kt");
    }
    
    @TestMetadata("AutoNotNullType.kt")
    public void testAutoNotNullType() throws Exception {
        doTest("idea/testData/completion/smart/AutoNotNullType.kt");
    }
    
    @TestMetadata("AutoNotNullType2.kt")
    public void testAutoNotNullType2() throws Exception {
        doTest("idea/testData/completion/smart/AutoNotNullType2.kt");
    }
    
    @TestMetadata("AutoNotNullTypeForConstructorParameter.kt")
    public void testAutoNotNullTypeForConstructorParameter() throws Exception {
        doTest("idea/testData/completion/smart/AutoNotNullTypeForConstructorParameter.kt");
    }
    
    @TestMetadata("AutoNotNullTypeWithQualifier.kt")
    public void testAutoNotNullTypeWithQualifier() throws Exception {
        doTest("idea/testData/completion/smart/AutoNotNullTypeWithQualifier.kt");
    }
    
    @TestMetadata("BeforeArgumentWithBinaryOperation.kt")
    public void testBeforeArgumentWithBinaryOperation() throws Exception {
        doTest("idea/testData/completion/smart/BeforeArgumentWithBinaryOperation.kt");
    }
    
    @TestMetadata("BeforeArgumentWithBinaryOperation2.kt")
    public void testBeforeArgumentWithBinaryOperation2() throws Exception {
        doTest("idea/testData/completion/smart/BeforeArgumentWithBinaryOperation2.kt");
    }
    
    @TestMetadata("BeforeArgumentWithBinaryOperation3.kt")
    public void testBeforeArgumentWithBinaryOperation3() throws Exception {
        doTest("idea/testData/completion/smart/BeforeArgumentWithBinaryOperation3.kt");
    }
    
    @TestMetadata("BooleanExpected.kt")
    public void testBooleanExpected() throws Exception {
        doTest("idea/testData/completion/smart/BooleanExpected.kt");
    }
    
    @TestMetadata("ChainedCall.kt")
    public void testChainedCall() throws Exception {
        doTest("idea/testData/completion/smart/ChainedCall.kt");
    }
    
    @TestMetadata("ClassObjectMembers.kt")
    public void testClassObjectMembers() throws Exception {
        doTest("idea/testData/completion/smart/ClassObjectMembers.kt");
    }
    
    @TestMetadata("ClassObjectMembersForNullable.kt")
    public void testClassObjectMembersForNullable() throws Exception {
        doTest("idea/testData/completion/smart/ClassObjectMembersForNullable.kt");
    }
    
    @TestMetadata("Constructor.kt")
    public void testConstructor() throws Exception {
        doTest("idea/testData/completion/smart/Constructor.kt");
    }
    
    @TestMetadata("ConstructorForGenericType.kt")
    public void testConstructorForGenericType() throws Exception {
        doTest("idea/testData/completion/smart/ConstructorForGenericType.kt");
    }
    
    @TestMetadata("ConstructorForJavaClass.kt")
    public void testConstructorForJavaClass() throws Exception {
        doTest("idea/testData/completion/smart/ConstructorForJavaClass.kt");
    }
    
    @TestMetadata("ConstructorForNullable.kt")
    public void testConstructorForNullable() throws Exception {
        doTest("idea/testData/completion/smart/ConstructorForNullable.kt");
    }
    
    @TestMetadata("EmptyPrefix.kt")
    public void testEmptyPrefix() throws Exception {
        doTest("idea/testData/completion/smart/EmptyPrefix.kt");
    }
    
    @TestMetadata("EnumMembers.kt")
    public void testEnumMembers() throws Exception {
        doTest("idea/testData/completion/smart/EnumMembers.kt");
    }
    
    @TestMetadata("EqOperator.kt")
    public void testEqOperator() throws Exception {
        doTest("idea/testData/completion/smart/EqOperator.kt");
    }
    
    @TestMetadata("FunctionReference1.kt")
    public void testFunctionReference1() throws Exception {
        doTest("idea/testData/completion/smart/FunctionReference1.kt");
    }
    
    @TestMetadata("FunctionReference3.kt")
    public void testFunctionReference3() throws Exception {
        doTest("idea/testData/completion/smart/FunctionReference3.kt");
    }
    
    @TestMetadata("FunctionReference4.kt")
    public void testFunctionReference4() throws Exception {
        doTest("idea/testData/completion/smart/FunctionReference4.kt");
    }
    
    @TestMetadata("FunctionReference5.kt")
    public void testFunctionReference5() throws Exception {
        doTest("idea/testData/completion/smart/FunctionReference5.kt");
    }
    
    @TestMetadata("FunctionReference6.kt")
    public void testFunctionReference6() throws Exception {
        doTest("idea/testData/completion/smart/FunctionReference6.kt");
    }
    
    @TestMetadata("FunctionReference7.kt")
    public void testFunctionReference7() throws Exception {
        doTest("idea/testData/completion/smart/FunctionReference7.kt");
    }
    
    @TestMetadata("FunctionReference9.kt")
    public void testFunctionReference9() throws Exception {
        doTest("idea/testData/completion/smart/FunctionReference9.kt");
    }
    
    @TestMetadata("GenericMethodArgument.kt")
    public void testGenericMethodArgument() throws Exception {
        doTest("idea/testData/completion/smart/GenericMethodArgument.kt");
    }
    
    @TestMetadata("IfCondition.kt")
    public void testIfCondition() throws Exception {
        doTest("idea/testData/completion/smart/IfCondition.kt");
    }
    
    @TestMetadata("IfValue1.kt")
    public void testIfValue1() throws Exception {
        doTest("idea/testData/completion/smart/IfValue1.kt");
    }
    
    @TestMetadata("IfValue2.kt")
    public void testIfValue2() throws Exception {
        doTest("idea/testData/completion/smart/IfValue2.kt");
    }
    
    @TestMetadata("IfValue3.kt")
    public void testIfValue3() throws Exception {
        doTest("idea/testData/completion/smart/IfValue3.kt");
    }
    
    @TestMetadata("InaccessibleConstructor.kt")
    public void testInaccessibleConstructor() throws Exception {
        doTest("idea/testData/completion/smart/InaccessibleConstructor.kt");
    }
    
    @TestMetadata("InsideIdentifier.kt")
    public void testInsideIdentifier() throws Exception {
        doTest("idea/testData/completion/smart/InsideIdentifier.kt");
    }
    
    @TestMetadata("JavaEnumMembers.kt")
    public void testJavaEnumMembers() throws Exception {
        doTest("idea/testData/completion/smart/JavaEnumMembers.kt");
    }
    
    @TestMetadata("JavaEnumMembersAfterQualifier.kt")
    public void testJavaEnumMembersAfterQualifier() throws Exception {
        doTest("idea/testData/completion/smart/JavaEnumMembersAfterQualifier.kt");
    }
    
    @TestMetadata("JavaEnumMembersForNullable.kt")
    public void testJavaEnumMembersForNullable() throws Exception {
        doTest("idea/testData/completion/smart/JavaEnumMembersForNullable.kt");
    }
    
    @TestMetadata("JavaStaticFields.kt")
    public void testJavaStaticFields() throws Exception {
        doTest("idea/testData/completion/smart/JavaStaticFields.kt");
    }
    
    @TestMetadata("JavaStaticFieldsForNullable.kt")
    public void testJavaStaticFieldsForNullable() throws Exception {
        doTest("idea/testData/completion/smart/JavaStaticFieldsForNullable.kt");
    }
    
    @TestMetadata("JavaStaticMethods.kt")
    public void testJavaStaticMethods() throws Exception {
        doTest("idea/testData/completion/smart/JavaStaticMethods.kt");
    }
    
    @TestMetadata("Lambda1.kt")
    public void testLambda1() throws Exception {
        doTest("idea/testData/completion/smart/Lambda1.kt");
    }
    
    @TestMetadata("Lambda2.kt")
    public void testLambda2() throws Exception {
        doTest("idea/testData/completion/smart/Lambda2.kt");
    }
    
    @TestMetadata("Lambda3.kt")
    public void testLambda3() throws Exception {
        doTest("idea/testData/completion/smart/Lambda3.kt");
    }
    
    @TestMetadata("Lambda4.kt")
    public void testLambda4() throws Exception {
        doTest("idea/testData/completion/smart/Lambda4.kt");
    }
    
    @TestMetadata("Lambda5.kt")
    public void testLambda5() throws Exception {
        doTest("idea/testData/completion/smart/Lambda5.kt");
    }
    
    @TestMetadata("MethodCallArgument.kt")
    public void testMethodCallArgument() throws Exception {
        doTest("idea/testData/completion/smart/MethodCallArgument.kt");
    }
    
    @TestMetadata("NoConstructorWithQualifier.kt")
    public void testNoConstructorWithQualifier() throws Exception {
        doTest("idea/testData/completion/smart/NoConstructorWithQualifier.kt");
    }
    
    @TestMetadata("NoFunctionReferenceAfterQualifier.kt")
    public void testNoFunctionReferenceAfterQualifier() throws Exception {
        doTest("idea/testData/completion/smart/NoFunctionReferenceAfterQualifier.kt");
    }
    
    @TestMetadata("NoNothing.kt")
    public void testNoNothing() throws Exception {
        doTest("idea/testData/completion/smart/NoNothing.kt");
    }
    
    @TestMetadata("NoSillyAssignment.kt")
    public void testNoSillyAssignment() throws Exception {
        doTest("idea/testData/completion/smart/NoSillyAssignment.kt");
    }
    
    @TestMetadata("NotEqOperator.kt")
    public void testNotEqOperator() throws Exception {
        doTest("idea/testData/completion/smart/NotEqOperator.kt");
    }
    
    @TestMetadata("NotSillyAssignment.kt")
    public void testNotSillyAssignment() throws Exception {
        doTest("idea/testData/completion/smart/NotSillyAssignment.kt");
    }
    
    @TestMetadata("Null.kt")
    public void testNull() throws Exception {
        doTest("idea/testData/completion/smart/Null.kt");
    }
    
    @TestMetadata("NullableThis.kt")
    public void testNullableThis() throws Exception {
        doTest("idea/testData/completion/smart/NullableThis.kt");
    }
    
    @TestMetadata("OverloadedConstructorArgument.kt")
    public void testOverloadedConstructorArgument() throws Exception {
        doTest("idea/testData/completion/smart/OverloadedConstructorArgument.kt");
    }
    
    @TestMetadata("OverloadedMethodCallArgument1.kt")
    public void testOverloadedMethodCallArgument1() throws Exception {
        doTest("idea/testData/completion/smart/OverloadedMethodCallArgument1.kt");
    }
    
    @TestMetadata("OverloadedMethodCallArgument2.kt")
    public void testOverloadedMethodCallArgument2() throws Exception {
        doTest("idea/testData/completion/smart/OverloadedMethodCallArgument2.kt");
    }
    
    @TestMetadata("OverloadedMethodCallArgument3.kt")
    public void testOverloadedMethodCallArgument3() throws Exception {
        doTest("idea/testData/completion/smart/OverloadedMethodCallArgument3.kt");
    }
    
    @TestMetadata("PrivateConstructorForAbstract.kt")
    public void testPrivateConstructorForAbstract() throws Exception {
        doTest("idea/testData/completion/smart/PrivateConstructorForAbstract.kt");
    }
    
    @TestMetadata("ProtectedConstructorForAbstract.kt")
    public void testProtectedConstructorForAbstract() throws Exception {
        doTest("idea/testData/completion/smart/ProtectedConstructorForAbstract.kt");
    }
    
    @TestMetadata("QualifiedOverloadedMethodCallArgument1.kt")
    public void testQualifiedOverloadedMethodCallArgument1() throws Exception {
        doTest("idea/testData/completion/smart/QualifiedOverloadedMethodCallArgument1.kt");
    }
    
    @TestMetadata("QualifiedOverloadedMethodCallArgument2.kt")
    public void testQualifiedOverloadedMethodCallArgument2() throws Exception {
        doTest("idea/testData/completion/smart/QualifiedOverloadedMethodCallArgument2.kt");
    }
    
    @TestMetadata("QualifiedThis.kt")
    public void testQualifiedThis() throws Exception {
        doTest("idea/testData/completion/smart/QualifiedThis.kt");
    }
    
    @TestMetadata("QualifiedThisOfAnonymousObject.kt")
    public void testQualifiedThisOfAnonymousObject() throws Exception {
        doTest("idea/testData/completion/smart/QualifiedThisOfAnonymousObject.kt");
    }
    
    @TestMetadata("QualifiedThisOfExtensionFunction.kt")
    public void testQualifiedThisOfExtensionFunction() throws Exception {
        doTest("idea/testData/completion/smart/QualifiedThisOfExtensionFunction.kt");
    }
    
    @TestMetadata("QualifiedThisOfExtensionLambda1.kt")
    public void testQualifiedThisOfExtensionLambda1() throws Exception {
        doTest("idea/testData/completion/smart/QualifiedThisOfExtensionLambda1.kt");
    }
    
    @TestMetadata("QualifiedThisOfExtensionLambda2.kt")
    public void testQualifiedThisOfExtensionLambda2() throws Exception {
        doTest("idea/testData/completion/smart/QualifiedThisOfExtensionLambda2.kt");
    }
    
    @TestMetadata("QualifiedThisOfExtensionLambda3.kt")
    public void testQualifiedThisOfExtensionLambda3() throws Exception {
        doTest("idea/testData/completion/smart/QualifiedThisOfExtensionLambda3.kt");
    }
    
    @TestMetadata("SAMExpected1.kt")
    public void testSAMExpected1() throws Exception {
        doTest("idea/testData/completion/smart/SAMExpected1.kt");
    }
    
    @TestMetadata("SkipUnresolvedTypes.kt")
    public void testSkipUnresolvedTypes() throws Exception {
        doTest("idea/testData/completion/smart/SkipUnresolvedTypes.kt");
    }
    
    @TestMetadata("This.kt")
    public void testThis() throws Exception {
        doTest("idea/testData/completion/smart/This.kt");
    }
    
    @TestMetadata("UnresolvedExpectedType.kt")
    public void testUnresolvedExpectedType() throws Exception {
        doTest("idea/testData/completion/smart/UnresolvedExpectedType.kt");
    }
    
    @TestMetadata("VariableInitializer.kt")
    public void testVariableInitializer() throws Exception {
        doTest("idea/testData/completion/smart/VariableInitializer.kt");
    }
    
    @TestMetadata("WithPrefix.kt")
    public void testWithPrefix() throws Exception {
        doTest("idea/testData/completion/smart/WithPrefix.kt");
    }
    
    @TestMetadata("WithQualifier.kt")
    public void testWithQualifier() throws Exception {
        doTest("idea/testData/completion/smart/WithQualifier.kt");
    }
    
}
