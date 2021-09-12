package tech.smallwonder.simplemusic.utils

import tech.smallwonder.simplemusic.models.Song

sealed class SortOrder(val title: String) {
    abstract fun sort(songs: List<Song>, ascending: Boolean): List<Song>
    abstract fun groupBy(songs: List<Song>): Map<String, List<Song>>

    class Title : SortOrder("Title") {
        override fun sort(songs: List<Song>, ascending: Boolean): List<Song> {
            return songs.sortedBy {
                it.title
            }
        }

        override fun groupBy(songs: List<Song>): Map<String, List<Song>> {
            return songs.groupBy {
                it.title[0].toString()
            }.toSortedMap()
        }
    }

    class Album : SortOrder("Album") {
        override fun sort(songs: List<Song>, ascending: Boolean): List<Song> {
            return songs.sortedBy {
                it.trackNo
            }.sortedBy {
                it.album
            }
        }

        override fun groupBy(songs: List<Song>): Map<String, List<Song>> {
            return songs.groupBy {
                it.album
            }.toSortedMap()
        }
    }

    class Artist : SortOrder("Artist") {
        override fun sort(songs: List<Song>, ascending: Boolean): List<Song> {
            return songs.sortedBy {
                it.trackNo
            }.sortedBy {
                it.artist
            }
        }

        override fun groupBy(songs: List<Song>): Map<String, List<Song>> {
            return songs.groupBy {
                it.artist
            }.toSortedMap()
        }
    }

    class Track : SortOrder("Track") {
        override fun sort(songs: List<Song>, ascending: Boolean): List<Song> {
            return songs.sortedBy {
                it.trackNo
            }
        }

        override fun groupBy(songs: List<Song>): Map<String, List<Song>> {
            return songs.groupBy {
                it.trackNo.toString()
            }.toSortedMap()
        }
    }
}
