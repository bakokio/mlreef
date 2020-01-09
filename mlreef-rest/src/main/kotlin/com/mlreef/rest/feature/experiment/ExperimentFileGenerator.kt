package com.mlreef.rest.feature.experiment

import com.mlreef.rest.DataProcessorInstance
import com.mlreef.rest.DataProcessorType
import com.mlreef.rest.ParameterInstance
import org.springframework.core.io.ClassPathResource
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

class ExperimentFileGenerator {

    companion object {
        const val EPF_TAG = "%EPF_TAG%"
        const val GITLAB_ROOT_URL = "%GITLAB_ROOT_URL%"
        const val GITLAB_GROUP = "%GITLAB_GROUP%"
        const val GITLAB_PROJECT = "%GITLAB_PROJECT%"
        const val CONF_EMAIL = "%CONF_EMAIL%"
        const val CONF_NAME = "%CONF_NAME%"
        const val SOURCE_BRANCH = "%SOURCE_BRANCH%"
        const val TARGET_BRANCH = "%TARGET_BRANCH%"
        const val PIPELINE_STRING = "%PIPELINE_STRING%"
        const val NEWLINE = "\n"

        fun writeInstances(list: List<DataProcessorInstance>): List<String> {
            return list.map { writeInstance(it) }
        }

        fun writeInstance(instance: DataProcessorInstance): String {
            // /epf/pipelines
            //  let line = `   - python ${path}/${dataOperation.command}.py --images-path#directoriesAndFiles`;
            val path = if (instance.dataProcessor.type == DataProcessorType.ALGORITHM) {
                "/epf/model/"
            } else {
                "/epf/pipelines/"
            }
            return "python $path${instance.dataProcessor.command}.py " + writeParameters(instance.parameterInstances)
        }

        fun writeParameters(parameterInstances: List<ParameterInstance>): String {
            return parameterInstances.joinToString(" ") { writeParameter(it) }
        }

        fun writeParameter(it: ParameterInstance): String {
            return "--${it.name} ${it.value}"
        }
    }

    var input: String = ""
    var output: String = ""

    fun init(): ExperimentFileGenerator {
        input = ""
        output = ""

        val classPathResource = ClassPathResource("mlreef-file-template.yml")
        val inputStream = classPathResource.inputStream
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.lines()
            input = lines.collect(Collectors.joining(NEWLINE))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        output = input
        return this
    }

    fun replacePipeline(list: List<DataProcessorInstance>): ExperimentFileGenerator {
        val pipelineStrings = writeInstances(list)
        val indexOf = input.indexOf("- git add .")
        val lineBegin = input.indexOf(NEWLINE, indexOf)
        val dash = input.indexOf("-", lineBegin)
        val indent = dash - lineBegin - 1
        val prefix = " ".repeat(indent) + "-"

        val indentedStrings = pipelineStrings.joinToString(NEWLINE) { "$prefix $it" }
        output = output.replace(PIPELINE_STRING, indentedStrings)
        return this
    }

    fun replaceAllSingleStrings(
        epfTag: String = "",
        gitlabRootUrl: String = "",
        gitlabGroup: String = "",
        gitlabProject: String = "",
        confEmail: String = "",
        confName: String = "",
        sourceBranch: String = "",
        targetBranch: String = ""

    ): ExperimentFileGenerator {
        output = output
            .replace(EPF_TAG, epfTag)
            .replace(GITLAB_ROOT_URL, gitlabRootUrl.replace("https://", "").replace("http://", ""))
            .replace(GITLAB_GROUP, gitlabGroup)
            .replace(GITLAB_PROJECT, gitlabProject)
            .replace(CONF_EMAIL, confEmail)
            .replace(CONF_NAME, confName)
            .replace(SOURCE_BRANCH, sourceBranch)
            .replace(TARGET_BRANCH, targetBranch)
        return this
    }
}
