import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion

/** Android settings shared by application and library modules. */
internal fun CommonExtension.configureAndroidCommon(namespaceValue: String) {
    namespace = namespaceValue
    compileSdk {
        version = release(ProjectConfig.COMPILE_SDK) {
            minorApiLevel = ProjectConfig.COMPILE_SDK_MINOR
        }
    }
    defaultConfig.apply {
        minSdk = ProjectConfig.MIN_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions.apply {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint.apply {
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = false
        // Version bumps are Dependabot's job, not a build failure.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
    testOptions.unitTests.apply {
        isReturnDefaultValues = true
        isIncludeAndroidResources = true
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}
