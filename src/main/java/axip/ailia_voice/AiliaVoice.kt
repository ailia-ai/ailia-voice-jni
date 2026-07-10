package axip.ailia_voice
import java.io.Closeable

/**
 * Text to speech (TTS) library.
 *
 * This class provides speech synthesis (Tacotron2 and GPT-SoVITS) via the
 * ailia Voice native library.
 *
 * @constructor Creates an ailia Voice instance.
 * @param envId Environment ID of ailia (-1 to select automatically).
 * @param numThread Number of threads (0 to select automatically).
 * @param memoryMode Memory mode of ailia.
 * @param flags OR of the AILIA_VOICE_FLAG_* constants.
 */
class AiliaVoice(
    envId: Int = -1, // Ailia.ENVIRONMENT_ID_AUTO
    numThread: Int = 0, // Ailia.MULTITHREAD_AUTO
    memoryMode: Int = 11, // Ailia.MEMORY_REDUCE_CONSTANT or MEMORY_REDUCE_CONSTANT_WITH_INPUT_INITIALIZER or MEMORY_REUSE_INTERSTAGE,
    flags: Int = AILIA_VOICE_FLAG_NONE,) : Closeable
{
    /**
     * Synthesized audio.
     *
     * @property samplingRate Sampling rate (Hz).
     * @property channels Number of channels.
     * @property data PCM data (interleaved if multi channel).
     */
    data class AudioData(
        val samplingRate: Int,
        val channels: Int,
        val data: FloatArray
    )

    /**
     * Constants of ailia Voice.
     *
     * AILIA_VOICE_DICTIONARY_TYPE_* defines the dictionary type,
     * AILIA_VOICE_MODEL_TYPE_* defines the model type,
     * AILIA_VOICE_CLEANER_TYPE_* defines the text cleaner type,
     * and AILIA_VOICE_G2P_TYPE_* defines the G2P type.
     */
    companion object {
        /** Status code of success. */
        const val AILIA_STATUS_SUCCESS = 0
        /** Default flag (no option). */
        const val AILIA_VOICE_FLAG_NONE = 0

        /** Dictionary for OpenJTalk (Japanese). */
        const val AILIA_VOICE_DICTIONARY_TYPE_OPEN_JTALK = 0
        /** Dictionary for G2P English. */
        const val AILIA_VOICE_DICTIONARY_TYPE_G2P_EN = 1
        /** Dictionary for G2P Chinese. */
        const val AILIA_VOICE_DICTIONARY_TYPE_G2P_CN = 2
        /** Dictionary for G2PW (Chinese polyphonic characters). */
        const val AILIA_VOICE_DICTIONARY_TYPE_G2PW = 3

        /** Tacotron2 model. */
        const val AILIA_VOICE_MODEL_TYPE_TACOTRON2 = 0
        /** GPT-SoVITS V1 model. */
        const val AILIA_VOICE_MODEL_TYPE_GPT_SOVITS = 1
        /** GPT-SoVITS V2 model. */
        const val AILIA_VOICE_MODEL_TYPE_GPT_SOVITS_V2 = 2
        /** GPT-SoVITS V3 model. */
        const val AILIA_VOICE_MODEL_TYPE_GPT_SOVITS_V3 = 3
        /** GPT-SoVITS V2 Pro model. */
        const val AILIA_VOICE_MODEL_TYPE_GPT_SOVITS_V2_PRO = 4

        /** Basic text cleaner. */
        const val AILIA_VOICE_CLEANER_TYPE_BASIC = 0
        /** Text cleaner for English. */
        const val AILIA_VOICE_CLEANER_TYPE_ENGLISH = 1

        /** G2P for English of GPT-SoVITS. */
        const val AILIA_VOICE_G2P_TYPE_GPT_SOVITS_EN = 1
        /** G2P for Japanese of GPT-SoVITS. */
        const val AILIA_VOICE_G2P_TYPE_GPT_SOVITS_JA = 2
        /** G2P for Chinese of GPT-SoVITS. */
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

    /**
     * Sets a user dictionary file.
     *
     * The user dictionary is in MeCab format. Call before [openDictionaryFile].
     *
     * @param path Path to the user dictionary file.
     * @param dictionaryType Type of the dictionary, one of the AILIA_VOICE_DICTIONARY_TYPE_* constants.
     */
    fun setUserDictionaryFile(path: String, dictionaryType: Int = AILIA_VOICE_DICTIONARY_TYPE_OPEN_JTALK) {
        check(setUserDictionaryFile(voice, path, dictionaryType))
    }

    /** Exception thrown when an ailia Voice API call fails. */
    class AiliaVoiceException(message: String) : Exception(message)

    private fun check(status : Int) {
        if (status != 0){
            throw AiliaVoiceException("ailia Voice Failed $status ${getErrorDetail()}")
        }
    }

    /**
     * Opens a dictionary.
     *
     * @param path Path to the dictionary folder.
     * @param dictionaryType Type of the dictionary, one of the AILIA_VOICE_DICTIONARY_TYPE_* constants.
     */
    fun openDictionaryFile(path: String, dictionaryType: Int = AILIA_VOICE_DICTIONARY_TYPE_OPEN_JTALK) {
        check(openDictionaryFile(voice, path, dictionaryType))
    }

    /**
     * Opens a model.
     *
     * @param encoder Path to the onnx file.
     * @param decoder1 Path to the onnx file.
     * @param decoder2 Path to the onnx file.
     * @param wave Path to the onnx file.
     * @param ssl Path to the onnx file.
     * @param modelType Type of the model, one of the AILIA_VOICE_MODEL_TYPE_* constants.
     * @param cleanerType Type of the text cleaner, one of the AILIA_VOICE_CLEANER_TYPE_* constants.
     */
    fun openModelFile(encoder: String, decoder1: String, decoder2: String, wave: String, ssl: String,
                     modelType: Int = AILIA_VOICE_MODEL_TYPE_TACOTRON2,
                     cleanerType: Int = AILIA_VOICE_CLEANER_TYPE_BASIC) {
        check(openModelFile(voice, encoder, decoder1, decoder2, wave, ssl, modelType, cleanerType))
    }

    /**
     * Opens a Tacotron2 model.
     *
     * @param encoder Path to the onnx file.
     * @param decoder1 Path to the onnx file.
     * @param decoder2 Path to the onnx file.
     * @param wave Path to the onnx file.
     * @param cleanerType Type of the text cleaner, one of the AILIA_VOICE_CLEANER_TYPE_* constants.
     */
    fun openTacotron2ModelFile(encoder: String, decoder1: String, decoder2: String, wave: String,
                              cleanerType: Int) {
        check(openTacotron2ModelFile(voice, encoder, decoder1, decoder2, wave, cleanerType))
    }

    /**
     * Opens a GPT-SoVITS V1 model.
     *
     * @param encoder Path to the onnx file.
     * @param decoder1 Path to the onnx file.
     * @param decoder2 Path to the onnx file.
     * @param wave Path to the onnx file.
     * @param ssl Path to the onnx file.
     */
    fun openGPTSoVITSV1ModelFile(encoder: String, decoder1: String, decoder2: String, wave: String, ssl: String) {
        check(openGPTSoVITSV1ModelFile(voice, encoder, decoder1, decoder2, wave, ssl))
    }

    /**
     * Opens a GPT-SoVITS V2 model.
     *
     * @param encoder Path to the onnx file.
     * @param decoder1 Path to the onnx file.
     * @param decoder2 Path to the onnx file.
     * @param wave Path to the onnx file.
     * @param ssl Path to the onnx file.
     * @param chineseBert Path to the chinese-roberta.onnx file, or null to disable Chinese BERT.
     * @param vocab Path to the vocab.txt file, or null to disable Chinese BERT.
     */
    fun openGPTSoVITSV2ModelFile(encoder: String, decoder1: String, decoder2: String, wave: String, ssl: String,
                                  chineseBert: String? = null, vocab: String? = null) {
        check(openGPTSoVITSV2ModelFile(voice, encoder, decoder1, decoder2, wave, ssl, chineseBert, vocab))
    }

    /**
     * Opens a GPT-SoVITS V3 model.
     *
     * @param encoder Path to the onnx file.
     * @param decoder1 Path to the onnx file.
     * @param decoder2 Path to the onnx file.
     * @param ssl Path to the onnx file.
     * @param vq Path to the onnx file.
     * @param cfm Path to the onnx file.
     * @param bigvgan Path to the onnx file.
     * @param chineseBert Path to the chinese-roberta.onnx file, or null to disable Chinese BERT.
     * @param vocab Path to the vocab.txt file, or null to disable Chinese BERT.
     */
    fun openGPTSoVITSV3ModelFile(encoder: String, decoder1: String, decoder2: String, ssl: String, vq: String, cfm: String, bigvgan: String,
                                  chineseBert: String? = null, vocab: String? = null) {
        check(openGPTSoVITSV3ModelFile(voice, encoder, decoder1, decoder2, ssl, vq, cfm, bigvgan, chineseBert, vocab))
    }

    /**
     * Opens a GPT-SoVITS V2 Pro model.
     *
     * @param encoder Path to the onnx file.
     * @param decoder1 Path to the onnx file.
     * @param decoder2 Path to the onnx file.
     * @param ssl Path to the onnx file.
     * @param vits Path to the onnx file.
     * @param sv Path to the onnx file.
     * @param chineseBert Path to the chinese-roberta.onnx file, or null to disable Chinese BERT.
     * @param vocab Path to the vocab.txt file, or null to disable Chinese BERT.
     */
    fun openGPTSoVITSV2ProModelFile(encoder: String, decoder1: String, decoder2: String, ssl: String, vits: String, sv: String,
                                     chineseBert: String? = null, vocab: String? = null) {
        check(openGPTSoVITSV2ProModelFile(voice, encoder, decoder1, decoder2, ssl, vits, sv, chineseBert, vocab))
    }

    /**
     * Sets the number of CFM sampling steps.
     *
     * Supported by GPT-SoVITS V3.
     *
     * @param steps Number of sampling steps.
     */
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

    /**
     * Specifies the voice that will serve as the timbre for speech synthesis.
     *
     * @param buf PCM data of the reference audio (interleaved if multi channel).
     * @param bufSize Size of the PCM data in bytes.
     * @param channels Number of channels.
     * @param samplingRate Sampling rate (Hz).
     * @param features Features of the reference text obtained by [g2p].
     */
    fun setReferenceAudio(buf: FloatArray, bufSize: Int, channels: Int, samplingRate: Int, features: String) {
        check(setReference(voice, buf, bufSize, channels, samplingRate, features))
    }

    /**
     * Converts graphemes to phonemes (G2P).
     *
     * @param text Input string.
     * @param g2pType Type of the G2P, one of the AILIA_VOICE_G2P_TYPE_* constants.
     * @return Feature string of the phonemes.
     */
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

    /**
     * Synthesizes voice from the input text.
     *
     * For GPT-SoVITS, pass the feature string obtained by [g2p].
     *
     * @param text Input text or feature string.
     * @return Synthesized audio.
     */
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

    /**
     * Releases the native resources.
     */
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
