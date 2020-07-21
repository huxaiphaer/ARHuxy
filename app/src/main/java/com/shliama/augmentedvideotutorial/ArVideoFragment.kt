package com.shliama.augmentedvideotutorial

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.*
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.animation.doOnStart
import androidx.core.graphics.rotationMatrix
import androidx.core.graphics.transform
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import java.io.File
import java.io.IOException
import java.util.*
import java.util.stream.IntStream
import kotlin.collections.HashMap
import kotlin.collections.set
import com.shliama.augmentedvideotutorial.Utils as Utils1

open class ArVideoFragment : ArFragment() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var externalTexture: ExternalTexture
    private lateinit var videoRenderable: ModelRenderable
    private lateinit var videoAnchorNode: VideoAnchorNode
    private lateinit var utils: Utils1
    private lateinit var map: Map<File, File>
    private var progressBar: ProgressBar? = null
    private var loader: LinearLayout? = null
    private var cameraView: FrameLayout? = null

    private var activeAugmentedImage: AugmentedImage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaPlayer = MediaPlayer()

    }

    private fun listAllFiles() {
        val imageNames: MutableList<File> =
            ArrayList()
        val videoNames: MutableList<File> =
            ArrayList()

        val allImagesList: Array<File>
        val allVideosList: Array<File>

        try {
            //Handling Images
            val root = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()
            val allImages = File(root, "NewVisionARImages")
            allImagesList = allImages.listFiles()
            val allVideos =
                context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES + "/NewVisionVideos/")
            allVideosList = allVideos!!.listFiles()
            for (i in allVideosList.indices) {
                videoNames.add(allVideosList[i])
            }
            for (i in allImagesList.indices) {
                imageNames.add(allImagesList[i])
            }
            map = IntStream.range(0, imageNames.size)
                .collect(
                    { HashMap() },
                    { m: java.util.HashMap<File, File>, i: Int ->
                        m[imageNames[i]] = videoNames[i]
                    }
                ) { obj: java.util.HashMap<File, File>, map: java.util.HashMap<File, File>? ->
                    obj.putAll(map!!)
                }
        } catch (e: java.lang.Exception) {
            println("error")
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null)
        arSceneView.planeRenderer.isEnabled = false
        arSceneView.isLightEstimationEnabled = false

        listAllFiles()
        initializeSession()
        createArScene()

        return view
    }

    override fun getSessionConfiguration(session: Session): Config {


        fun loadAugmentedImageBitmap(imageName: String): Bitmap? {
            val file =
                File(imageName)
            val filePath: String = file.path
            return BitmapFactory.decodeFile(filePath)

        }


        fun setupAugmentedImageDatabase(config: Config, session: Session): Boolean {
            try {
                config.augmentedImageDatabase = AugmentedImageDatabase(session).also { db ->

                    for ((i, v) in map) {

                        db.addImage(v.absolutePath, loadAugmentedImageBitmap(i.absolutePath))
                    }

                }
                return true
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Could not add bitmap to augmented image database", e)
            } catch (e: IOException) {
                Log.e(TAG, "IO exception loading augmented image bitmap.", e)
            }
            return false
        }

        return super.getSessionConfiguration(session).also {
            it.lightEstimationMode = Config.LightEstimationMode.DISABLED
            it.focusMode = Config.FocusMode.AUTO

            if (!setupAugmentedImageDatabase(it, session)) {
                Toast.makeText(
                    requireContext(),
                    "Something went wrong while setting up.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun createArScene() {
        // Create an ExternalTexture for displaying the contents of the video.
        externalTexture = ExternalTexture().also {
            mediaPlayer.setSurface(it.surface)
        }

        // Create a renderable with a material that has a parameter of type 'samplerExternal' so that
        // it can display an ExternalTexture.
        ModelRenderable.builder()
            .setSource(requireContext(), R.raw.augmented_video_model)
            .build()
            .thenAccept { renderable ->
                videoRenderable = renderable
                renderable.isShadowCaster = false
                renderable.isShadowReceiver = false
                renderable.material.setExternalTexture("videoTexture", externalTexture)
            }
            .exceptionally { throwable ->
                Log.e(TAG, "Could not create ModelRenderable", throwable)
                return@exceptionally null
            }

        videoAnchorNode = VideoAnchorNode().apply {
            setParent(arSceneView.scene)
        }
    }

    /**
     * In this case, we want to support the playback of one video at a time.
     * Therefore, if ARCore loses current active image FULL_TRACKING we will pause the video.
     * If the same image gets FULL_TRACKING back, the video will resume.
     * If a new image will become active, then the corresponding video will start from scratch.
     */
    override fun onUpdate(frameTime: FrameTime) {
        val frame = arSceneView.arFrame ?: return

        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)

        // If current active augmented image isn't tracked anymore and video playback is started - pause video playback
        val nonFullTrackingImages =
            updatedAugmentedImages.filter { it.trackingMethod != AugmentedImage.TrackingMethod.FULL_TRACKING }
        activeAugmentedImage?.let { activeAugmentedImage ->
            if (isArVideoPlaying() && nonFullTrackingImages.any { it.index == activeAugmentedImage.index }) {
                pauseArVideo()
            }
        }

        val fullTrackingImages =
            updatedAugmentedImages.filter { it.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING }
        if (fullTrackingImages.isEmpty()) return

        // If current active augmented image is tracked but video playback is paused - resume video playback
        activeAugmentedImage?.let { activeAugmentedImage ->
            if (fullTrackingImages.any { it.index == activeAugmentedImage.index }) {
                if (!isArVideoPlaying()) {
                    resumeArVideo()
                }
                return
            }
        }

        // Otherwise - make the first tracked image active and start video playback
        fullTrackingImages.firstOrNull()?.let { augmentedImage ->
            try {
                playbackArVideo(augmentedImage)
            } catch (e: Exception) {
                Log.e(TAG, "Could not play video [${augmentedImage.name}]", e)
            }
        }
    }

    private fun isArVideoPlaying() = mediaPlayer.isPlaying

    private fun pauseArVideo() {
        videoAnchorNode.renderable = null
        mediaPlayer.pause()
    }

    private fun resumeArVideo() {
        mediaPlayer.start()
        fadeInVideo()
    }

    private fun dismissArVideo() {
        videoAnchorNode.anchor?.detach()
        videoAnchorNode.renderable = null
        activeAugmentedImage = null
        mediaPlayer.reset()
    }

    private fun playbackArVideo(augmentedImage: AugmentedImage) {
        Log.d(TAG, "playbackVideo = ${augmentedImage.name}")


        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(
            augmentedImage.name
        )

        val videoWidth =
            metadataRetriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH).toFloatOrNull()
                ?: 0f
        val videoHeight =
            metadataRetriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT).toFloatOrNull()
                ?: 0f
        val videoRotation =
            metadataRetriever.extractMetadata(METADATA_KEY_VIDEO_ROTATION).toFloatOrNull()
                ?: 0f

        // Account for video rotation, so that scale logic math works properly
        val imageSize = RectF(0f, 0f, augmentedImage.extentX, augmentedImage.extentZ)
            .transform(rotationMatrix(videoRotation))

        val videoScaleType = VideoScaleType.CenterCrop

        videoAnchorNode.setVideoProperties(
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            videoRotation = videoRotation,
            imageWidth = imageSize.width(),
            imageHeight = imageSize.height(),
            videoScaleType = videoScaleType
        )

        // Update the material parameters
        videoRenderable.material.setFloat2(
            MATERIAL_IMAGE_SIZE,
            imageSize.width(),
            imageSize.height()
        )
        videoRenderable.material.setFloat2(MATERIAL_VIDEO_SIZE, videoWidth, videoHeight)
        videoRenderable.material.setBoolean(MATERIAL_VIDEO_CROP, VIDEO_CROP_ENABLED)

        mediaPlayer.reset()
        mediaPlayer.setDataSource(augmentedImage.name)

        mediaPlayer.isLooping = true
        mediaPlayer.prepare()
        mediaPlayer.start()



        videoAnchorNode.anchor?.detach()
        videoAnchorNode.anchor = augmentedImage.createAnchor(augmentedImage.centerPose)

        activeAugmentedImage = augmentedImage

        externalTexture.surfaceTexture.setOnFrameAvailableListener {
            it.setOnFrameAvailableListener(null)
            fadeInVideo()
        }
    }

    private fun fadeInVideo() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400L
            interpolator = LinearInterpolator()
            addUpdateListener { v ->
                videoRenderable.material.setFloat(MATERIAL_VIDEO_ALPHA, v.animatedValue as Float)
            }
            doOnStart { videoAnchorNode.renderable = videoRenderable }
            start()
        }
    }

    override fun onPause() {
        super.onPause()
        dismissArVideo()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    companion object {
        private const val TAG = "ArVideoFragment"

        private const val VIDEO_CROP_ENABLED = true

        private const val MATERIAL_IMAGE_SIZE = "imageSize"
        private const val MATERIAL_VIDEO_SIZE = "videoSize"
        private const val MATERIAL_VIDEO_CROP = "videoCropEnabled"
        private const val MATERIAL_VIDEO_ALPHA = "videoAlpha"
    }
}