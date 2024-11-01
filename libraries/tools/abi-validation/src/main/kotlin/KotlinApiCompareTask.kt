/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import org.gradle.api.*
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.abi.tools.JvmAbiTools

public open class KotlinApiCompareTask : DefaultTask() {

    @get:InputFiles // don't fail the task if file does not exist, instead print custom error message from verify()
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public val projectApiFile: RegularFileProperty = project.objects.fileProperty()

    @get:InputFiles // don't fail the task if file does not exist, instead print custom error message from verify()
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public val generatedApiFile: RegularFileProperty = project.objects.fileProperty()

    private val projectName = project.name

    private val rootDir = project.rootDir

    @TaskAction
    internal fun verify() {
        val projectApiFile = projectApiFile.get().asFile
        val generatedApiFile = generatedApiFile.get().asFile

        if (!projectApiFile.exists()) {
            error(
                "Expected file with API declarations '${projectApiFile.relativeTo(rootDir)}' does not exist.\n" +
                        "Please ensure that ':apiDump' was executed in order to get " +
                        "an API dump to compare the build against"
            )
        }
        if (!generatedApiFile.exists()) {
            error(
                "Expected file with generated API declarations '${generatedApiFile.relativeTo(rootDir)}'" +
                        " does not exist."
            )
        }

        val diffSet = mutableSetOf<String>()
        val diff = JvmAbiTools().filesDiff(projectApiFile, generatedApiFile)
        if (diff != null) diffSet.add(diff)
        if (diffSet.isNotEmpty()) {
            val diffText = diffSet.joinToString("\n\n")
            val subject = projectName
            error(
                "API check failed for project $subject.\n$diffText\n\n" +
                        "You can run :$subject:apiDump task to overwrite API declarations"
            )
        }
    }
}
