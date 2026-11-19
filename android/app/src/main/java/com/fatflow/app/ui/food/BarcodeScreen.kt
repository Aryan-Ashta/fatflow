package com.fatflow.app.ui.food

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat as Ctx
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fatflow.app.data.local.entity.FoodLogEntity
import com.fatflow.app.data.repo.CachedFood
import com.fatflow.app.di.AppContainer
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.time.LocalDate
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun BarcodeScreen(container: AppContainer, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var permissionDenied by remember { mutableStateOf(false) }
    var product by remember { mutableStateOf<CachedFood?>(null) }
    var grams by remember { mutableStateOf("100") }
    var manualCode by remember { mutableStateOf("") }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionDenied = !granted
        }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    Column(Modifier.padding(16.dp)) {
        when {
            permissionDenied -> {
                Text("Camera permission is required to scan.")
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            },
                        )
                    },
                ) { Text("Open settings") }
            }
            else -> {
                val lifecycleOwner = LocalLifecycleOwner.current
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val executor = Executors.newSingleThreadExecutor()
                        ProcessCameraProvider.getInstance(ctx).addListener({
                            val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val analysis =
                                ImageAnalysis.Builder().build().also { analyzer ->
                                    analyzer.setAnalyzer(executor) { imageProxy ->
                                        val media = imageProxy.image
                                        if (media != null) {
                                            val image =
                                                InputImage.fromMediaImage(
                                                    media,
                                                    imageProxy.imageInfo.rotationDegrees,
                                                )
                                            BarcodeScanning.getClient().process(image)
                                                .addOnSuccessListener { barcodes ->
                                                    val allowed =
                                                        setOf(
                                                            Barcode.FORMAT_EAN_13,
                                                            Barcode.FORMAT_EAN_8,
                                                            Barcode.FORMAT_UPC_A,
                                                            Barcode.FORMAT_UPC_E,
                                                            Barcode.FORMAT_CODE_128,
                                                        )
                                                    val code =
                                                        barcodes.firstOrNull { it.format in allowed }?.rawValue
                                                    if (code != null) {
                                                        scope.launch {
                                                            product = container.foodSearch.product(code)
                                                            cameraProvider.unbindAll()
                                                        }
                                                    }
                                                }
                                        }
                                        imageProxy.close()
                                    }
                                }
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis,
                            )
                        }, Ctx.getMainExecutor(ctx))
                        previewView
                    },
                )
            }
        }
        OutlinedTextField(manualCode, { manualCode = it }, label = { Text("Barcode") })
        Button(
            onClick = {
                scope.launch { product = container.foodSearch.product(manualCode) }
            },
        ) { Text("Look up") }
        product?.let { food ->
            Text(food.name)
            Text("kcal/100g: ${food.kcalPer100g}")
            OutlinedTextField(grams, { grams = it }, label = { Text("Grams") })
            Button(
                onClick = {
                    scope.launch {
                        val g = grams.toDoubleOrNull() ?: 100.0
                        container.foodLogs.log(
                            FoodLogEntity(
                                date = LocalDate.now(),
                                mealName = food.name,
                                calories = (food.kcalPer100g * g / 100.0).roundToInt(),
                                proteinGrams = food.proteinPer100g * g.toFloat() / 100f,
                                carbGrams = food.carbPer100g * g.toFloat() / 100f,
                                fatGrams = food.fatPer100g * g.toFloat() / 100f,
                                source = "BARCODE",
                                barcode = food.barcode,
                                grams = g.toFloat(),
                                isPreWorkout = defaultIsPreWorkout(container),
                            ),
                        )
                        onDone()
                    }
                },
            ) { Text("Save") }
        }
    }
}