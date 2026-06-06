package com.example.generator

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

data class VerseData(val text: String, val translation: String?, val audioPath: String, val durationUs: Long)

class VideoGenerator {

    private val client = OkHttpClient()

    suspend fun generateReel(
        context: Context,
        surah: Int,
        startAyah: Int,
        endAyah: Int,
        reciterId: String,
        showTranslation: Boolean,
        pexelsApiKey: String,
        onProgress: (String, Float) -> Unit,
        onComplete: (Uri) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val verses = mutableListOf<VerseData>()
            val totalAyahs = endAyah - startAyah + 1
            
            // 1. Fetch Background Image if Pexels API Key is provided
            var bgBitmap: Bitmap? = null
            if (pexelsApiKey.isNotBlank()) {
                onProgress("جاري تحميل الخلفية...", 0.05f)
                val bgFile = File(context.cacheDir, "bg_image.jpg")
                try {
                    val request = Request.Builder()
                        .url("https://api.pexels.com/v1/search?query=nature+night+sky&orientation=portrait&per_page=15")
                        .addHeader("Authorization", pexelsApiKey)
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val photos = json.getJSONArray("photos")
                        if (photos.length() > 0) {
                            val randomPhoto = photos.getJSONObject((0 until photos.length()).random())
                            val imgUrl = randomPhoto.getJSONObject("src").getString("large2x")
                            downloadAudio(imgUrl, bgFile) // Reusing download logic
                            bgBitmap = android.graphics.BitmapFactory.decodeFile(bgFile.absolutePath)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // fallback to null
                }
            }
            
            for (i in 0 until totalAyahs) {
                val ayah = startAyah + i
                onProgress("جاري تحميل الآية \$ayah...", 0.1f + (i * 0.4f / totalAyahs))
                
                val text = fetchVerseText(surah, ayah, "quran-uthmani")
                val translation = if (showTranslation) fetchVerseText(surah, ayah, "en.asad") else null

                val audioFileName = String.format("%03d%03d.mp3", surah, ayah)
                val url = "https://everyayah.com/data/\$reciterId/\$audioFileName"
                val destFile = File(context.cacheDir, audioFileName)
                
                downloadAudio(url, destFile)
                
                val ext = MediaExtractor().apply { setDataSource(destFile.absolutePath) }
                ext.selectTrack(0)
                var durationUs = ext.getTrackFormat(0).getLong(MediaFormat.KEY_DURATION, -1L)
                if (durationUs <= 0) {
                    var maxTs = 0L
                    val bb = ByteBuffer.allocate(256)
                    while (ext.readSampleData(bb, 0) >= 0) {
                        maxTs = ext.sampleTime
                        ext.advance()
                    }
                    durationUs = maxTs
                }
                ext.release()
                verses.add(VerseData(text, translation, destFile.absolutePath, durationUs))
            }
            
            onProgress("جاري إنشاء الفيديو...", 0.5f)
            
            if (verses.isEmpty()) throw Exception("لا توجد آيات")
            
            val outputPath = File(context.cacheDir, "quran_reel_\${System.currentTimeMillis()}.mp4").absolutePath
            val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            var videoTrackIdx = -1
            var audioTrackIdx = -1
            val muxerStarted = java.util.concurrent.atomic.AtomicBoolean(false)
            
            val audioFormat = MediaExtractor().apply { setDataSource(verses[0].audioPath) }.apply { selectTrack(0) }.getTrackFormat(0)
            
            val videoFormat = MediaFormat.createVideoFormat("video/avc", 1080, 1920).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, 2000000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            
            val videoCodec = MediaCodec.createEncoderByType("video/avc")
            videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            videoCodec.start()
            
            val drainLatch = CountDownLatch(1)
            
            val drainThread = thread {
                try {
                    val bufferInfo = MediaCodec.BufferInfo()
                    while (true) {
                        val outIdx = videoCodec.dequeueOutputBuffer(bufferInfo, 10000)
                        if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            val vf = videoCodec.outputFormat
                            videoTrackIdx = muxer.addTrack(vf)
                            audioTrackIdx = muxer.addTrack(audioFormat)
                            muxer.start()
                            muxerStarted.set(true)
                        } else if (outIdx >= 0) {
                            val buf = videoCodec.getOutputBuffer(outIdx)!!
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0 && muxerStarted.get()) {
                                buf.position(bufferInfo.offset)
                                buf.limit(bufferInfo.offset + bufferInfo.size)
                                synchronized(muxer) {
                                    muxer.writeSampleData(videoTrackIdx, buf, bufferInfo)
                                }
                            }
                            videoCodec.releaseOutputBuffer(outIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    drainLatch.countDown()
                }
            }
            
            val audioThread = thread {
                try {
                    var audioPtsUs = 0L
                    for (verse in verses) {
                        val ext = MediaExtractor().apply { setDataSource(verse.audioPath) }
                        ext.selectTrack(0)
                        val buf = ByteBuffer.allocate(1024 * 1024)
                        val info = MediaCodec.BufferInfo()
                        while (true) {
                            val size = ext.readSampleData(buf, 0)
                            if (size < 0) break
                            val pts = ext.sampleTime
                            info.offset = 0
                            info.size = size
                            info.flags = if ((ext.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                                MediaCodec.BUFFER_FLAG_KEY_FRAME
                            } else 0
                            info.presentationTimeUs = audioPtsUs + pts
                            
                            while (!muxerStarted.get() && drainLatch.count > 0) { Thread.sleep(10) }
                            if (muxerStarted.get()) {
                                synchronized(muxer) {
                                    muxer.writeSampleData(audioTrackIdx, buf, info)
                                }
                            }
                            ext.advance()
                        }
                        audioPtsUs += verse.durationUs
                        ext.release()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            var videoPtsUs = 0L
            val fps = 2
            val frameDurationUs = 1000000L / fps
            
            for ((idx, verse) in verses.withIndex()) {
                onProgress("جاري إنشاء مشهد الآية \${startAyah + idx}...", 0.5f + (idx * 0.4f / verses.size))
                val bitmap = createVerseBitmap(verse.text, verse.translation, bgBitmap, context)
                val framesNeeded = Math.max(1, (verse.durationUs / frameDurationUs).toInt() + 1)
                
                for (i in 0 until framesNeeded) {
                    val inIdx = videoCodec.dequeueInputBuffer(-1)
                    val img = videoCodec.getInputImage(inIdx)!!
                    fillImageFromBitmap(img, bitmap)
                    videoCodec.queueInputBuffer(inIdx, 0, img.planes[0].buffer.capacity() * 3/2, videoPtsUs, 0)
                    videoPtsUs += frameDurationUs
                }
                bitmap.recycle()
            }
            
            val eosIdx = videoCodec.dequeueInputBuffer(-1)
            videoCodec.queueInputBuffer(eosIdx, 0, 0, videoPtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            
            drainLatch.await(3, TimeUnit.MINUTES)
            audioThread.join(10000)
            
            muxer.stop()
            muxer.release()
            videoCodec.stop()
            videoCodec.release()
            bgBitmap?.recycle()
            
            onProgress("جاري الحفظ في المعرض...", 0.95f)
            
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "Quran_Reel_\${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/QuranReels")
            }
            val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    File(outputPath).inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                File(outputPath).delete()
                withContext(Dispatchers.Main) { onComplete(uri) }
            } else {
                throw Exception("فشل في حفظ الفيديو")
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { onError(e.message ?: "حدث خطأ غير معروف") }
        }
    }

    private fun fetchVerseText(surah: Int, ayah: Int, edition: String): String {
        val url = "https://api.alquran.cloud/v1/ayah/\$surah:\$ayah/\$edition"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("فشل تحميل النص")
        val body = response.body?.string() ?: ""
        val json = JSONObject(body)
        return json.getJSONObject("data").getString("text")
    }

    private fun downloadAudio(url: String, destFile: File) {
        if (destFile.exists() && destFile.length() > 0) return
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("فشل تحميل الصوت")
        response.body?.byteStream()?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun createVerseBitmap(text: String, translation: String?, bgBitmap: Bitmap?, context: Context): Bitmap {
        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        if (bgBitmap != null) {
            val src = android.graphics.Rect(0, 0, bgBitmap.width, bgBitmap.height)
            val dst = android.graphics.Rect(0, 0, 1080, 1920)
            canvas.drawBitmap(bgBitmap, src, dst, null)
            canvas.drawColor(Color.argb(128, 0, 0, 0)) // Dark overlay for readability
        } else {
            canvas.drawColor(Color.BLACK)
        }
        
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(5f, 0f, 0f, Color.BLACK)
        }
        
        var textSize = 150f
        var sl: StaticLayout
        while (true) {
            textPaint.textSize = textSize
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sl = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, 960)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                sl = StaticLayout(text, textPaint, 960, Layout.Alignment.ALIGN_CENTER, 1f, 0f, false)
            }
            if (sl.height < 960 || textSize <= 40f) break
            textSize -= 5f
        }
        
        val transPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.YELLOW
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
            this.textSize = 50f
            setShadowLayer(5f, 0f, 0f, Color.BLACK)
        }
        
        var transSl: StaticLayout? = null
        if (translation != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                transSl = StaticLayout.Builder.obtain(translation, 0, translation.length, transPaint, 960)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                transSl = StaticLayout(translation, transPaint, 960, Layout.Alignment.ALIGN_CENTER, 1f, 0f, false)
            }
        }

        val totalHeight = sl.height + (transSl?.height?.plus(60f) ?: 0f)
        val startY = (1920f - totalHeight) / 2f
        
        canvas.save()
        canvas.translate(540f, startY)
        sl.draw(canvas)
        canvas.restore()
        
        if (transSl != null) {
            canvas.save()
            canvas.translate(540f, startY + sl.height + 60f)
            transSl.draw(canvas)
            canvas.restore()
        }
        
        return bitmap
    }

    private fun fillImageFromBitmap(image: Image, bitmap: Bitmap) {
        val width = image.width
        val height = image.height
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)
        
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        
        val yRowStride = yPlane.rowStride
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride
        
        yBuffer.clear()
        uBuffer.clear()
        vBuffer.clear()
        
        val yBytes = ByteArray(width)
        var index = 0
        
        for (r in 0 until height) {
            for (c in 0 until width) {
                val color = argb[index++]
                val rCol = (color and 0xff0000) shr 16
                val gCol = (color and 0xff00) shr 8
                val bCol = (color and 0xff) shr 0
                
                var Y = ((66 * rCol + 129 * gCol + 25 * bCol + 128) shr 8) + 16
                Y = Y.coerceIn(0, 255)
                yBytes[c] = Y.toByte()

                if (r % 2 == 0 && c % 2 == 0) {
                    var U = ((-38 * rCol - 74 * gCol + 112 * bCol + 128) shr 8) + 128
                    var V = ((112 * rCol - 94 * gCol - 18 * bCol + 128) shr 8) + 128
                    U = U.coerceIn(0, 255)
                    V = V.coerceIn(0, 255)
                    
                    val cHalf = c / 2
                    val uPos = (r / 2) * uRowStride + cHalf * uPixelStride
                    val vPos = (r / 2) * vRowStride + cHalf * vPixelStride
                    
                    uBuffer.position(uPos)
                    uBuffer.put(U.toByte())
                    
                    vBuffer.position(vPos)
                    vBuffer.put(V.toByte())
                }
            }
            yBuffer.position(r * yRowStride)
            yBuffer.put(yBytes)
        }
    }
}
