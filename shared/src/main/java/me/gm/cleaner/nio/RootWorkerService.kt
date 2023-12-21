package me.gm.cleaner.nio

import com.google.flatbuffers.FlatBufferBuilder
import me.gm.cleaner.browser.IProgressListener
import me.gm.cleaner.browser.IRootWorkerService
import me.gm.cleaner.model.ParceledException
import me.gm.cleaner.nio.fs.StructStatPath
import java.io.InterruptedIOException
import java.io.RandomAccessFile
import java.net.URI
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.io.path.toPath

class RootWorkerService : IRootWorkerService.Stub() {
    private val activeWorks: MutableSet<String> = CopyOnWriteArraySet()

    override fun cancelWork(uuid: String) {
        activeWorks.remove(uuid)
    }

    private fun decode(uriString: String): Path = URI.create(uriString).toPath()

    override fun delete(uuid: String, listener: IProgressListener, file: String) {
        activeWorks.add(uuid)
        try {
            val limiter = CallbackRateLimiter()
            decode(file).deleteRecursivelyInterruptable {
                val progress = 0F
                if (limiter.tryTriggerCallback { listener.onProgress(progress) } &&
                    !activeWorks.contains(uuid)) {
                    throw InterruptedIOException()
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            listener.onException(ParceledException.create(e))
        } finally {
            activeWorks.remove(uuid)
        }
    }

    override fun copy(
        uuid: String, listener: IProgressListener, source: String, target: String
    ) {
        TODO("Not yet implemented")
    }

    override fun move(
        uuid: String, listener: IProgressListener, source: String, target: String
    ) {
        TODO("Not yet implemented")
    }

    override fun snapshot(
        uuid: String, listener: IProgressListener, pendingSnapshotFile: String, path: String
    ): Boolean {
        activeWorks.add(uuid)
        return try {
            val limiter = CallbackRateLimiter()
            RandomAccessFile(
                decode(pendingSnapshotFile).toFile(), "rw"
            ).channel.use { channel ->
                val initialSize = 200L * 1000L * 1000L // 200 MB
                val buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, initialSize)
                val fb = FlatBufferBuilder(buffer)
                // truncate head
                val dataStart = initialSize - fb.offset()
                channel.transferTo(dataStart, fb.offset().toLong(), channel)
                channel.truncate(fb.offset().toLong())
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            listener.onException(ParceledException.create(e))
            false
        } finally {
            activeWorks.remove(uuid)
        }
    }

    // @see AlertRateLimiter
    // System's limit interval is 1000. Use a slightly larger value to avoid being muted.
    class CallbackRateLimiter(private val interval: Long = 1100L) {
        private var lastCallbackTime: Long = 0L

        @Synchronized
        fun tryTriggerCallback(callback: Runnable): Boolean {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastCallbackTime >= interval) {
                lastCallbackTime = currentTime
                callback.run()
                return true
            }
            return false
        }
    }
}
