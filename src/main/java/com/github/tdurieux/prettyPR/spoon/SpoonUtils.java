package com.github.tdurieux.prettyPR.spoon;

import spoon.Launcher;
import spoon.compiler.SpoonCompiler;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.support.compiler.jdt.JDTSnippetCompiler;

/**
 * Created by thomas on 3/09/15.
 */
public class SpoonUtils {

    public static CtType<?> stringToCTElement(String javaElement) {
        //javaElement = javaElement.replace("public class", "class");
        Factory factory = createFactory();
        factory.getEnvironment().setSourceClasspath(new String[]{});
        SpoonCompiler builder = new JDTSnippetCompiler(factory, javaElement);
        try {
            builder.build();
        } catch (Exception e) {
            // ignore compilation error
        }
        return factory.Type().getAll().get(0);
    }

    private static Factory createFactory() {
        Factory factory = new Launcher().getFactory();
        factory.getEnvironment().setAutoImports(true);
        factory.getEnvironment().setNoClasspath(true);
        return factory;
    }
}
