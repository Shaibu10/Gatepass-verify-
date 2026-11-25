package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Size
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@OptIn(ExperimentalGetImage::class)
@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraQrScannerCard(
  onQrDecoded: (rawText: String, fromLiveCamera: Boolean) -> Unit,
  onPhotoDecodeError: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var cameraActive by remember { mutableStateOf(true) }
  var torchEnabled by remember { mutableStateOf(false) }
  var isExpanded by remember { mutableStateOf(false) }
  var zoomLevel by remember { mutableFloatStateOf(1.0f) }
  var maxZoom by remember { mutableFloatStateOf(3.0f) }
  var cameraBindError by remember { mutableStateOf<String?>(null) }
  var boundCamera by remember { mutableStateOf<Camera?>(null) }
  var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
  var tapFocusOffset by remember { mutableStateOf<Offset?>(null) }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
    }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
      if (uri != null) {
        decodeQrFromPhotoUri(
          context = context,
          uri = uri,
          onDecoded = { text -> onQrDecoded(text, false) },
          onError = onPhotoDecodeError,
        )
      }
    }

  val barcodeScanner = remember {
    val options =
      BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    BarcodeScanning.getClient(options)
  }

  val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

  DisposableEffect(Unit) {
    onDispose {
      barcodeScanner.close()
      analysisExecutor.shutdown()
    }
  }

  // Hide tap-to-focus indicator shortly after tapping
  LaunchedEffect(tapFocusOffset) {
    if (tapFocusOffset != null) {
      delay(1200)
      tapFocusOffset = null
    }
  }

  val scannerHeight by
    animateDpAsState(
      targetValue = if (isExpanded) 380.dp else 240.dp,
      label = "scanner_height",
    )

  Surface(
    shape = MaterialTheme.shapes.large,
    color = Color(0xFF0A140E),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(scannerHeight)
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color(0xFF0A140E)),
      ) {
        if (hasCameraPermission && cameraActive && cameraBindError == null) {
          val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

          AndroidView(
            factory = { ctx ->
              PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                previewViewRef = this
                setOnTouchListener { view, event ->
                  if (event.action == MotionEvent.ACTION_UP) {
                    val cam = boundCamera ?: return@setOnTouchListener false
                    val point = meteringPointFactory.createPoint(event.x, event.y)
                    val action =
                      FocusMeteringAction.Builder(
                        point,
                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
                      )
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build()
                    cam.cameraControl.startFocusAndMetering(action)
                    tapFocusOffset = Offset(event.x, event.y)
                    view.performClick()
                  }
                  true
                }
              }
            },
            modifier = Modifier.fillMaxSize(),
          )

          DisposableEffect(lifecycleOwner, previewViewRef) {
            val previewView = previewViewRef
            var provider: ProcessCameraProvider? = null
            if (previewView != null) {
              val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
              cameraProviderFuture.addListener(
                {
                  runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    provider = cameraProvider

                    val hdSelector =
                      ResolutionSelector.Builder()
                        .setResolutionStrategy(
                          ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                          )
                        )
                        .build()

                    val preview =
                      Preview.Builder()
                        .setResolutionSelector(hdSelector)
                        .build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }

                    val imageAnalysis =
                      ImageAnalysis.Builder()
                        .setResolutionSelector(hdSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                      val mediaImage = imageProxy.image
                      if (mediaImage != null) {
                        val inputImage =
                          InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees,
                          )
                        barcodeScanner
                          .process(inputImage)
                          .addOnSuccessListener(mainExecutor) { barcodes ->
                            val firstRaw =
                              barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                            if (!firstRaw.isNullOrBlank()) {
                              onQrDecoded(firstRaw, true)
                            }
                          }
                          .addOnCompleteListener {
                            imageProxy.close()
                          }
                      } else {
                        imageProxy.close()
                      }
                    }

                    val selector =
                      when {
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                          CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                          CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> CameraSelector.DEFAULT_BACK_CAMERA
                      }

                    cameraProvider.unbindAll()
                    val camera =
                      cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageAnalysis,
                      )
                    boundCamera = camera
                    maxZoom = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 3.0f
                    camera.cameraControl.setZoomRatio(zoomLevel.coerceAtMost(maxZoom))
                    camera.cameraControl.enableTorch(torchEnabled)

                    // Trigger crisp initial center autofocus
                    previewView.post {
                      if (previewView.width > 0 && previewView.height > 0) {
                        val centerPoint =
                          previewView.meteringPointFactory.createPoint(
                            previewView.width / 2f,
                            previewView.height / 2f,
                          )
                        val action =
                          FocusMeteringAction.Builder(
                            centerPoint,
                            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
                          )
                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                            .build()
                        camera.cameraControl.startFocusAndMetering(action)
                      }
                    }
                  }.onFailure { err ->
                    cameraBindError =
                      err.localizedMessage ?: "Camera preview unavailable on this device"
                  }
                },
                mainExecutor,
              )
            }
            onDispose {
              runCatching { provider?.unbindAll() }
              boundCamera = null
            }
          }

          ScannerReticleOverlay(modifier = Modifier.fillMaxSize())

          // Tap-to-focus visual ring
          androidx.compose.animation.AnimatedVisibility(
            visible = tapFocusOffset != null,
            enter = fadeIn(),
            exit = fadeOut(),
          ) {
            tapFocusOffset?.let { pos ->
              val ringSizePx = with(LocalDensity.current) { 58.dp.toPx() }
              Box(
                modifier =
                  Modifier
                    .offset {
                      IntOffset(
                        (pos.x - ringSizePx / 2f).roundToInt(),
                        (pos.y - ringSizePx / 2f).roundToInt(),
                      )
                    }
                    .size(58.dp)
                    .border(2.dp, Color(0xFFF5C344), CircleShape),
              )
            }
          }

          // Top camera controls (HD indicator + Refocus + Zoom + Torch + Expand)
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
          ) {
            Surface(
              color = Color.Black.copy(alpha = 0.6f),
              shape = RoundedCornerShape(50),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.QrCodeScanner,
                  contentDescription = null,
                  tint = Color(0xFF8BE8AD),
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Tap to focus • ${String.format(java.util.Locale.US, "%.1fx", zoomLevel)}",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White,
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              // Quick Zoom toggle (1.0x -> 1.5x -> 2.0x)
              Surface(
                onClick = {
                  val next =
                    when {
                      zoomLevel < 1.4f -> 1.5f
                      zoomLevel < 1.9f -> 2.0f
                      else -> 1.0f
                    }.coerceAtMost(maxZoom)
                  zoomLevel = next
                  boundCamera?.cameraControl?.setZoomRatio(next)
                },
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp).testTag("toggle_zoom_button"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = if (zoomLevel >= 1.9f) "2x" else if (zoomLevel >= 1.4f) "1.5x" else "1x",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (zoomLevel > 1.05f) Color(0xFFF5C344) else Color.White,
                  )
                }
              }

              // Center Refocus button
              IconButton(
                onClick = {
                  val cam = boundCamera
                  val pv = previewViewRef
                  if (cam != null && pv != null && pv.width > 0 && pv.height > 0) {
                    val cx = pv.width / 2f
                    val cy = pv.height / 2f
                    val point = pv.meteringPointFactory.createPoint(cx, cy)
                    val action =
                      FocusMeteringAction.Builder(
                        point,
                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
                      )
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build()
                    cam.cameraControl.startFocusAndMetering(action)
                    tapFocusOffset = Offset(cx, cy)
                  }
                },
                modifier =
                  Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("refocus_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.CenterFocusStrong,
                  contentDescription = "Refocus camera",
                  tint = Color.White,
                )
              }

              // Torch toggle
              IconButton(
                onClick = {
                  torchEnabled = !torchEnabled
                  runCatching { boundCamera?.cameraControl?.enableTorch(torchEnabled) }
                },
                modifier =
                  Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("toggle_torch_button"),
              ) {
                Icon(
                  imageVector =
                    if (torchEnabled) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                  contentDescription = if (torchEnabled) "Turn off flash" else "Turn on flash",
                  tint = if (torchEnabled) Color(0xFFF5C344) else Color.White,
                )
              }

              // Expand / Compact viewfinder toggle
              IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier =
                  Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("toggle_expand_scanner_button"),
              ) {
                Icon(
                  imageVector =
                    if (isExpanded) Icons.Filled.CloseFullscreen else Icons.Filled.OpenInFull,
                  contentDescription =
                    if (isExpanded) "Shrink scanner view" else "Expand scanner view",
                  tint = Color.White,
                )
              }
            }
          }
        } else {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(20.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.CameraAlt,
              contentDescription = null,
              tint = Color(0xFF8BE8AD),
              modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text =
                cameraBindError
                  ?: "Camera access lets you scan vehicle QR stickers in real time.",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.9f),
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (!hasCameraPermission) {
              Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.testTag("grant_camera_button"),
              ) {
                Text("Enable Camera Scanner")
              }
            } else if (cameraBindError != null) {
              OutlinedButton(
                onClick = { cameraBindError = null },
                modifier = Modifier.testTag("retry_camera_button"),
              ) {
                Text("Retry Camera", color = Color.White)
              }
            }
          }
        }
      }

      // Bottom Scanner Action Bar (Scan from Photo Picker + Camera toggle)
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
      ) {
        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier =
            Modifier
              .semantics(mergeDescendants = true) {}
              .testTag("scan_sticker_photo_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Scan Sticker Photo")
        }

        if (hasCameraPermission && cameraBindError == null) {
          Button(
            onClick = { cameraActive = !cameraActive },
            modifier = Modifier.testTag("toggle_camera_active_button"),
          ) {
            Text(
              text = if (cameraActive) "Pause Camera" else "Resume Camera",
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ScannerReticleOverlay(modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition(label = "scanner_line")
  val scanProgress by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1900, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "scan_progress",
    )

  Canvas(modifier = modifier) {
    val boxSize = minOf(size.width, size.height) * 0.68f
    val left = (size.width - boxSize) / 2f
    val top = (size.height - boxSize) / 2f
    val right = left + boxSize
    val bottom = top + boxSize
    val cornerLen = boxSize * 0.18f

    // Darken ONLY the outer edges around the QR window so the center scan area stays 100% clear
    val outerScrim = Color.Black.copy(alpha = 0.36f)
    drawRect(color = outerScrim, topLeft = Offset.Zero, size = ComposeSize(size.width, top))
    drawRect(
      color = outerScrim,
      topLeft = Offset(0f, bottom),
      size = ComposeSize(size.width, size.height - bottom),
    )
    drawRect(
      color = outerScrim,
      topLeft = Offset(0f, top),
      size = ComposeSize(left, boxSize),
    )
    drawRect(
      color = outerScrim,
      topLeft = Offset(right, top),
      size = ComposeSize(size.width - right, boxSize),
    )

    drawRoundRect(
      color = Color.White.copy(alpha = 0.25f),
      topLeft = Offset(left, top),
      size = ComposeSize(boxSize, boxSize),
      cornerRadius = CornerRadius(14f, 14f),
      style = Stroke(width = 2f),
    )

    val bracketColor = Color(0xFFF5C344)
    val strokeWidth = 7f
    // Top-left
    drawLine(bracketColor, Offset(left, top + cornerLen), Offset(left, top), strokeWidth, StrokeCap.Round)
    drawLine(bracketColor, Offset(left, top), Offset(left + cornerLen, top), strokeWidth, StrokeCap.Round)
    // Top-right
    drawLine(bracketColor, Offset(right - cornerLen, top), Offset(right, top), strokeWidth, StrokeCap.Round)
    drawLine(bracketColor, Offset(right, top), Offset(right, top + cornerLen), strokeWidth, StrokeCap.Round)
    // Bottom-left
    drawLine(bracketColor, Offset(left, bottom - cornerLen), Offset(left, bottom), strokeWidth, StrokeCap.Round)
    drawLine(bracketColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth, StrokeCap.Round)
    // Bottom-right
    drawLine(bracketColor, Offset(right - cornerLen, bottom), Offset(right, bottom), strokeWidth, StrokeCap.Round)
    drawLine(bracketColor, Offset(right, bottom - cornerLen), Offset(right, bottom), strokeWidth, StrokeCap.Round)

    // Animated scan line
    val lineY = top + (boxSize * 0.1f) + (boxSize * 0.8f * scanProgress)
    drawLine(
      color = Color(0xFF8BE8AD),
      start = Offset(left + 12f, lineY),
      end = Offset(right - 12f, lineY),
      strokeWidth = 3.5f,
      cap = StrokeCap.Round,
    )
  }
}

private fun decodeQrFromPhotoUri(
  context: android.content.Context,
  uri: Uri,
  onDecoded: (String) -> Unit,
  onError: (String) -> Unit,
) {
  runCatching {
    val image = InputImage.fromFilePath(context, uri)
    val options =
      BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val scanner = BarcodeScanning.getClient(options)
    scanner
      .process(image)
      .addOnSuccessListener { barcodes ->
        val raw = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
        scanner.close()
        if (!raw.isNullOrBlank()) {
          onDecoded(raw)
        } else {
          onError("No QR code detected in the selected photo.")
        }
      }
      .addOnFailureListener { err ->
        scanner.close()
        onError(err.localizedMessage ?: "Failed to scan image.")
      }
  }.onFailure { err ->
    onError(err.localizedMessage ?: "Unable to read selected image.")
  }
}
