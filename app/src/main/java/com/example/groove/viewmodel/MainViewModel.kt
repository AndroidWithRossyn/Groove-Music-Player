package com.example.groove.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.groove.model.Song
import com.example.groove.repository.SongRepository
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    private val songRepository: SongRepository,
    private val application: Application
) : AndroidViewModel(application) {

    companion object {
        var sortOrder: Int = 0
        val sortingList = arrayOf(
            MediaStore.Audio.Media.DATE_ADDED + " DESC", MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.SIZE + " DESC"
        )
    }

    private var allSongsLiveData = MutableLiveData<List<Song>>()
    fun observeAllSongsLiveData(): LiveData<List<Song>> {
        return allSongsLiveData
    }

    private var allLocalSongsLiveData = MutableLiveData<List<Song>>()


    @SuppressLint("Recycle", "Range")
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun scanForSongs(){
        val tempList = ArrayList<Song>()
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val cursor = application.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
            sortingList[sortOrder], null
        )
        if (cursor != null) {
            if (cursor.moveToFirst())
                do {
                    val titleC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                            ?: "Unknown"
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                        ?: "Unknown"
                    val albumC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                            ?: "Unknown"
                    val artistC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                            ?: "Unknown"
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val durationC =
                        cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    val albumIdC =
                        cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                            .toString()
//                    val bitrateC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE))
//                    val dateAddedC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED))
//                    val contentTypeC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.CONTENT_TYPE))
                    val uri = Uri.parse("content://media/external/audio/albumart")
                    val artUriC = Uri.withAppendedPath(uri, albumIdC).toString()
                    val song = Song(
                        id = idC,
                        title = titleC,
                        album = albumC,
                        artist = artistC,
                        path = pathC,
                        duration = durationC,
                        artUri = artUriC
                    )
                    val file = File(song.path)
                    if (file.exists())
                        tempList.add(song)
                        songRepository.upsertSong(song)
                } while (cursor.moveToNext())
            cursor.close()
        }
        allSongsLiveData.value = tempList
    }


    // To delete the deleted songs from roomDatabase
    suspend fun updateAllSongRoomDatabase(){
        val allSongs = songRepository.getAllSongs()
        for(song in allSongs){
            val file = File(song.path)
            if (!file.exists()){
                songRepository.deleteSong(song)
            }
        }
    }

}