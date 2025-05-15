package com.example.helmetdetector

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.helmetdetector.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val apiService = HelmetDetectionApiService.create()
    private var currentPhotoPath: String? = null
    private var selectedImageUri: Uri? = null

    // Register activity result launchers
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Camera photo saved to file, load it
            currentPhotoPath?.let { path ->
                loadImageFromPath(path)
                selectedImageUri = Uri.fromFile(File(path))
                binding.buttonPredict.isEnabled = true
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                loadImageFromUri(uri)
                selectedImageUri = uri
                binding.buttonPredict.isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Camera button click listener
        binding.buttonCamera.setOnClickListener {
            if (PermissionUtils.hasCameraPermission(this)) {
                dispatchTakePictureIntent()
            } else {
                PermissionUtils.requestCameraPermission(this)
            }
        }

        // Gallery button click listener
        binding.buttonGallery.setOnClickListener {
            if (PermissionUtils.hasStoragePermission(this)) {
                openGallery()
            } else {
                PermissionUtils.requestStoragePermission(this)
            }
        }

        // Predict button click listener
        binding.buttonPredict.setOnClickListener {
            selectedImageUri?.let { uri ->
                uploadImageForPrediction(uri)
            } ?: Toast.makeText(this, R.string.no_image_selected, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionUtils.CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent()
                } else {
                    Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show()
                }
            }
            PermissionUtils.STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, R.string.storage_permission_required, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }

                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    cameraLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun loadImageFromPath(imagePath: String) {
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            binding.imageView.setImageBitmap(bitmap)
        } catch (e: IOException) {
            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadImageFromUri(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .into(binding.imageView)
    }

    private fun uploadImageForPrediction(imageUri: Uri) {
        binding.progressIndicator.visibility = View.VISIBLE
        binding.buttonPredict.isEnabled = false

        Toast.makeText(this, R.string.upload_in_progress, Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get file from URI
                val file = getFileFromUri(imageUri)

                // Create multipart request
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

                // Call API
                val response = apiService.predictHelmet(imagePart)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        // Convert response body to a bitmap and display it
                        val inputStream = response.body()!!.byteStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.imageView.setImageBitmap(bitmap)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.error_api, response.message()),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    binding.progressIndicator.visibility = View.GONE
                    binding.buttonPredict.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.error_api, e.message ?: "Unknown error"),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressIndicator.visibility = View.GONE
                    binding.buttonPredict.isEnabled = true
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun getFileFromUri(uri: Uri): File {
        // For camera photos that already have a file path
        if (uri.scheme == "file") {
            return File(uri.path!!)
        }

        // For gallery images that need to be copied to a temporary file
        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload_", ".jpg", cacheDir)
        tempFile.deleteOnExit()

        inputStream?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }
}