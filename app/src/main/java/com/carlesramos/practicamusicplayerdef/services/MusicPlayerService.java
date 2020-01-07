package com.carlesramos.practicamusicplayerdef.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.carlesramos.practicamusicplayerdef.R;
import com.carlesramos.practicamusicplayerdef.model.Song;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

public class MusicPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener{

    private final IBinder binder = new LocalBinder();
    public static final String TAG = "MusicPlayerService";
    public static final String INIT_SEEKBAR = "com.carlesramos.init_seekbar";
    public static final String ALERT = "com.carlesramos.alert";
    public static final int CANCIONES_MUESTRA = 4;

    private ArrayList<Song> songs;
    private Uri[] uris;
    private MediaPlayer player;
    private boolean isPaused;
    private Intent initSeekBarIntent;
    private boolean isInTestMode;
    private int arrayPosition;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"onCreate");
        isPaused = false;
        initSeekBarIntent = new Intent();
        initSeekBarIntent.setAction(INIT_SEEKBAR);
        Intent alertIntent = new Intent();
        alertIntent.setAction(ALERT);
        player = new MediaPlayer();
        songs = new ArrayList<>();
        uris = new Uri[CANCIONES_MUESTRA];
        getMusicFromExternal();
        if (songs.size() == 0){
            getMusicFromRaw();
            isInTestMode = true;
            LocalBroadcastManager.getInstance(this).sendBroadcast(alertIntent);
        }
        Collections.sort(songs);
        arrayPosition = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        try {
            if (!isInTestMode) {
                player.setDataSource(songs.get(arrayPosition).getPath());
            }
            else player.setDataSource(this, uris[arrayPosition]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");
        return true;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        Log.i(TAG, "OnPrepared");
        player.start();
        LocalBroadcastManager.getInstance(this).sendBroadcast(initSeekBarIntent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i(TAG, "OnComletion");
            next();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.i(TAG, "OnError");
        play();
        return false;
    }

    public class LocalBinder extends Binder {
        public MusicPlayerService getService(){
            return MusicPlayerService.this;
        }
    }

    public void play() {
        if (!isPaused){
            try {
                player.reset();
                if(!isInTestMode){
                    player.setDataSource(songs.get(arrayPosition).getPath());
                }
                else player.setDataSource(this, uris[arrayPosition]);

                player.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            player.start();
            isPaused = false;
        }
    }

    public void play(int arrayPosition){
        this.arrayPosition = arrayPosition;

        try {
            player.reset();
            if(!isInTestMode){
                player.setDataSource(songs.get(arrayPosition).getPath());
            }
            else player.setDataSource(this, uris[arrayPosition]);
            player.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pause(){
        player.pause();
        isPaused = true;
    }

    public void next(){
        arrayPosition++;
        if (arrayPosition == songs.size()) arrayPosition = 0;
        play();
    }
    public void prev(){
        arrayPosition--;
        if (arrayPosition < 0){
            arrayPosition = songs.size() - 1;
        }
        play();
    }

    public int getPosition(){
        return player.getCurrentPosition();
    }

    public void seekToPosition(int position){
        if (player.isPlaying()){
            player.seekTo(position);
        }
    }

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public int getSongDuration(){
        return player.getDuration();
    }

    public String getSongTitle(){
        return songs.get(arrayPosition).getTitle();
    }

    /**
     * Metode per a buscar musica en el almacenament del dispositiu
     */
    public void getMusicFromExternal(){
        String title;
        String artist;
        String album;
        int duration;
        String path;

        ContentResolver contentResolver = getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor songCursor = contentResolver.query(EXTERNAL_CONTENT_URI, null, selection, null, sortOrder);
        if (songCursor != null && songCursor.moveToFirst()){
            int songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int songDuration = songCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int songLocation = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            while (songCursor.moveToNext()){
                title = songCursor.getString(songTitle);
                artist= songCursor.getString(songArtist);
                album = songCursor.getString(albumTitle);
                duration = songCursor.getInt(songDuration);
                path = songCursor.getString(songLocation);
                songs.add(new Song(title,artist,album,duration,path));
            }
            songCursor.close();
        }
    }

    /**
     * obtinc la musica del directori raw
     */
    public void getMusicFromRaw(){
        MediaPlayer m;
        int songId;
        uris[0] = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.song1);
        uris[1] = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.song2);
        uris[2] = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.song3);
        uris[3] = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.song4);
        for (int i=0; i<CANCIONES_MUESTRA; i++){
            songId = getResources().getIdentifier("raw/song"+(i+1),null, getPackageName());
            m = MediaPlayer.create(this, songId);
            songs.add(new Song("Song"+(i+1), "Test Mode", "Test Mode", m.getDuration(), "Test Mode"));
            m.release();
        }

    }
}