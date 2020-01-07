package com.carlesramos.practicamusicplayerdef.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;


public class Song implements Comparable<Song>, Parcelable, Serializable {
    private int id;
    private String title;
    private String artist;
    private String album;
    private int duration;
    private String path;
    private int minuts;
    private int seconds;

    public Song(String title, String artist, String album, int dur, String path) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = dur;
        this.path = path;
        minuts = (int) ((dur / (1000*60)) % 60);
        seconds = (int) (dur / 1000) % 60 ;
    }


    protected Song(Parcel in) {
        title = in.readString();
        artist = in.readString();
        album = in.readString();
        duration = in.readInt();
        path = in.readString();
        minuts = in.readInt();
        seconds = in.readInt();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }
        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    public int getMinuts() {
        return minuts;
    }

    public int getSeconds() {
        return seconds;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public int getDuration() {
        return duration;
    }

    public String getPath() {
        return path;
    }

    @Override
    public int compareTo(Song o) {
        int last = this.artist.compareTo(o.artist);
        return last == 0 ? this.title.compareTo(o.title) : last;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeInt(duration);
        dest.writeString(path);
        dest.writeInt(minuts);
        dest.writeInt(seconds);
    }

}
