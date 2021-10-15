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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import java.io.IOException
import java.lang.IllegalArgumentException

private const val LOCALURI = "localuri"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "urlsanduri")

class MainActivityViewModel(private val appl: Application) : AndroidViewModel(appl) {

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

    private val mutex = Mutex()
    fun download() {
        Log.i(TAG, "download: started")
        viewModelScope.launch(Dispatchers.IO) {
            if (mutex.isLocked)
                return@launch
            else
                mutex.lock()
            Log.i(TAG, "download: refreshing start")
            isRefreshing.emit(true)

            if(!ipcamurl.value.isNullOrBlank()) {
                val p = dl(ipcamurl.value!!,localuri.value)
                if(p.first!=null)
                    dlinkibm.postValue(p.first?.asImageBitmap())
                if(p.second!=null)
                    dlinkcamfilename.postValue(p.second)
            }
            if(!webcamurl.value.isNullOrBlank()) {
                val p = dl(webcamurl.value!!,localuri.value)
                if(p.first!=null)
                    webcamibm.postValue(p.first?.asImageBitmap())
                if(p.second!=null)
                    webcamfilename.postValue(p.second)
            }

            /*if (!webcamurl.value.isNullOrBlank())
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
*/

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
            val localUriStr = data[stringPreferencesKey(LOCALURI)]
            if (!localUriStr.isNullOrBlank()) { //localUriStr ->
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

                    Log.i(TAG, "first Uri: $it")
                    appl.contentResolver.openInputStream(it).use { inputStream ->
                        val bm = BitmapFactory
                            .decodeStream(inputStream)
                            ?.asImageBitmap()
                        if (bm != null) {
                            webcamibm.postValue(bm)
                            localwebcamuri.postValue(it.toString())
                            appl.contentResolver.query(it, null, null, null, null)?.use { curs ->
                                curs.moveToFirst()
                                webcamfilename.postValue(
                                    curs.getString(curs.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME))
                                )
                            }
                        }
                    }
                }

                if (dlist.orEmpty().size > 1) {
                    dlist?.component2()?.uri?.let {
                        Log.i(TAG, "second Uri: $it")
                        appl.contentResolver.openInputStream(it).use { inputStream ->
                            val bm = BitmapFactory
                                .decodeStream(inputStream)
                                ?.asImageBitmap()
                            if (bm != null) {
                                dlinkibm.postValue(bm)
                                localipcamuri.postValue(it.toString())
                                appl.contentResolver.query(it, null, null, null, null)
                                    ?.use { curs ->
                                        curs.moveToFirst()
                                        dlinkcamfilename.postValue(
                                            curs.getString(curs.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME))
                                        )
                                    }
                            }
                        }
                    }
                }
            } else
                Log.i(TAG, "cannot get first file: to bitmap")
        }
    }

    var isRefreshing = MutableStateFlow(false)

    var webcamfilename = MutableLiveData<String>()
    var dlinkcamfilename = MutableLiveData<String>()

    private suspend fun dl(urlstring: String, localTreeUri: String?): Pair<Bitmap?, String?> {
        var fname: String? = null
        var bm: Bitmap? = null

        if (urlstring.isNotBlank())
            if (URLUtil.isHttpsUrl(urlstring))
                try {
                    val response = retrofitService.getPicture(urlstring)
                    if (response.isSuccessful) {
                        val dftree = try {
                            DocumentFile.fromTreeUri(
                                appl.applicationContext,
                                Uri.parse(
                                    localTreeUri ?: ""
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
                        response.body()?.use { body ->
                            fname = response.headers()["Content-Disposition"]
                                ?.replace("filename=", "")
                                ?.replace("\"", "") // ?: "${datelocaltimestring()}.jpeg"
                            if (!fname.isNullOrBlank()) {
                                Log.i(TAG, "download: $fname")
                                val newdf = dftree?.createFile("image/jpeg", fname!!)
                                val bb = body.byteStream().use {
                                    it.readBytes()
                                }
                                if (newdf?.uri != null) {
                                    viewModelScope.launch(Dispatchers.IO) {
                                        appl.contentResolver.openOutputStream(newdf.uri)?.use {
                                            it.write(bb)
                                        }
                                    }
                                    bm = BitmapFactory
                                        .decodeByteArray(bb, 0, bb.size)
                                } else {
                                    dlinkibm.postValue(
                                        BitmapFactory.decodeStream(body.byteStream())
                                            .asImageBitmap()
                                    )
                                }
                            } else {
                                Log.i(
                                    TAG,
                                    "download: fname is not defined in http header Content-Disposition for d-link ip camera"
                                )
                            }
                        }
                    } else {
                        val errorString: String? = viewModelScope.async(Dispatchers.IO) {
                            response.errorBody()?.string()
                        }.await()
                        Log.i(
                            TAG,
                            "download: ${response.code()}\n\n$errorString"
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                appl.applicationContext,
                                "${response.code()}\n\n$errorString",
                                Toast.LENGTH_SHORT
                            ).show()
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
        return Pair(bm,fname)
    }
}

private const val TAG = "MainActivityViewModel"

