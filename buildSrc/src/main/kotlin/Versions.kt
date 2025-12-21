import org.gradle.api.Project
import java.io.ByteArrayOutputStream

object Versions {
    fun getVersionFromGit(project: Project): String {
        return try {
            val stdout = ByteArrayOutputStream()
            project.exec {
                commandLine("git", "describe", "--tags", "--always", "--dirty")
                standardOutput = stdout
                isIgnoreExitValue = true
            }
            val version = stdout.toString().trim()
            
            when {
                version.isEmpty() -> "0.1.0-SNAPSHOT"
                version.startsWith("v") -> version.substring(1)
                else -> "$version-SNAPSHOT"
            }
        } catch (e: Exception) {
            "0.1.0-SNAPSHOT"
        }
    }
}
