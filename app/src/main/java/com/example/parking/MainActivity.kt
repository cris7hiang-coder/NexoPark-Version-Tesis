package com.example.parking
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.parking.components.auth.AuthNavigation
import com.example.parking.components.auth.AuthViewModel
import com.example.parking.components.nav.MainNavigationScreen
import com.example.parking.ui.theme.ParkingTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        // 🔥 CACHE FIRESTORE (CLAVE PARA RENDIMIENTO)
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()

        setContent {
            ParkingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                    RequestNotificationPermission()
                }
            }
        }
    }
}
@Composable
fun AppContent(viewModel: AuthViewModel = viewModel()) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    val navController = rememberNavController()
    var lastRole by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.checkUserStatus(context)
    }

    if (showSplash) {
        SplashNexoParkScreen(
            onFinish = {
                showSplash = false
            }
        )
        return
    }

    LaunchedEffect(userRole) {
        if (userRole != null && userRole != lastRole) {
            lastRole = userRole
            navController.navigate("reinit") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        !isLoggedIn -> {
            AuthNavigation(
                onLoginSuccess = { viewModel.setLoginSuccess(context) }
            )
        }

        userRole != null -> {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "reinit",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("reinit") {
                        key(userRole!!) {
                            MainNavigationScreen(
                                role = userRole!!,
                                onLogout = { viewModel.logout(context) }
                            )
                        }
                    }
                }
            }
        }

        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                SplashNexoParkScreen(onFinish = { showSplash = false })
            }
        }
    }
}
@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                Log.d("FCM", if (granted) "Permiso concedido" else "Permiso denegado")
            }
        )

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}



