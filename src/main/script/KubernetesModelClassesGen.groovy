import com.google.common.reflect.ClassPath
import io.kubernetes.client.util.Yaml

class KubernetesModelClassesGen {

    public generate( project, String packageName, String className ) {

        // Where to write the classes
        File targetDirectory = new File( project.basedir.toString() + '/src/main/java' )

        ClassPath cp = ClassPath.from(Yaml.class.getClassLoader());
        Set<ClassPath.ClassInfo> allClasses = cp.getTopLevelClasses("io.kubernetes.client.models");

        // The directory to write the source to
        File packageDir = new File( targetDirectory, packageName.replace( '.', '/' ) )

        // Now to create our enum
        def out = []
        out<<'package '+packageName+';\n \n'

        out<<'import io.kubernetes.client.models.*;\n'
        out<<'import java.util.ArrayList;;\n'
        out<<'import java.util.List;\n'
        out<<'import java.util.Collections;\n\n'
        out<<'public class '+className+' extends ArrayList<Class> {\n \n'
        out<<'  private static final List<Class> allClasses = Collections.unmodifiableList(new '+className+'()); \n'

        out<<'  private '+className+'(){ \n'


        for (ClassPath.ClassInfo clazz : allClasses) {
            out<<'    add('+clazz.simpleName+'.class);\n'
        }
        out<<'  }\n \n'
        out<<'  public static List<Class> getAllClasses() { \n'
        out<<'    return allClasses; \n'

        // Finish the class
        out<<'  }\n'
        out<<'}\n'


        // Convert the array into a string
        StringBuilder sb = new StringBuilder()
        out.each { sb.append(it) }

        // Now write the source, ensuring the directory exists first
        packageDir.mkdirs()
        new File( packageDir, className + ".java" ).write( sb.toString() );
    }

}