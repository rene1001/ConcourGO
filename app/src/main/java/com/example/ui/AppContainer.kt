package com.example.ui

import android.content.Context
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.payment.BictorysApiService
import com.example.data.payment.BictorysPaymentRepository
import com.example.data.remote.CloudSyncRepository
import com.example.data.repository.ConcoursRepository
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    val preferencesRepository = PreferencesRepository(appContext)
    private val firestoreDb: FirebaseFirestore = FirebaseFirestore.getInstance(
        appContext.getString(R.string.firestore_database_id)
    )
    private val cloudSyncRepository = CloudSyncRepository(firestoreDb)
    val concoursRepository = ConcoursRepository(
        context = appContext,
        dao = database.concoursDao(),
        preferencesRepository = preferencesRepository,
        cloudSyncRepository = cloudSyncRepository
    )
    private val bictorysApiService = BictorysApiService.create()
    val bictorysPaymentRepository = BictorysPaymentRepository(
        apiService = bictorysApiService,
        preferencesRepository = preferencesRepository,
        concoursRepository = concoursRepository
    )

    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context).also { INSTANCE = it }
            }
        }
    }
}
