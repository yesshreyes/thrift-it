package com.example.thriftit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import com.example.thriftit.navigation.ThriftItNavGraph
import com.example.thriftit.ui.theme.ThriftitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ThriftitTheme {
                ThriftItNavGraph()
            }
        }
    }
}
