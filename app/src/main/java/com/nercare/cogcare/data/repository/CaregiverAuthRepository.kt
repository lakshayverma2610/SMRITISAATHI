package com.nercare.cogcare.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaregiverAuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    val currentCaregiverId: String? get() = auth.currentUser?.uid

    suspend fun signIn(email: String, password: String): Result<String> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await().user?.uid
            ?: error("Sign-in did not return an account")
    }

    suspend fun createAccount(email: String, password: String): Result<String> = runCatching {
        auth.createUserWithEmailAndPassword(email.trim(), password).await().user?.uid
            ?: error("Account creation did not return an account")
    }

    suspend fun signInWithGoogle(idToken: String): Result<String> = runCatching {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user?.uid
            ?: error("Google sign-in did not return an account")
    }

    fun signOut() = auth.signOut()
}
