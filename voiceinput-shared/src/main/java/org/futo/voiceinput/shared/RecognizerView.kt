package org.futo.voiceinput.shared

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleCoroutineScope
import org.futo.voiceinput.shared.engine.ConnectivityManagerNetworkStatus
import org.futo.voiceinput.shared.engine.GoogleSpeechRecognizer
import org.futo.voiceinput.shared.engine.OnlineEngineConfiguration
import org.futo.voiceinput.shared.engine.SpeechEngine
import org.futo.voiceinput.shared.engine.SpeechEngineFailure
import org.futo.voiceinput.shared.engine.SpeechEngineKind
import org.futo.voiceinput.shared.engine.SpeechEngineMode
import org.futo.voiceinput.shared.engine.isOnlineRecognitionUsable
import org.futo.voiceinput.shared.engine.selectSpeechEngine
import org.futo.voiceinput.shared.engine.shouldFallBackToLocal
import org.futo.voiceinput.shared.types.AudioRecognizerListener
import org.futo.voiceinput.shared.types.InferenceState
import org.futo.voiceinput.shared.types.Language
import org.futo.voiceinput.shared.types.MagnitudeState
import org.futo.voiceinput.shared.ui.InnerRecognize
import org.futo.voiceinput.shared.ui.MicrophoneDeviceState
import org.futo.voiceinput.shared.ui.PartialDecodingResult
import org.futo.voiceinput.shared.ui.RecognizeLoadingCircle
import org.futo.voiceinput.shared.ui.RecognizeMicError
import org.futo.voiceinput.shared.whisper.DecodingConfiguration
import org.futo.voiceinput.shared.whisper.ModelManager
import org.futo.voiceinput.shared.whisper.MultiModelRunConfiguration

data class RecognizerViewSettings(
    val shouldShowVerboseFeedback: Boolean,
    val shouldShowInlinePartialResult: Boolean,
    val shouldAnimateBubble: Boolean,

    val engineMode: SpeechEngineMode,
    val onlineConfiguration: OnlineEngineConfiguration,
    val allowLocalFallback: Boolean,

    /** The on-device model to use, or null when none is installed. */
    val modelRunConfiguration: MultiModelRunConfiguration?,
    val decodingConfiguration: DecodingConfiguration,
    val recordingConfiguration: RecordingSettings
)

private val VerboseAnnotations = hashMapOf(
    InferenceState.ExtractingMel to R.string.extracting_features,
    InferenceState.LoadingModel to R.string.loading_model,
    InferenceState.Encoding to R.string.processing,
    InferenceState.DecodingLanguage to R.string.decoding,
    InferenceState.SwitchingModel to R.string.switching_model,
    InferenceState.DecodingStarted to R.string.decoding
)

private val DefaultAnnotations = hashMapOf(
    InferenceState.ExtractingMel to R.string.processing,
    InferenceState.LoadingModel to R.string.processing,
    InferenceState.Encoding to R.string.processing,
    InferenceState.DecodingLanguage to R.string.processing,
    InferenceState.SwitchingModel to R.string.switching_model,
    InferenceState.DecodingStarted to R.string.processing
)

interface RecognizerViewListener {
    fun cancelled()

    fun recordingStarted(device: MicrophoneDeviceState)

    fun finished(result: String)

    fun partialResult(result: String)

    // Return true if a permission modal was shown, otherwise return false
    fun requestPermission(onGranted: () -> Unit, onRejected: () -> Unit): Boolean

    fun openSettings()
}

class RecognizerView(
    private val context: Context,
    private val listener: RecognizerViewListener,
    private val settings: RecognizerViewSettings,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val modelManager: ModelManager
) {
    private val magnitudeState = mutableFloatStateOf(0.0f)
    private val statusState = mutableStateOf(MagnitudeState.NOT_TALKED_YET)

    enum class CurrentView {
        LoadingCircle, PartialDecodingResult, InnerRecognize, PermissionError, ModelError, OnlineError
    }

    private val loadingCircleText = mutableStateOf("")
    private val partialDecodingText = mutableStateOf("")
    private val currentViewState = mutableStateOf(CurrentView.LoadingCircle)

    private val currentDeviceState = mutableStateOf(MicrophoneDeviceState(
        bluetoothAvailable = false,
        bluetoothActive = false,
        setBluetooth = { },
        deviceName = "",
        bluetoothPreferredByUser = false
    ))

    @Composable
    private fun ClickableErrorText(text: String) {
        Box(modifier = Modifier
            .fillMaxSize()
            .clickable(
                enabled = true,
                onClickLabel = null,
                onClick = {
                    listener.openSettings()
                },
                role = null,
                indication = null,
                interactionSource = remember { MutableInteractionSource() })) {
            Text(
                text,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp), textAlign = TextAlign.Center)
        }
    }

    @Composable
    fun Content() {
        when (currentViewState.value) {
            CurrentView.LoadingCircle -> {
                Column {
                    RecognizeLoadingCircle(text = loadingCircleText.value)
                }
            }

            CurrentView.PartialDecodingResult -> {
                Column {
                    PartialDecodingResult(text = partialDecodingText.value)
                }
            }

            CurrentView.InnerRecognize -> {
                InnerRecognize(
                    magnitude = magnitudeState,
                    state = statusState,
                    device = currentDeviceState
                )
            }

            CurrentView.PermissionError -> {
                Column {
                    RecognizeMicError(openSettings = { currentEngine?.openPermissionSettings() })
                }
            }

            CurrentView.ModelError -> {
                ClickableErrorText(text = stringResource(R.string.model_load_error))
            }

            CurrentView.OnlineError -> {
                ClickableErrorText(text = stringResource(R.string.online_recognition_failed))
            }
        }
    }

    fun finish() {
        currentEngine?.finish()
    }

    fun cancel() {
        val engine = currentEngine
        if (engine == null) {
            audioRecognizerListener.cancelled()
        } else {
            engine.cancel()
        }
    }

    private val audioRecognizerListener = object : AudioRecognizerListener {
        override fun cancelled() {
            listener.cancelled()
        }

        override fun finished(result: String) {
            listener.finished(result)
        }

        override fun languageDetected(language: Language) {
            // TODO
        }

        override fun modelLoadingFailed() {
            listener.cancelled()
            currentViewState.value = CurrentView.ModelError
        }

        override fun partialResult(result: String) {
            listener.partialResult(result)
            if (settings.shouldShowInlinePartialResult && result.isNotBlank()) {
                partialDecodingText.value = result
                currentViewState.value = CurrentView.PartialDecodingResult
            }
        }


        override fun decodingStatus(status: InferenceState) {
            val text = context.getString(
                when (settings.shouldShowVerboseFeedback) {
                    true -> VerboseAnnotations[status]!!
                    false -> DefaultAnnotations[status]!!
                }
            )

            loadingCircleText.value = text
            currentViewState.value = CurrentView.LoadingCircle
        }

        override fun loading() {
            loadingCircleText.value = context.getString(R.string.initializing)
            currentViewState.value = CurrentView.LoadingCircle
        }

        override fun needPermission(onResult: (Boolean) -> Unit) {
            val shown = listener.requestPermission(
                onGranted = {
                    onResult(true)
                },
                onRejected = {
                    onResult(false)
                    currentViewState.value = CurrentView.PermissionError
                }
            )

            if(!shown) {
                currentViewState.value = CurrentView.PermissionError
            }
        }

        override fun recordingStarted(device: MicrophoneDeviceState) {
            updateMagnitude(0.0f, MagnitudeState.NOT_TALKED_YET)
            currentDeviceState.value = device
            listener.recordingStarted(device)
        }

        override fun updateMagnitude(magnitude: Float, state: MagnitudeState) {
            if(settings.shouldAnimateBubble) magnitudeState.floatValue = magnitude
            statusState.value = state
            currentViewState.value = CurrentView.InnerRecognize
        }

        override fun processing() {
            loadingCircleText.value = context.getString(R.string.processing)
            currentViewState.value = CurrentView.LoadingCircle
        }
    }

    private val selection = selectSpeechEngine(
        mode = settings.engineMode,
        isOnlineUsable = isOnlineRecognitionUsable(context, ConnectivityManagerNetworkStatus(context)),
        canFallBackToLocal = settings.allowLocalFallback && settings.modelRunConfiguration != null
    )

    private val onlineEngine = GoogleSpeechRecognizer(
        context = context,
        listener = audioRecognizerListener,
        configuration = settings.onlineConfiguration,
        onFailure = ::handleEngineFailure
    )

    private var localEngine: SpeechEngine? = null
    private var currentEngine: SpeechEngine? = null

    private fun obtainLocalEngine(): SpeechEngine? {
        localEngine?.let { return it }

        val runConfiguration = settings.modelRunConfiguration ?: return null

        return try {
            AudioRecognizer(
                context = context,
                lifecycleScope = lifecycleScope,
                modelManager = modelManager,
                listener = audioRecognizerListener,
                settings = AudioRecognizerSettings(
                    modelRunConfiguration = runConfiguration,
                    decodingConfiguration = settings.decodingConfiguration,
                    recordingConfiguration = settings.recordingConfiguration
                )
            ).also { localEngine = it }
        } catch (e: ModelDoesNotExistException) {
            null
        }
    }

    private fun handleEngineFailure(failure: SpeechEngineFailure) {
        val local = obtainLocalEngine()

        if (shouldFallBackToLocal(failure, selection) && local != null) {
            currentEngine = local
            loadingCircleText.value = context.getString(R.string.switching_to_offline_model)
            currentViewState.value = CurrentView.LoadingCircle
            local.start()
        } else {
            listener.cancelled()
            currentViewState.value = CurrentView.OnlineError
        }
    }

    fun reset() {
        currentEngine?.reset()
    }

    fun start() {
        val engine = when (selection.kind) {
            SpeechEngineKind.ONLINE -> onlineEngine
            SpeechEngineKind.LOCAL -> obtainLocalEngine()
        }

        if (engine == null) {
            audioRecognizerListener.modelLoadingFailed()
            return
        }

        currentEngine = engine
        engine.start()
    }
}
