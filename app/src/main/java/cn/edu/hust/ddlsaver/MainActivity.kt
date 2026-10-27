package cn.edu.hust.ddlsaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.edu.hust.ddlsaver.ui.SaverApp
import cn.edu.hust.ddlsaver.ui.SaverTheme
import cn.edu.hust.ddlsaver.ui.SaverViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent {
            SaverTheme {
                val model: SaverViewModel = viewModel(factory = SaverViewModel.factory(application))
                SaverApp(model)
            }
        }
    }
}
