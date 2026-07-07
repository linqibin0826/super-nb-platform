/**
 * Precompiled [snb.hexagonal-domain.gradle.kts][Snb_hexagonal_domain_gradle] script plugin.
 *
 * @see Snb_hexagonal_domain_gradle
 */
public
class Snb_hexagonalDomainPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Snb_hexagonal_domain_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
