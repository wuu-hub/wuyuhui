package com.wuyuhui.wordmemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wuyuhui.wordmemo.ui.navigation.WordMemoApp
import com.wuyuhui.wordmemo.ui.theme.WordMemoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as WordMemoApplication).repository
        setContent {
            WordMemoTheme {
                WordMemoApp(repository = repository)
            }
        }
    }
}
