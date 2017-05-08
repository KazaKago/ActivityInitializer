package com.kazakago.activityfactory.generator;

import com.kazakago.activityfactory.FactoryParam;
import com.kazakago.activityfactory.constants.Annotations;
import com.kazakago.activityfactory.constants.IntentTypes;
import com.kazakago.activityfactory.constants.Types;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

/**
 * Activity factory class generator.
 * <p>
 * Created by KazaKago on 2017/03/16.
 */
public class ActivityFactoryGenerator extends CodeGenerator {

    public ActivityFactoryGenerator(ProcessingEnvironment processingEnv) {
        super(processingEnv);
    }

    @Override
    public void execute(Element element) throws IOException {
        String packageName = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
        ClassName modelClassName = ClassName.get(packageName, element.getSimpleName().toString());
        ClassName generatedClassName = ClassName.get(packageName, element.getSimpleName().toString() + "Factory");

        MethodSpec constructor = generateConstructor();
        MethodSpec createIntentMethod = generateCreateIntentMethod(element, modelClassName);
        MethodSpec injectArgumentMethod = generateInjectArgumentMethod(element, modelClassName);

        TypeSpec generatedClass = TypeSpec.classBuilder(generatedClassName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(constructor)
                .addMethod(createIntentMethod)
                .addMethod(injectArgumentMethod)
                .build();

        JavaFile.builder(packageName, generatedClass)
                .build()
                .writeTo(processingEnv.getFiler());
    }

    private MethodSpec generateConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build();
    }

    private MethodSpec generateCreateIntentMethod(Element element, ClassName modelClassName) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("createIntent")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .addAnnotation(Annotations.NonNull)
                .addParameter(ParameterSpec.builder(Types.Context, "context")
                        .addAnnotation(Annotations.NonNull)
                        .build())
                .addStatement("$T intent = new $T(context, $L.class)", Types.Intent, Types.Intent, modelClassName.simpleName());
        for (Element el : element.getEnclosedElements()) {
            if (el.getAnnotation(FactoryParam.class) != null) {
                IntentTypes intentType = IntentTypes.resolve(processingEnv,  el.asType());
                if (intentType != null) {
                    TypeName fieldType = TypeName.get(el.asType());
                    String fieldName = el.getSimpleName().toString();
                    ParameterSpec.Builder paramBuilder = ParameterSpec.builder(fieldType, fieldName);
                    if (!fieldType.isPrimitive()) {
                        paramBuilder.addAnnotation(Annotations.NonNull);
                    }
                    methodBuilder.addParameter(paramBuilder.build())
                            .addStatement("intent.$L($S, $L)", intentType.putMethodName, fieldName, fieldName);
                }
            }
        }
        return methodBuilder.addStatement("return intent")
                .returns(Types.Intent)
                .build();
    }

    private MethodSpec generateInjectArgumentMethod(Element element, ClassName modelClassName) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("injectArgument")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .addParameter(ParameterSpec.builder(modelClassName, "activity")
                        .addAnnotation(Annotations.NonNull)
                        .build())
                .addStatement("Intent intent = activity.getIntent()");
        for (Element el : element.getEnclosedElements()) {
            if (el.getAnnotation(FactoryParam.class) != null) {
                IntentTypes intentType = IntentTypes.resolve(processingEnv,  el.asType());
                if (intentType != null) {
                    TypeName fieldType = TypeName.get(el.asType());
                    String fieldName = el.getSimpleName().toString();
                    if (intentType.getDefaultValue != null) {
                        methodBuilder.addStatement("activity.$L = ($T) intent.$L($S, $L)", fieldName, fieldType, intentType.getMethodName, fieldName, intentType.getDefaultValue);
                    } else {
                        methodBuilder.addStatement("activity.$L = ($T) intent.$L($S)", fieldName, fieldType, intentType.getMethodName, fieldName);
                    }
                }
            }
        }
        return methodBuilder.build();
    }

}