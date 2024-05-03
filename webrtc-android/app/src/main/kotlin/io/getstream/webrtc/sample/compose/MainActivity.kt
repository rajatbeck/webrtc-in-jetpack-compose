/*
 * Copyright 2023 Stream.IO, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.webrtc.sample.compose

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arx.camera.ArxHeadsetApi
import com.arx.camera.UsbDeviceManagerFilter
import com.arx.camera.foreground.ArxHeadsetHandler
import com.arx.camera.headsetbutton.ArxHeadsetButton
import com.arx.camera.headsetbutton.ImuData
import com.arx.camera.ui.ArxPermissionActivityResult
import com.arx.camera.ui.ArxPermissionActivityResultContract
import com.arx.camera.util.Resolution
import io.getstream.webrtc.sample.compose.ui.screens.stage.StageScreen
import io.getstream.webrtc.sample.compose.ui.screens.video.VideoCallScreen
import io.getstream.webrtc.sample.compose.ui.theme.WebrtcSampleComposeTheme
import io.getstream.webrtc.sample.compose.webrtc.SignalingClient
import io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerConnectionFactory
import io.getstream.webrtc.sample.compose.webrtc.sessions.LocalWebRtcSessionManager
import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManager
import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManagerImpl

class MainActivity : ComponentActivity() {

  private val usbDeviceFilter by lazy { UsbDeviceManagerFilter(this) }
  private var arxHeadsetHandler: ArxHeadsetHandler? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 0)

    val sessionManager: WebRtcSessionManager = WebRtcSessionManagerImpl(
      context = this,
      signalingClient = SignalingClient(),
      peerConnectionFactory = StreamPeerConnectionFactory(this)
    )

    setContent {
      WebrtcSampleComposeTheme {
        CompositionLocalProvider(LocalWebRtcSessionManager provides sessionManager) {
          // A surface container using the 'background' color from the theme
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
          ) {
            var onCallScreen by remember { mutableStateOf(false) }
            val state by sessionManager.signalingClient.sessionStateFlow.collectAsState()

            if (!onCallScreen) {
              StageScreen(state = state) { onCallScreen = true }
            } else {
              VideoCallScreen()
            }
          }
        }
      }
    }
    initArx()
  }

  private val myActivityResultContract = ArxPermissionActivityResultContract()

  private val myActivityResultLauncher = registerForActivityResult(myActivityResultContract) {
    when (it) {
      ArxPermissionActivityResult.AllPermissionsGranted -> {
        startHeadSetIfUsbConnected()
      }
      ArxPermissionActivityResult.UsbDisconnected -> Unit
      ArxPermissionActivityResult.BackPressed -> Unit
      ArxPermissionActivityResult.CloseAppRequested -> finish()
    }
  }


  private fun initArx() {
    arxHeadsetHandler = ArxHeadsetHandler(this, true, object : ArxHeadsetApi {
      override fun onDeviceConnectionError(p0: Throwable) {

      }

      override fun onDevicePhotoReceived(bitmap: Bitmap, p1: Resolution) {

      }

      override fun onStillPhotoReceived(bitmap: Bitmap, p1: Resolution) {

      }

      override fun onButtonClicked(p0: ArxHeadsetButton, p1: Boolean) {

      }

      override fun onDisconnect() {

      }

      override fun onCameraResolutionUpdate(p0: MutableList<Resolution>, p1: Resolution) {

      }

      override fun onPermissionDenied() {
        if (!usbDeviceFilter.isUsbDeviceConnectedAndPermissionGiven()) {
          startPermissionHandler()
        }
      }

      override fun onImuDataUpdate(p0: ImuData) {

      }
    })
    startHeadSetIfUsbConnected()
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    arxHeadsetHandler?.startHeadSetService(Resolution._1280x720)
  }

  private fun startPermissionHandler() {
    myActivityResultLauncher.launch(true)
  }

  private fun startHeadSetIfUsbConnected() {
    if (usbDeviceFilter.isAllRequiredUsbConnected()) {
      arxHeadsetHandler?.startHeadSetService(Resolution._1280x720)
    }
  }
}
