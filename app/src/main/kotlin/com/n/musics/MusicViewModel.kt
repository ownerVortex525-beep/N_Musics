package com.n.musics

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private var currentIndex = 0

    init {
        loadSongs()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            val songList = withContext(Dispatchers.IO) {
                val songs = mutableListOf<Song>()
                val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DATA
                )

                val cursor = getApplication<Application>().contentResolver.query(
                    collection,
                    projection,
                    null,
                    null,
                    "${MediaStore.Audio.Media.TITLE} ASC"
                )

                cursor?.use {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (it.moveToNext()) {
                        val id = it.getString(idColumn)
                        val title = it.getString(titleColumn)
                        val artist = it.getString(artistColumn) ?: "Unknown Artist"
                        val albumId = it.getLong(albumIdColumn)
                        val data = it.getString(dataColumn)

                        val albumArtUri = try {
                            val artUri = ContentUris.withAppendedId(
                                Uri.parse("content://media/external/audio/albumart"),
                                albumId
                            )
                            artUri.toString()
                        } catch (e: Exception) {
                            null
                        }

                        songs.add(
                            Song(
                                id = id,
                                title = title,
                                artist = artist,
                                albumArtUri = albumArtUri,
                                audioUri = data
                            )
                        )
                    }
                }
                songs
            }
            _songs.value = songList
        }
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _isPlaying.value = true
        currentIndex = _songs.value.indexOf(song)
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun playNext() {
        if (_songs.value.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % _songs.value.size
            _currentSong.value = _songs.value[currentIndex]
            _isPlaying.value = true
        }
    }

    fun playPrevious() {
        if (_songs.value.isNotEmpty()) {
            currentIndex = if (currentIndex - 1 < 0) _songs.value.size - 1 else currentIndex - 1
            _currentSong.value = _songs.value[currentIndex]
            _isPlaying.value = true
        }
    }
}