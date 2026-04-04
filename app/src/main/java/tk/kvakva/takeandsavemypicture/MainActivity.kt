package tk.kvakva.takeandsavemypicture

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import tk.kvakva.takeandsavemypicture.ui.theme.TakeAndSaveMyPictureTheme

private const val TAG = "MainActivity"


class MainActivity : ComponentActivity() {

    private val mainActivityViewModel by viewModels<MainActivityViewModel>()

    private var editImgLauncher = registerForActivityResult(
        ActivityResultContracts
            .StartActivityForResult()
    ) {
        Log.i(TAG, "editMyPic: resultCode: ${it.resultCode}")
        Log.i(TAG, "editMyPic: data,data: ${it.data?.data}")
        Log.i(TAG, "editMyPic: data: ${it.data}")
        Log.i(TAG, "editMyPic: data.scheme: ${it.data?.scheme}")
    }

    private var dirpicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) {
            Log.i(TAG, "No catalog selected")
            Toast.makeText(this, "No catalog selected", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        Log.i(TAG, "selected: $uri")
        mainActivityViewModel.saveLocalUri("$uri")

        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
// Check for the freshest data.
        contentResolver.takePersistableUriPermission(uri, takeFlags)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Force the 3-button navigation bar to be transparent
            // See: https://developer.android.com/develop/ui/views/layout/edge-to-edge#create-transparent
            window.isNavigationBarContrastEnforced = false
        }

        Log.e(
            TAG,
            "onCreate: versionCode: ${BuildConfig.VERSION_CODE}, versionName: ${BuildConfig.VERSION_NAME}"
        )
        setContent {
            TakeAndSaveMyPictureTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Greeting()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun Greeting() {
        val isRefreshing by mainActivityViewModel.isRefreshing.collectAsState()
        var displaydialog by remember { mutableStateOf(false) }

        val refreshState = rememberPullRefreshState(isRefreshing, {
            mainActivityViewModel.isRefreshing.value = true
            if (mainActivityViewModel.ipcamurl.value.isNullOrBlank() and mainActivityViewModel.webcamurl.value.isNullOrBlank()) {
                displaydialog = true
                Log.i(
                    TAG,
                    "Greeting: Swiped but no urls found in settings"
                )
                return@rememberPullRefreshState
            }
            Log.i(TAG, "Greeting: Swiped")
            mainActivityViewModel.download()
        })

        val dlinkfilename by mainActivityViewModel.dlinkcamfilename.observeAsState()
        val webcamfilename by mainActivityViewModel.webcamfilename.observeAsState()
        var displayTopBar by remember { mutableStateOf(true) }
        val view = LocalView.current

//        SideEffect {
//            val window = (view.context as MainActivity).window
//            val controller = WindowCompat.getInsetsController(window, view)
//
//            controller.let {
//                // it.hide(WindowInsetsCompat.Type.statusBars())  // Скрыть status bar
//                it.systemBarsBehavior =
//                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//            }
//        }

        val urldl: String? by mainActivityViewModel.ipcamurl.observeAsState()
        val urlwc: String? by mainActivityViewModel.webcamurl.observeAsState()
        val localuri: String? by mainActivityViewModel.localuri.observeAsState()
        val imgbmipc: ImageBitmap? by mainActivityViewModel.dlinkibm.observeAsState()
        val imgbmweb: ImageBitmap? by mainActivityViewModel.webcamibm.observeAsState()
        //remember { mutableStateOf("") }

        Scaffold(
            topBar = {
                if (displayTopBar) TopAppBar(
                    title = {
                        Text(stringResource(R.string.app_name))
                    },
                    navigationIcon = {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(Icons.Filled.Menu, contentDescription = null)
                        }
                    },
                    elevation = 2.dp,
                    actions = {
                        IconButton(
                            onClick = {
                                dirpicker.launch(null)
                            },
                        ) {
                            Icon(
                                // Icons.Filled.Create,
                                painterResource(R.drawable.ic_twotone_folder_24),
                                contentDescription = "Settings",
                            )
                        }
                        IconButton(
                            onClick = {
                                displaydialog = !displaydialog
                            }
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            },
            modifier = Modifier
                .safeDrawingPadding(),
        ) { p ->
            if (displaydialog) {
                AlertDialog(
                    {
                        mainActivityViewModel.updateUrlsFromDataStore()
                        displaydialog = false
                    },
                    confirmButton = {
                        Button(
                            {
                                mainActivityViewModel.saveUrls()
                                Log.i(TAG, "Greeting: button clicked on alertdialog")
                                displaydialog = false
                            }
                        ) {
                            Text("ok")
                        }
                    },
                    modifier = Modifier.padding(p),
                    dismissButton = {
                        Button(
                            {
                                mainActivityViewModel.updateUrlsFromDataStore()
                                displaydialog = false
                            },
                        ) {
                            Text("cancel")
                        }
                    },
                    title = { Text(text = "Urls") },
                    text = {
                        Column {
                            Text("Local Uri:")
                            Text(localuri ?: "Local Uri has not set")
                            Text("set urls:")
                            Divider(
                                Modifier.padding(2.dp),
                                thickness = 2.dp,
                                color = Color.Gray
                            )
                            TextField(
                                value = urldl ?: "",
                                onValueChange = { mainActivityViewModel.ipcamurl.postValue(it) },
                                label = { Text("IP Camera URL") }
                            )
                            Divider(
                                Modifier.padding(2.dp),
                                thickness = 2.dp,
                                color = Color.Gray
                            )
                            TextField(
                                value = urlwc ?: "",
                                onValueChange = { mainActivityViewModel.webcamurl.postValue(it) },
                                label = { Text("Web Camera URL") },

                                )
                            Divider(
                                Modifier.padding(2.dp),
                                thickness = 2.dp,
                                color = Color.Gray
                            )
                        }
                    }
                )
            }
            Box(
                modifier = Modifier
                    .padding(p)
                    .pullRefresh(refreshState),
            ) {
                LazyColumn(
                    Modifier
                        // .verticalScroll(ScrollState(0))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    displayTopBar = !displayTopBar
                                    val controller = WindowCompat.getInsetsController(
                                        (view.context as MainActivity).window,
                                        view
                                    )

                                    if (displayTopBar) {
                                        controller.show(WindowInsetsCompat.Type.statusBars())
                                        //controller.show(WindowInsetsCompat.Type.navigationBars())
                                    } else {
                                        controller.hide(WindowInsetsCompat.Type.statusBars())
                                        //controller.hide(WindowInsetsCompat.Type.navigationBars())
                                    }
                                },
                            )
                        }
                ) {

                    item {
                        dlinkfilename?.let {
                            Text(it)
                        }
                        imgbmipc?.let { it1 ->
                            Image(
                                bitmap = it1,
                                // painterResource(R.drawable.wg433),
                                "another picture",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                if (!dlinkfilename.isNullOrBlank()) {
                                                    Log.i(
                                                        TAG,
                                                        "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\nGreeting: ${mainActivityViewModel.localipcamuri.value}"
                                                    )
                                                    mainActivityViewModel
                                                        .localipcamuri.value?.let { uriString ->
                                                            editMyPic(uriString)
                                                        }
                                                }
                                            },
                                            onDoubleTap = {
                                                if (!dlinkfilename.isNullOrBlank()) {
                                                    Log.i(
                                                        TAG,
                                                        "@@@ View: ${mainActivityViewModel.localipcamuri.value}"
                                                    )
                                                    mainActivityViewModel
                                                        .localipcamuri.value?.let { uriString ->
                                                            viewMyPic(uriString)
                                                        }
                                                }
                                            }
                                        )
                                    },
                                contentScale = ContentScale.FillWidth,
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Divider(
                            color = Color.Green,
                            thickness = 4.dp,
                        )
                        Spacer(Modifier.height(8.dp))

                        webcamfilename?.let {
                            Text(it)
                        }
                        imgbmweb?.let { it1 ->
                            Image(
                                it1,
                                "sexy girl",
                                contentScale = ContentScale.FillWidth,
                                //modifier = Modifier
                                //.align(Alignment.CenterHorizontally)
                                //.fillMaxWidth()
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                if (!webcamfilename.isNullOrBlank()) {
                                                    Log.i(
                                                        TAG,
                                                        "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\nGreeting: ${mainActivityViewModel.localipcamuri.value}"
                                                    )
                                                    mainActivityViewModel
                                                        .localwebcamuri.value?.let { uriString ->
                                                            editMyPic(uriString)
                                                        }
                                                }
                                            },
                                            onDoubleTap = {
                                                if (!webcamfilename.isNullOrBlank()) {
                                                    Log.i(
                                                        TAG,
                                                        "@@@ View: ${mainActivityViewModel.localwebcamuri.value}"
                                                    )
                                                    mainActivityViewModel
                                                        .localwebcamuri.value?.let { uriString ->
                                                            viewMyPic(uriString)
                                                        }
                                                }
                                            }
                                        )
                                    },
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Divider(
                            color = Color.Green,
                            thickness = 4.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Button(
                                onClick = {
                                    if (mainActivityViewModel.ipcamurl.value.isNullOrBlank() and mainActivityViewModel.webcamurl.value.isNullOrBlank()) {
                                        displaydialog = true
                                        Log.i(
                                            TAG,
                                            "Greeting: Download clicked but no urls found in settings"
                                        )
                                        return@Button
                                    }
                                    Log.i(TAG, "Greeting: Download clicked")
                                    mainActivityViewModel.download()
                                }
                            ) {
                                Text(text = "Download")
                            }
                        }
                    }
                }
                PullRefreshIndicator(isRefreshing, refreshState, Modifier.align(Alignment.TopCenter))
            }
        }
    }

    fun editMyPic(uriStr: String) {
        val i = Intent(Intent.ACTION_EDIT)
        i.setDataAndType(uriStr.toUri(), "image/*")
        i.putExtra(MediaStore.EXTRA_OUTPUT, uriStr.toUri())
        i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        editImgLauncher.launch(i)
    }

    fun viewMyPic(uriStr: String) {
        val i = Intent(Intent.ACTION_VIEW)
        i.setDataAndType(uriStr.toUri(), "image/*")
        //i.putExtra(MediaStore.EXTRA_OUTPUT, uriStr.toUri())
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        editImgLauncher.launch(i)
    }

}
