package com.nercare.cogcare.presentation.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.nercare.cogcare.BuildConfig
import com.nercare.cogcare.data.repository.CaregiverAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nercare.cogcare.presentation.theme.*

data class CaregiverAuthState(val loading: Boolean = false, val error: String? = null, val signedIn: Boolean = false)

@HiltViewModel
class CaregiverAuthViewModel @Inject constructor(private val repository: CaregiverAuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(CaregiverAuthState(signedIn = repository.currentCaregiverId != null))
    val state = _state.asStateFlow()

    fun submit(email: String, password: String, create: Boolean) = viewModelScope.launch {
        _state.value = CaregiverAuthState(loading = true)
        val result = if (create) repository.createAccount(email, password) else repository.signIn(email, password)
        _state.value = result.fold({ CaregiverAuthState(signedIn = true) }, { CaregiverAuthState(error = it.localizedMessage ?: "Authentication failed") })
    }

    fun google(context: Context) = viewModelScope.launch {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            _state.value = CaregiverAuthState(error = "Google sign-in needs GOOGLE_WEB_CLIENT_ID in local.properties")
            return@launch
        }
        _state.value = CaregiverAuthState(loading = true)
        runCatching {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        }.fold(
            onSuccess = { token -> repository.signInWithGoogle(token).fold({ _state.value = CaregiverAuthState(signedIn = true) }, { _state.value = CaregiverAuthState(error = it.localizedMessage) }) },
            onFailure = { _state.value = CaregiverAuthState(error = it.localizedMessage ?: "Google sign-in failed") }
        )
    }
}

@Composable
fun CaregiverAuthScreen(onSuccess: () -> Unit, onBack: () -> Unit, viewModel: CaregiverAuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var create by remember { mutableStateOf(false) }
    LaunchedEffect(state.signedIn) { if (state.signedIn) onSuccess() }
    Column(Modifier.fillMaxSize().background(BackgroundCream).verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.Center) {
        TextButton(onClick = onBack) { Text("Back") }
        Text(if (create) "Create caregiver account" else "Caregiver sign in", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PrimaryGreen)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { viewModel.submit(email, password, create) }, enabled = email.isNotBlank() && password.length >= 6 && !state.loading, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text(if (create) "Create account" else "Sign in")
        }
        OutlinedButton(onClick = { viewModel.google(context) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(54.dp)) { Text("Continue with Google") }
        TextButton(onClick = { create = !create }, modifier = Modifier.fillMaxWidth()) { Text(if (create) "Already registered? Sign in" else "New caregiver? Create account") }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}
