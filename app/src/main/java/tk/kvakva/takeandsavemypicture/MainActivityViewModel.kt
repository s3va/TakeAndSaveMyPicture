package tk.kvakva.takeandsavemypicture

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.IllegalArgumentException

private const val LOCALURI = "localuri"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "urlsanduri")

class MainActivityViewModel(val appl: Application) : AndroidViewModel(appl) {

    fun saveUrls() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                appl.dataStore.edit {
                    it[stringPreferencesKey(appl.resources.getString(R.string.dlinkurlkey))] =
                        ipcamurl.value ?: ""
                    it[stringPreferencesKey(appl.resources.getString(R.string.webcurlkey))] =
                        webcamurl.value ?: ""
                }
            }
        }
    }

    fun saveLocalUri(localUri: String) {
        localuri.value = localUri
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                appl.dataStore.edit {
                    it[stringPreferencesKey(LOCALURI)] =
                        localUri
                }
            }
        }
    }


    var ipcamurl = MutableLiveData<String>()

    // val ipcamurl: LiveData<String> = _ipcamurl
    var webcamurl = MutableLiveData<String>()

    //val webcamurl: LiveData<String> = _webcamurl
    var localuri = MutableLiveData<String>()
    // val localuri: LiveData<String> = _localuri
    var localipcamuri = MutableLiveData<String>()
    var localwebcamuri = MutableLiveData<String>()

    init {
        updateUrlsFromDataStore()

    }

    fun updateUrlsFromDataStore() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val data = appl.dataStore.data.first()
                webcamurl.postValue(
                    data[stringPreferencesKey(appl.resources.getString(R.string.webcurlkey))]
                )
                ipcamurl.postValue(
                    data[stringPreferencesKey(appl.resources.getString(R.string.dlinkurlkey))]
                )
                localuri.postValue(
                    data[stringPreferencesKey(LOCALURI)]
                )
            }
        }
    }

    val mutex = Mutex()
    fun download() {
        Log.i(TAG, "download: started")
        viewModelScope.launch(Dispatchers.IO) {
            if (mutex.isLocked)
                return@launch
            else
                mutex.lock()
            Log.i(TAG, "download: refreshing start")
            isRefreshing.emit(true)

            if (!ipcamurl.value.isNullOrBlank())
                if (URLUtil.isHttpsUrl(ipcamurl.value))
                    try {
                        val jpegfl = ipcamurl.value?.let { retrofitService.getPicture(it) }
                        jpegfl?.let { responseBody ->
                            if (responseBody.isSuccessful) {
                                val dftree = try {
                                    DocumentFile.fromTreeUri(
                                        appl.applicationContext,
                                        Uri.parse(
                                            localuri.value ?: ""
                                        )
                                    )
                                } catch (e: IllegalArgumentException) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            appl,
                                            "Select catalog to save files",
                                            Toast.LENGTH_LONG
                                        )
                                            .show()
                                    }
                                    null
                                }
                                responseBody.body()?.use { body ->
                                    val fname = responseBody.headers()["Content-Disposition"]
                                        ?.replace("filename=", "")
                                        ?.replace("\"", "") ?: "${datelocaltimestring()}.jpeg"
                                    Log.i(TAG, "download: $fname")
                                    val newdf = dftree?.createFile("image/jpeg", fname)
                                    dlinkcamfilename.postValue(fname)
                                    newdf?.uri?.run {
                                        appl.contentResolver.openOutputStream(this)?.use {
                                            it.write(body.bytes())
                                        }
                                    }
                                    newdf?.uri?.run {
                                        appl.contentResolver.openInputStream(this)?.use {
                                            dlinkibm.postValue(
                                                BitmapFactory.decodeStream(it).asImageBitmap()
                                            )
                                        }
                                        localipcamuri.postValue(this.toString())
                                    }
                                    if (newdf?.uri == null) {
                                        dlinkibm.postValue(
                                            BitmapFactory.decodeStream(body.byteStream())
                                                .asImageBitmap()
                                        )
                                    }
                                }
                            } else {
                                val errorString = responseBody.errorBody()?.string()
                                Log.i(
                                    TAG,
                                    "download: ${responseBody.code()}\n\n$errorString"
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        appl.applicationContext,
                                        "${responseBody.code()}\n\n$errorString",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(appl, e.stackTraceToString(), Toast.LENGTH_LONG).show()
                        }
                    }
                else
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            appl,
                            "${ipcamurl.value}\nis not https url",
                            Toast.LENGTH_LONG
                        ).show()
                    }

            if (!webcamurl.value.isNullOrBlank())
                if (URLUtil.isHttpsUrl(webcamurl.value))
                    try {
                        val jpegfl = webcamurl.value?.let { retrofitService.getPicture(it) }
                        jpegfl?.let { responseBody ->
                            if (responseBody.isSuccessful) {
                                val dftree = try {
                                    DocumentFile.fromTreeUri(
                                        appl.applicationContext,
                                        Uri.parse(
                                            localuri.value ?: ""
                                        )
                                    )
                                } catch (e: IllegalArgumentException) {
                                    e.printStackTrace()
                                    //withContext(Dispatchers.Main) {
                                    //    Toast.makeText(appl, "Select catalog to same files", Toast.LENGTH_LONG)
                                    //        .show()
                                    //}
                                    null
                                }
                                responseBody.body()?.use { body ->
                                    val fname = responseBody.headers()["Content-Disposition"]
                                        ?.replace("filename=", "")
                                        ?.replace("\"", "") ?: "${datelocaltimestring()}.jpeg"
                                    Log.i(TAG, "download: $fname")
                                    val newdf = dftree?.createFile("image/jpeg", fname)
                                    webcamfilename.postValue(fname)
                                    newdf?.uri?.run {
                                        appl.contentResolver.openOutputStream(this)?.use {
                                            it.write(body.bytes())
                                        }
                                    }
                                    newdf?.uri?.run {
                                        appl.contentResolver.openInputStream(this)?.use {
                                            webcamibm.postValue(
                                                BitmapFactory.decodeStream(it).asImageBitmap()
                                            )
                                        }
                                        localwebcamuri.postValue(this.toString())
                                    }
                                    if (newdf?.uri == null) {
                                        webcamibm.postValue(
                                            BitmapFactory.decodeStream(body.byteStream())
                                                .asImageBitmap()
                                        )
                                    }
                                }
                            } else {
                                val errorString = responseBody.errorBody()?.string()
                                Log.i(
                                    TAG,
                                    "download: ${responseBody.code()}\n\n$errorString"
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        appl.applicationContext,
                                        "${responseBody.code()}\n\n$errorString",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(appl, e.stackTraceToString(), Toast.LENGTH_LONG).show()
                        }
                    }
                else
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            appl,
                            "${webcamurl.value}\nis not https url",
                            Toast.LENGTH_LONG
                        ).show()
                    }

            isRefreshing.emit(false)
            Log.i(TAG, "download: refreshing stoped")
            mutex.unlock()
        }
        Log.i(TAG, "download: stoped")
    }

    var dlinkibm = MutableLiveData<ImageBitmap>().apply {
        val bitmp = BitmapFactory.decodeResource(appl.resources, R.drawable.wg233)
        val matrix = Matrix()
        matrix.setRotate(90f)
        val nbitmap = Bitmap.createBitmap(bitmp, 0, 0, bitmp.width, bitmp.height, matrix, true)
        value = nbitmap.asImageBitmap()
    }
    var webcamibm = MutableLiveData<ImageBitmap>().apply {
        Log.i(
            TAG,
            "****************************************************************init webcamibm: start it"
        )
        value = BitmapFactory.decodeResource(appl.resources, R.drawable.wg433).asImageBitmap()
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val data = appl.dataStore.data.first()
            Log.i(
                TAG,
                "!!!!!!!!!!!!!!!!!!!!!!!!!!! ${data[stringPreferencesKey(LOCALURI)]} !!!!!!!!!!!!!!!!!!!!!!!!!: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
            )
            data[stringPreferencesKey(LOCALURI)]?.let { localUriStr ->
                Log.i(TAG, "************* localuri.value?.let { localUriStr ->: $localUriStr ")
                val dftree = DocumentFile.fromTreeUri(
                    appl.applicationContext,
                    Uri.parse(localUriStr)
                )  //${MediaStore.Images.Media.DATE_TAKEN} DESC
                val dlist = dftree?.listFiles()?.sortedByDescending {
                    it.lastModified()
                }
                Log.i(TAG, "dlist: $dlist")
                dlist?.firstOrNull()?.uri?.let {
                    appl.contentResolver.query(it, null, null, null, null)?.use { curs ->
                        curs.moveToFirst()
                        webcamfilename.postValue(
                            curs.getString(curs.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME))
                        )
                    }
                    Log.i(TAG, "first Uri: $it")
                    appl.contentResolver.openInputStream(it).use { inputStream ->
                        webcamibm.postValue(
                            BitmapFactory.decodeStream(inputStream).asImageBitmap()
                        )
                        localwebcamuri.postValue(it.toString())
                    }
                }

                if (dlist.orEmpty().size > 1) {
                    dlist?.component2()?.uri?.let {
                        appl.contentResolver.query(it, null, null, null, null)?.use { curs ->
                            curs.moveToFirst()
                            dlinkcamfilename.postValue(
                                curs.getString(curs.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME))
                            )
                        }
                        Log.i(TAG, "first Uri: $it")
                        appl.contentResolver.openInputStream(it).use { inputStream ->
                            dlinkibm.postValue(
                                BitmapFactory.decodeStream(inputStream).asImageBitmap()
                            )
                            localipcamuri.postValue(it.toString())
                        }
                    }
                }
            }
            Log.i(TAG, "cannot get first file: to bitmap")
        }
    }

    var isRefreshing = MutableStateFlow(false)

    var webcamfilename = MutableLiveData<String>()
    var dlinkcamfilename = MutableLiveData<String>()
}

private const val TAG = "MainActivityViewModel"