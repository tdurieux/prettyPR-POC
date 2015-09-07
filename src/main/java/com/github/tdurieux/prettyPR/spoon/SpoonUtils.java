package com.github.tdurieux.prettyPR.spoon;

import spoon.Launcher;
import spoon.compiler.SpoonCompiler;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.support.compiler.VirtualFile;
import spoon.support.compiler.jdt.JDTSnippetCompiler;

import java.util.*;

public class SpoonUtils {

    public static Map<String, CtType> stringToCTElement(Map<String, String> classes) {
        //javaElement = javaElement.replace("public class", "class");
        Factory factory = createFactory();
        factory.getEnvironment().setSourceClasspath(new String[]{});
        SpoonCompiler builder = new JDTSnippetCompiler(factory, "");
        builder.getInputSources().clear();
        final Set<String> fileNames = classes.keySet();
        for (Iterator<String> iterator = fileNames.iterator(); iterator.hasNext(); ) {
            String filename = iterator.next();
            final String classContent = classes.get(filename);
            if(classContent != null) {
                builder.addInputSource(new VirtualFile(classes.get(filename), filename));
            }
        }
        try {
            builder.build();
        } catch (Exception e) {
            // ignore compilation error
        }
        Map<String, CtType> output = new HashMap<String, CtType>();
        final List<CtType<?>> all = factory.Type().getAll();
        for (Iterator<String> iterator = fileNames.iterator(); iterator.hasNext(); ) {
            String fileName = iterator.next();
            for (int i = 0; i < all.size(); i++) {
                CtType<?> ctType = all.get(i);
                if(ctType.getPosition().getFile().getPath().contains(fileName)) {
                    output.put(fileName, ctType);
                }
            }
        }
        return output;
    }

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
        factory.getEnvironment().setPreserveLineNumbers(true);
        return factory;
    }
}
