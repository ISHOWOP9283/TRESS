package com.example.treemap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.treemap.ui.MainScreen
import com.example.treemap.ui.MainViewModel
import com.example.treemap.ui.theme.TreeMapTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as TreeMapApp
        MainViewModel.Factory(app.repository, app.userRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TreeMapTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
