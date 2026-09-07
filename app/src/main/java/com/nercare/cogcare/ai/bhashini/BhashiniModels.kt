package com.nercare.cogcare.ai.bhashini

data class BhashiniPipelineRequest(
    val pipelineTasks: List<PipelineTask>,
    val inputData: InputData
)

data class PipelineTask(
    val taskType: String,
    val config: PipelineConfig
)

data class PipelineConfig(
    val language: LanguageConfig,
    val serviceId: String? = null,
    val audioFormat: String? = null,
    val samplingRate: Int? = null
)

data class LanguageConfig(
    val sourceLanguage: String,
    val targetLanguage: String? = null
)

data class InputData(
    val audio: List<BhashiniAudioData>? = null,
    val input: List<BhashiniTextData>? = null
)

data class BhashiniAudioData(
    val audioContent: String // Base64 encoded audio
)

data class BhashiniTextData(
    val source: String
)

data class BhashiniResponse(
    val pipelineResponse: List<PipelineTaskResponse>
)

data class PipelineTaskResponse(
    val taskType: String,
    val output: List<OutputData>
)

data class OutputData(
    val source: String?,
    val target: String?,
    val audio: List<BhashiniAudioData>?
)

data class BhashiniNmtRequest(
    val input: List<BhashiniTextData>,
    val config: PipelineConfig
)
