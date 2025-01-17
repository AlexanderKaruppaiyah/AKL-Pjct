package com.example.slt

import android.animation.Keyframe
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.sceneview.Scene
import io.github.sceneview.animation.AnimatableModel
import io.github.sceneview.animation.ModelAnimation
import io.github.sceneview.animation.ModelAnimator
import io.github.sceneview.model.model
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader

class TTSActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TTSContent()
        }
    }

    @Composable
    fun TTSContent() {
        Box(modifier = Modifier.fillMaxSize()) {
            val engine = rememberEngine()
            val modelLoader = rememberModelLoader(engine)
            val modelNode = ModelNode(
                modelInstance = modelLoader.createModelInstance("models/boy1.glb"),
                scaleToUnits = 0.5f
            )

            Scene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                childNodes = listOf(modelNode)
            )


        }
    }
}
