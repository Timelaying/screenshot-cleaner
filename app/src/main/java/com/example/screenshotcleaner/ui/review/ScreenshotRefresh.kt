package com.example.screenshotcleaner.ui.review

import com.example.screenshotcleaner.domain.ScreenshotItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

// Called on the UI thread; the owning composition's scope controls its lifetime.
internal class ScreenshotRefresh(
    private val scope: CoroutineScope,
    private val loadScreenshots: suspend (Long) -> List<ScreenshotItem>
) {
    private var refreshJob: Job? = null

    fun refresh(
        ageDays: Long,
        onSuccess: (List<ScreenshotItem>) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val result = try {
                Result.success(loadScreenshots(ageDays))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }
            // A blocking provider may finish after cancellation. Never publish its stale result.
            currentCoroutineContext().ensureActive()
            result.onSuccess(onSuccess).onFailure(onFailure)
        }
    }
}
