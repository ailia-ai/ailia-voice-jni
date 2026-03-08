package axip.ailia_voice
import java.io.Closeable

class AiliaVoice(
    envId: Int = -1, // Ailia.ENVIRONMENT_ID_AUTO
    numThread: Int = 0, // Ailia.MULTITHREAD_AUTO
    memoryMode: Int = 11, // Ailia.MEMORY_REDUCE_CONSTANT or MEMORY_REDUCE_CONSTANT_WITH_INPUT_INITIALIZER or MEMORY_REUSE_INTERSTAGE,
    flags: Int = AILIA_VOICE_FLAG_NONE,) : Closeable
{
    data class AudioData(
        val samplingRate: Int,
        val channels: Int,
        val data: FloatArray
    )

    companion object {
        const val AILIA_STATUS_SUCCESS = 0
        const val AILIA_VOICE_FLAG_NONE = 0
        
        const val AILIA_VOICE_DICTIONARY_TYPE_OPEN_JTALK = 0
        const val AILIA_VOICE_DICTIONARY_TYPE_G2P_EN = 1
        const val AILIA_VOICE_DICTIONARY_TYPE_G2P_CN = 2
        const val AILIA_VOICE_DICTIONARY_TYPE_G2PW = 3

        const val AILIA_VOICE_MODEL_TYPE_TACOTRON2 = 0
        const val AILIA_VOICE_MODEL_TYPE_GPT_SOVITS = 1
        const val AILIA_VOICE_MODEL_TYPE_GPT_SOVITS_V2 = 2
        const val AILIA_VOICE_MODEL_TYPE_GPT_SOVITS_V3 = 3
        const val AILIA_VOICE_MODEL_TYPE_GPT_SOVITS_V2_PRO = 4

        const val AILIA_VOICE_CLEANER_TYPE_BASIC = 0
        const val AILIA_VOICE_CLEANER_TYPE_ENGLISH = 1

        const val AILIA_VOICE_G2P_TYPE_GPT_SOVITS_EN = 1
        const val AILIA_VOICE_G2P_TYPE_GPT_SOVITS_JA = 2
        const val AILIA_VOICE_G2P_TYPE_GPT_SOVITS_ZH = 3
        init {
            System.loadLibrary("ailia_voice")
            System.loadLibrary("ailia") // requirements from another repository
            System.loadLibrary("ailia_audio") // requirements from another repository
        }
    }

    private var voice: Long = 0

    init {
        voice = create(envId, numThread, memoryMode, flags)
    }

    fun setUserDictionaryFile(path: String, dictionaryType: Int = AILIA_VOICE_DICTIONARY_TYPE_OPEN_JTALK) {
        check(setUserDictionaryFile(voice, path, dictionaryType))
    }

    class AiliaVoiceException(message: String) : Exception(message)

    private fun check(status : Int) {
        if (status != 0){
            throw AiliaVoiceException("ailia Voice Failed $status ${getErrorDetail()}")
        }
    }

    fun openDictionaryFile(path: String, dictionaryType: Int = AILIA_VOICE_DICTIONARY_TYPE_OPEN_JTALK) {
        check(openDictionaryFile(voice, path, dictionaryType))
    }

    fun openModelFile(encoder: String, decoder1: String, decoder2: String, wave: String, ssl: String,
                     modelType: Int = AILIA_VOICE_MODEL_TYPE_TACOTRON2,
                     cleanerType: Int = AILIA_VOICE_CLEANER_TYPE_BASIC) {
        check(openModelFile(voice, encoder, decoder1, decoder2, wave, ssl, modelType, cleanerType))
    }

    fun openTacotron2ModelFile(encoder: String, decoder1: String, decoder2: String, wave: String,
                              cleanerType: Int) {
        check(openTacotron2ModelFile(voice, encoder, decoder1, decoder2, wave, cleanerType))
    }

    fun openGPTSoVITSV1ModelFile(encoder: String, decoder1: String, decoder2: String, wave: String, ssl: String) {
        check(openGPTSoVITSV1ModelFile(voice, encoder, decoder1, decoder2, wave, ssl))
    }

    fun openGPTSoVITSV2ModelFile(encoder: String, decoder1: String, decoder2: String, wave: String, ssl: String,
                                  chineseBert: String? = null, vocab: String? = null) {
        check(openGPTSoVITSV2ModelFile(voice, encoder, decoder1, decoder2, wave, ssl, chineseBert, vocab))
    }

    fun openGPTSoVITSV3ModelFile(encoder: String, decoder1: String, decoder2: String, ssl: String, vq: String, cfm: String, bigvgan: String,
                                  chineseBert: String? = null, vocab: String? = null) {
        check(openGPTSoVITSV3ModelFile(voice, encoder, decoder1, decoder2, ssl, vq, cfm, bigvgan, chineseBert, vocab))
    }

    fun openGPTSoVITSV2ProModelFile(encoder: String, decoder1: String, decoder2: String, ssl: String, vits: String, sv: String,
                                     chineseBert: String? = null, vocab: String? = null) {
        check(openGPTSoVITSV2ProModelFile(voice, encoder, decoder1, decoder2, ssl, vits, sv, chineseBert, vocab))
    }

    fun setSampleSteps(steps: Int) {
        check(setSampleSteps(voice, steps))
    }

    /**
     * Set the speech speed for synthesis.
     * Supported by GPT-SoVITS V2 and V3. Not effective for V1.
     * @param speed Speed value (default 1.0, must be greater than 0)
     */
    fun setSpeed(speed: Float) {
        check(setSpeed(voice, speed))
    }

    /**
     * Set the model type for G2P processing.
     * Sets the model type when using G2P standalone without loading model files.
     * Automatically set when openModelFile or openGPTSoVITSV3ModelFile is called.
     * @param modelType AILIA_VOICE_MODEL_TYPE_*
     */
    fun setModelType(modelType: Int) {
        check(setModelType(voice, modelType))
    }

    private fun extractFullContext(text: String) {
        check(extractFullContext(voice, text))
    }

    fun setReferenceAudio(buf: FloatArray, bufSize: Int, channels: Int, samplingRate: Int, features: String) {
        check(setReference(voice, buf, bufSize, channels, samplingRate, features))
    }

    fun g2p(text : String, g2pType: Int = AILIA_VOICE_G2P_TYPE_GPT_SOVITS_JA) : String {
        check(graphemeToPhoneme(voice, text, g2pType))
        val featureLength = getFeatureLength(voice)
        var features: String? = ""
        if (featureLength > 0) {
            features = getFeatures(voice)
            return features!!
        }
        return ""
    }

    fun synthesizeVoice(text: String): AudioData {
        check(inference(voice, text))
        val waveInfo = getWaveInfo(voice)!!
        val samples = waveInfo[0]
        val channels = waveInfo[1]
        val samplingRate = waveInfo[2]
        val bufferSize = samples * channels * 4
        val waveBuffer = FloatArray(samples * channels)
        check(getWave(voice, waveBuffer, bufferSize))
        val data = AudioData(samplingRate = samplingRate, channels = channels, data = waveBuffer)
        return data
    }

    private fun getErrorDetail(): String? {
        return getErrorDetail(voice)
    }

    override fun close() {
        if (voice != 0L) {
            destroy(voice)
            voice = 0L
        }
    }

    private external fun create(envId: Int, numThread: Int, memoryMode: Int, flags: Int): Long
    private external fun destroy(voice: Long)
    private external fun setUserDictionaryFile(voice: Long, path: String, dictionaryType: Int): Int
    private external fun openDictionaryFile(voice: Long, path: String, dictionaryType: Int): Int
    private external fun openModelFile(voice: Long, encoder: String, decoder1: String, decoder2: String, wave: String, ssl: String, modelType: Int, cleanerType: Int): Int
    private external fun openTacotron2ModelFile(voice: Long, encoder: String, decoder1: String, decoder2: String, wave: String, cleanerType: Int): Int
    private external fun openGPTSoVITSV1ModelFile(voice: Long, encoder: String, decoder1: String, decoder2: String, wave: String, ssl: String): Int
    private external fun openGPTSoVITSV2ModelFile(voice: Long, encoder: String, decoder1: String, decoder2: String, wave: String, ssl: String, chineseBert: String?, vocab: String?): Int
    private external fun openGPTSoVITSV3ModelFile(voice: Long, encoder: String, decoder1: String, decoder2: String, ssl: String, vq: String, cfm: String, bigvgan: String, chineseBert: String?, vocab: String?): Int
    private external fun openGPTSoVITSV2ProModelFile(voice: Long, encoder: String, decoder1: String, decoder2: String, ssl: String, vits: String, sv: String, chineseBert: String?, vocab: String?): Int
    private external fun setSampleSteps(voice: Long, steps: Int): Int
    private external fun setSpeed(voice: Long, speed: Float): Int
    private external fun setModelType(voice: Long, modelType: Int): Int
    private external fun graphemeToPhoneme(voice: Long, text: String, g2pType: Int): Int
    private external fun extractFullContext(voice: Long, text: String): Int
    private external fun getFeatureLength(voice: Long): Int
    private external fun getFeatures(voice: Long): String?
    private external fun setReference(voice: Long, buf: FloatArray, bufSize: Int, channels: Int, samplingRate: Int, features: String): Int
    private external fun inference(voice: Long, text: String): Int
    private external fun getWaveInfo(voice: Long): IntArray?
    private external fun getWave(voice: Long, buf: FloatArray, bufSize: Int): Int
    private external fun getErrorDetail(voice: Long): String?
}
