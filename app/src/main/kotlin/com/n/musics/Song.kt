package com.n.musics

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtUri: String?,
    val audioUri: String
)