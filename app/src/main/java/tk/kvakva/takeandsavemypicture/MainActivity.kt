package tk.kvakva.takeandsavemypicture

import android.content.Intent
import android.content.UriMatcher
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.systemuicontroller.rememberSystemUiController

import tk.kvakva.takeandsavemypicture.ui.theme.TakeAndSaveMyPictureTheme

private const val TAG = "MainActivity"


class MainActivity : ComponentActivity() {

    private val mainActivityViewModel by viewModels<MainActivityViewModel>()

    private var editImgLauncher = registerForActivityResult(
        ActivityResultContracts
            .StartActivityForResult()
    ){
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
        setContent {
            TakeAndSaveMyPictureTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Greeting()
                }
            }
        }
    }

    @Composable
    fun Greeting() {

        val systemUiController = rememberSystemUiController()
        val isRefreshing by mainActivityViewModel.isRefreshing.collectAsState()

        val dlinkfilename by mainActivityViewModel.dlinkcamfilename.observeAsState()
        val webcamfilename by mainActivityViewModel.webcamfilename.observeAsState()
        var displayTopBar by remember { mutableStateOf(true) }
        var displaydialog by remember { mutableStateOf(false) }
        //var urldl by remember { mutableStateOf("") }
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
            }


        ) {
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
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing),
                onRefresh =
                {
                    if (mainActivityViewModel.ipcamurl.value.isNullOrBlank() and mainActivityViewModel.webcamurl.value.isNullOrBlank()) {
                        displaydialog = true
                        Log.i(
                            TAG,
                            "Greeting: Swiped but no urls found in settings"
                        )
                        return@SwipeRefresh
                    }
                    Log.i(TAG, "Greeting: Swiped")
                    mainActivityViewModel.download()
                },
            ) {
                Column(
                    Modifier
                        .verticalScroll(ScrollState(0))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    displayTopBar = !displayTopBar
                                    systemUiController.isStatusBarVisible = displayTopBar
                                },
                            )
                        }
                ) {

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
                                            if (!dlinkfilename.isNullOrBlank()) {
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
        }
    }

    fun editMyPic(uriStr: String) {
        val i = Intent(Intent.ACTION_EDIT)
        i.setDataAndType(Uri.parse(uriStr), "image/*")
        i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.parse(uriStr))
        i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        editImgLauncher.launch(i)
    }
}
//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    TakeAndSaveMyPictureTheme {
//        Greeting(dirpic)
//    }
//}
