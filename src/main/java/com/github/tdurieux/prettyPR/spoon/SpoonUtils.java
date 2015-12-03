package com.github.tdurieux.prettyPR.spoon;

import spoon.Launcher;
import spoon.OutputType;
import spoon.compiler.SpoonCompiler;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.support.compiler.VirtualFile;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;
import spoon.support.compiler.jdt.JDTSnippetCompiler;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SpoonUtils {

    public static Map<String, CtType> stringToCTElement(Map<String, String> classes) {
        //javaElement = javaElement.replace("public class", "class");
        Factory factory = createFactory().getFactory();
        factory.getEnvironment().setSourceClasspath(new String[]{});
        SpoonCompiler builder = new JDTBasedSpoonCompiler(factory);
        Map<String, CtType> output = new HashMap<String, CtType>();

        final Set<String> fileNames = classes.keySet();
        for (Iterator<String> iterator = fileNames.iterator(); iterator.hasNext(); ) {
            builder.getInputSources().clear();
            String filename = iterator.next();
            final String classContent = classes.get(filename);
            if(classContent != null) {
                builder.addInputSource(new VirtualFile(classContent, filename));
                try {
                    builder.build();
                } catch (Exception e) {
                    // ignore compilation error
                }
                final List<CtType<?>> all = factory.Type().getAll();
                for (int i = 0; i < all.size(); i++) {
                    CtType<?> ctType = all.get(i);
                    if(ctType.getPosition().getFile().getPath().contains(filename)) {
                        output.put(filename, ctType);
                    }
                }
            }
        }
        return output;
    }

    public static Map<String, CtType> prToCtType(String path) {
        File file = new File(path);
        Map<String, CtType> output = new HashMap<String, CtType>();
        Launcher launcher = createFactory();
        Factory factory = launcher.getFactory();
        factory.getEnvironment().setSourceClasspath(new String[]{});
        launcher.addInputResource(path);
        try {
            launcher.run();
        } catch (Exception e) {
            // ignore compilation error
        }
        List<CtType<?>> all = factory.Type().getAll();
        for (int i = 0; i < all.size(); i++) {
            CtType<?> ctType = all.get(i);
            try {
                output.put(ctType.getPosition().getFile().getPath().replace(file.getCanonicalPath() + "/", ""), ctType);
            } catch (IOException e) {

            }
        }
        return output;
    }

    public static CtType<?> stringToCTElement(String javaElement) {
        //javaElement = javaElement.replace("public class", "class");
        Launcher launcher = createFactory();
        Factory factory = launcher.getFactory();
        factory.getEnvironment().setSourceClasspath(new String[]{});
        SpoonCompiler builder = new JDTSnippetCompiler(factory, javaElement);
        builder.generateProcessedSourceFiles(OutputType.COMPILATION_UNITS);
        try {
            builder.build();
        } catch (Exception e) {
            // ignore compilation error
        }
        launcher.prettyprint();
        return factory.Type().getAll().get(0);
    }

    private static Launcher createFactory() {
        Launcher launcher = new Launcher();
        launcher.setSourceOutputDirectory("spoon");
        Factory factory = launcher.getFactory();
        factory.getEnvironment().setAutoImports(true);
        factory.getEnvironment().setNoClasspath(true);
        factory.getEnvironment().setComplianceLevel(8);
        //factory.getEnvironment().setPreserveLineNumbers(true);
        return launcher;
    }
}
