package com.example.vibe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val vm: AppViewModel = viewModel()
                val user by vm.user.collectAsState()
                if (user == null) {
                    AuthScreen(vm)
                } else {
                    SocialHomeScreen(vm, user!!.uid, user!!.email.orEmpty())
                }
            }
        }
    }
}
