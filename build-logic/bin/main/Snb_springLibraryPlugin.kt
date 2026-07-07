/**
 * Precompiled [snb.spring-library.gradle.kts][Snb_spring_library_gradle] script plugin.
 *
 * @see Snb_spring_library_gradle
 */
public
class Snb_springLibraryPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Snb_spring_library_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
