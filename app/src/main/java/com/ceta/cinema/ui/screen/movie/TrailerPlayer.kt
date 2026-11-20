package com.ceta.cinema.ui.screen.movie

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ceta.cinema.R
import com.ceta.cinema.ui.theme.Ceta

/** The YouTube video id of a watch, youtu.be or embed link; null for anything else (a search page, a blank link). */
internal fun youtubeId(url: String?): String? = YOUTUBE_LINK.find(url.orEmpty().trim())?.groupValues?.get(1)

private val YOUTUBE_LINK = Regex("""^https?://(?:www\.|m\.)?(?:youtube\.com/(?:watch\?(?:[^#]*&)?v=|embed/)|youtu\.be/)([A-Za-z0-9_-]{6,20})(?:[&?#/].*)?$""")

/**
 * The trailer played inside the app: a full-screen dialog around YouTube's embedded player (a WebView, so no new library). If the
 * video's owner does not allow embedding the player reports an error and the dialog offers to open it in YouTube instead.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrailerDialog(videoId: String, title: String, onClose: () -> Unit) {
    val c = Ceta.colors
    val uri = LocalUriHandler.current
    var failed by remember(videoId) { mutableStateOf(false) }
    var loading by remember(videoId) { mutableStateOf(true) }
    val appContext = androidx.compose.ui.platform.LocalContext.current
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        com.ceta.cinema.ui.KeepLocale(appContext) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Column(Modifier.align(Alignment.Center).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                    if (!failed) AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                setBackgroundColor(android.graphics.Color.BLACK)
                                settings.javaScriptEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.domStorageEnabled = true
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) { loading = false }
                                }
                                addJavascriptInterface(object {
                                    @JavascriptInterface fun onError(code: Int) { android.util.Log.w("Trailer", "player error $code"); post { failed = true } }
                                }, "Host")
                                // YouTube checks the embedding page's origin: it must be a real https origin that is not youtube.com itself.
                                loadDataWithBaseURL(PLAYER_ORIGIN, playerPage(videoId), "text/html", "utf-8", null)
                            }
                        },
                        onRelease = { it.loadUrl("about:blank"); it.destroy() },
                    )
                    if (loading && !failed) CircularProgressIndicator(Modifier.align(Alignment.Center), color = c.ac)
                    if (failed) Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(stringResource(R.string.trailer_cannot_embed), color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Text(
                            stringResource(R.string.trailer_open_youtube), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(22.dp)).background(c.ac).clickable { uri.openUri("https://www.youtube.com/watch?v=$videoId") }.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
                Text(title, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
            }
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp).size(44.dp).clip(CircleShape).background(Color(0x66FFFFFF)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        }
    }
}

private const val PLAYER_ORIGIN = "https://cetacinema.app"

/** YouTube's IFrame player: autoplay, inline, no related videos; any playback error is reported back to the dialog. */
private fun playerPage(videoId: String) = """
    <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">
    <style>html,body{margin:0;height:100%;background:#000;overflow:hidden}#p{position:absolute;inset:0}</style></head>
    <body><div id="p"></div>
    <script src="https://www.youtube.com/iframe_api"></script>
    <script>
      function onYouTubeIframeAPIReady() {
        new YT.Player('p', { videoId: '$videoId', width: '100%', height: '100%',
          playerVars: { autoplay: 1, playsinline: 1, rel: 0, fs: 0, modestbranding: 1, origin: '$PLAYER_ORIGIN' },
          events: { onError: function (e) { Host.onError(e.data); } } });
      }
    </script></body></html>
""".trimIndent()
