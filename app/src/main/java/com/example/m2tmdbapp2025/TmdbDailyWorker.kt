package com.example.m2tmdbapp2025

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class TmdbDailyWorker  (context: Context, params: WorkerParameters) : Worker(context, params) {
    private val LOGTAG = TmdbDailyWorker::class.simpleName

    override fun doWork(): Result {
        return Result.success()
    }
}