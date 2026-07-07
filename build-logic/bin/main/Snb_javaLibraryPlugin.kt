/**
 * Precompiled [snb.java-library.gradle.kts][Snb_java_library_gradle] script plugin.
 *
 * @see Snb_java_library_gradle
 */
public
class Snb_javaLibraryPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Snb_java_library_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
