import io.kubernetes.client.models.*
import com.google.common.reflect.ClassPath
import io.kubernetes.client.util.Yaml
import org.apache.commons.lang3.tuple.Pair;

class KubernetesModelClassesGen {

    public generate( project, String packageName, String className ) {

        // Where to write the classes
        File targetDirectory = new File( project.basedir.toString() + '/src/main/java' )

        ClassPath cp = ClassPath.from(Yaml.class.getClassLoader());
        Set<ClassPath.ClassInfo> allClasses = cp.getTopLevelClasses("io.kubernetes.client.models");

        for (ClassPath.ClassInfo clazz : allClasses) {
            print(clazz.simpleName)
        }

        // The directory to write the source to
        File packageDir = new File( targetDirectory, packageName.replace( '.', '/' ) )

        // Now to create our enum
        def out = []
        out<<'package '+packageName+';\n \n'

        out<<'import io.kubernetes.client.models.*'+';\n'
        out<<'import java.util.ArrayList'+';\n'
        out<<'import java.util.List'+';\n'
        out<<'public class '+className+' {\n \n'
        out<<'  private List<Class> allClasses = new ArrayList(); \n'

        out<<'  public KubernetesModelClasses(){ \n'


        for (ClassPath.ClassInfo clazz : allClasses) {
            out<<'    allClasses.add('+clazz.simpleName+'.class);\n'
        }
        out<<'  }\n \n'
        out<<'  public List<Class> getAllClasses() { \n'
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