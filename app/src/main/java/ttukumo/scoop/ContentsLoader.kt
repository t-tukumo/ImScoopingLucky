package ttukumo.scoop

import android.content.Context
import androidx.preference.PreferenceManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import kotlin.coroutines.CoroutineContext
import kotlin.math.min
import kotlin.random.Random

const val MAX_SEEK = 2000 // 2000はなろうAPIのシーク限度

object ContentsLoader : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job


    fun loadContent(container: NovelModel, context: Context) {
        launch {
            try {
                loadPage(container, context)
            } catch (e: Exception) {
                container.searchHint.postValue("エラー")
                container.honbun.postValue(e.stackTrace.joinToString("\n"))
                e.printStackTrace()

            }
        }
    }

    fun setTutorial(container: NovelModel, context: Context) {
        GlobalScope.launch {
            try {
                container.searchHint.postValue(context.getString(R.string.start_guide))
                container.honbun.postValue(context.getString(R.string.start_page_text))

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var allcount = 0

    // なろう小説API https://dev.syosetu.com/man/api/
    // TODO 例外・タイムアウト処理・グローバルスコープなんとかすること
    private suspend fun loadPage(container: NovelModel, context: Context) =

        withContext(Dispatchers.IO)
        {

            container.searchHint.postValue(context.getString(R.string.searching))

            val (queryParameters, queryParametersViewing) = buildParameters(context)

            var url =
                "https://api.syosetu.com/novelapi/api/?out=json&of=n$queryParameters&lim=1"
            Logs.d(url)
            url.run {
                val (_, response, result) = this.httpGet().responseJson()
                if (result is Result.Failure) {
                    Logs.e(result.error.message ?: "null")
                    container.honbun.postValue(response.toString())
                    return@withContext
                }
                val jsonArray = (result as Result.Success).value.array()

                allcount = jsonArray.getJSONObject(0).getInt("allcount")
            }

            if (allcount == 0) {
                val s = StringBuilder()
                    .append(context.getString(R.string.not_found)).appendln()
                    .append(queryParametersViewing).appendln()

                container.honbun.postValue(s.toString())
                return@withContext
            }

            // 検索結果のランダムな位置の小説情報を得る。 stは取得開始位置。
            val st = 1 + Random.nextInt(min(allcount, MAX_SEEK))

            url =
                "https://api.syosetu.com/novelapi/api/?out=json&of=n-w-ga&$queryParameters&st=$st&lim=1"
            Logs.d(url)
            url.run {
                val (_, response, result) = this.httpGet().responseJson()
                if (result is Result.Failure) {
                    Logs.e(result.error.message ?: "null")
                    container.honbun.postValue(response.toString())
                    return@withContext
                }
                val jsonArray = (result as Result.Success).value.array()

                Logs.i(jsonArray.toString()) // log the json response

                //1つ目のオブジェクトにはallcountが入るのでインデックス1以降が個々の小説情報になる。
                val info = jsonArray.getJSONObject(1)
                val ncode = info.getString("ncode")
                val generalAllNo = info.getInt("general_all_no")
                val writer = info.getString("writer")
                val novelPageUrl =
                    if (generalAllNo == 1) {//短編の場合ページ数無し
                        "https://ncode.syosetu.com/$ncode/"
                    } else {
                        val page = randomPage(context, generalAllNo)
                        "https://ncode.syosetu.com/$ncode/$page/"
                    }

                val hint = context.getString(
                    R.string.hit_counter,
                    min(allcount, MAX_SEEK),
                    MAX_SEEK
                )

                container.searchHint.postValue(
                    hint + "\n" + context.getString(R.string.now_loading)
                )
                val document = Jsoup.connect(novelPageUrl).get()
                val honbun =
                    document.select("#novel_honbun")
                        .first() // <div id="novel_honbun"> .. </> を取得//TODO 初回2ページ目の読み込み時にNullになることがある。読み切っていないうちにパースしてる疑い。
                        .getElementsByTag("p") //<p id="L1"> 各行のテキスト </p> を取得
                        .asSequence().map { it.text() }//各行のテキストを取り出し
                        .joinTo(buffer = StringBuffer(), separator = "\n").toString() //改行で結合

                container.searchHint.postValue(hint)
                container.honbun.postValue(honbun)
                container.url.postValue(novelPageUrl)
                container.writer.postValue(context.getString(R.string.writer) + ": " + writer)

            }
        }

    private fun randomPage(context: Context, generalAllNo: Int): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val excludesEnding =
            prefs.getBoolean(
                context.getString(R.string.prefs_key_exclude_ending), false
            )
        return 1 + Random.nextInt(
            if (excludesEnding) {
                (generalAllNo * 0.7).toInt()
            } else {
                generalAllNo
            }
        )
    }

    private fun buildParameters(context: Context): Pair<String, String> {
        with(context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            Logs.d("preferences" + prefs.all.keys)

            // ジャンル条件
            val (genreConditionParameter, genreDescription) = getGenreFilterParameterAndDescription()

            val minlen =
                prefs.getString(getString(R.string.prefs_key_total_char_count_min), "0")

            //登録必須タグ条件群
            var isTensei = threeValueParameter(R.string.prefs_key_tensei, "tensei")
            var isTenni = threeValueParameter(R.string.prefs_key_tenni, "tenni")
            var isTenseiTenni = threeValueParameter(R.string.prefs_key_tensei_tenni, "tt")
            if (isTenseiTenni == "&nottt=1") {
                isTenseiTenni = ""
                isTensei = "&nottensei=1"
                isTenni = "&nottenni=1"
            }
            val paramsB = arrayOf(
                threeValueParameter(R.string.prefs_key_r15, "r15"),
                threeValueParameter(R.string.prefs_key_bl, "bl"),
                threeValueParameter(R.string.prefs_key_gl, "gl"),
                threeValueParameter(R.string.prefs_key_zankoku, "zankoku"),
                isTensei,
                isTenni,
                isTenseiTenni
            )

            // create ?parameters
            val queryParameters = StringBuilder()
                .append("&minlen=$minlen")
            for (param in paramsB) {
                queryParameters.append(param)
            }
            queryParameters
                .append(genreConditionParameter)
                .append("&order=new")

            val queryParametersLog =
                "\n" + getString(R.string.min_all_len_title) + ":" + minlen + "\n" +
                        genreDescription + " \n" + getString(R.string.omitted_below)
            Logs.d(queryParametersLog)

            return queryParameters.toString() to queryParametersLog
        }
    }

    private fun Context.threeValueParameter(
        keyResId: Int,
        paramBase: String
    ): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return when (prefs.getString(this.getString(keyResId), "0")) {
            "1" -> "&is$paramBase=1"
            "-1" -> "&not$paramBase=1"
            else -> ""
        }
    }

    private fun Context.getGenreFilterParameterAndDescription(): Pair<String, String> {
        val allGenreValues = resources.getStringArray(R.array.genre_values)
        val allGenreValuesSet = allGenreValues.toSet()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val filtersGenre = prefs.getBoolean(getString(R.string.prefs_key_filters_genre), false)
        if (!filtersGenre) return Pair("", "")

        val genreCondition = prefs.getStringSet(
            getString(R.string.prefs_key_target_genre_flags),
            allGenreValuesSet
        )!!

        //許容ジャンルの数に応じてジャンル不問、OR検索、NOR検索使い分け
        if (genreCondition.size == allGenreValuesSet.size) return Pair(
            ""/*parameter*/,
            ""/*description*/
        )

        val allGenreEntries =
            resources.getStringArray(R.array.genre_entries) zip allGenreValues
        val genreNames =
            allGenreEntries.filter { genreCondition.contains(it.second) }.map { it.first }
        val description =
            getString(R.string.genre_title) + "\n" + genreNames.joinTo(StringBuffer(), "\n")

        val param =
            if (genreCondition.size < 11) {
                "&genre=" + genreCondition.joinTo(StringBuffer(), "-")
            } else {
                "&notgenre=" + allGenreValuesSet.subtract(genreCondition)
                    .joinTo(StringBuffer(), "-")
            }

        return Pair(param, description)
    }


}

