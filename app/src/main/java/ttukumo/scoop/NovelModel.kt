package ttukumo.scoop

import android.content.Context
import androidx.lifecycle.MutableLiveData

data class NovelModel(
    val searchHint: MutableLiveData<String> = MutableLiveData<String>(),
    val honbun: MutableLiveData<String> = MutableLiveData<String>(),
    val url: MutableLiveData<String> = MutableLiveData<String>(),
    val writer: MutableLiveData<String> = MutableLiveData<String>()
)

object NovelModels {
    fun newNovel(context: Context): NovelModel {
        return NovelModel().also {
            ContentsLoader.loadContent(it, context) // Load novel data into it
        }
    }

    fun tutorialPage(context: Context): NovelModel {
        return NovelModel().also {
            ContentsLoader.setTutorial(it, context) // The first default page
        }
    }


}