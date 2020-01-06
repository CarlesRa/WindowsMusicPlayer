package com.carlesramos.practicamusicplayerdef.services;

import android.app.Notification;
import android.app.PendingIntent;
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
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.carlesramos.practicamusicplayerdef.MainActivity;
import com.carlesramos.practicamusicplayerdef.R;
import com.carlesramos.practicamusicplayerdef.model.Song;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
import static com.carlesramos.practicamusicplayerdef.App.CHANNEL_ID;

public class MusicPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener{

    private final IBinder binder = new LocalBinder();
    public static final String TAG = "MusicPlayerService";
    public static final String INIT_SEEKBAR = "com.carlesramos.init_seekbar";

    private ArrayList<Song> songs;
    private MediaPlayer player;
    private boolean isPaused;
    private Intent initSeekBarIntent;
    private boolean playContinuous;
    private int arrayPosition;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"onCreate");
        isPaused = false;
        player = new MediaPlayer();
        player.setWakeMode(MusicPlayerService.this, PowerManager.PARTIAL_WAKE_LOCK);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        songs = new ArrayList<>();
        getMusicFromExternal();
        Collections.sort(songs);
        arrayPosition = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        if (player.isPlaying()){
            player.pause();
        }
        initSeekBarIntent = new Intent();
        initSeekBarIntent.setAction(INIT_SEEKBAR);
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
        player = null;
        super.onDestroy();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.i(TAG, "OnPrepared");
        //createNotification();
        mp.start();
        LocalBroadcastManager.getInstance(this).sendBroadcast(initSeekBarIntent);
        playContinuous = true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i(TAG, "OnComletion");
        if (playContinuous){
            next();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.i(TAG, "OnError");
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
                player.setDataSource(songs.get(arrayPosition).getPath());
                player.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(isPaused) {
            player.start();
            isPaused = false;
        }
    }

    public void play(int arrayPosition){
        this.arrayPosition = arrayPosition;

        try {
            player.reset();
            player.setDataSource(songs.get(arrayPosition).getPath());
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

    public MediaPlayer getPlayer() {
        return this.player;
    }

    public int getPosition(){
        return player.getCurrentPosition();
    }

    public void seekToPosition(int position){
        if (player.isPlaying()){
            player.seekTo(position);
        }
    }

    public void createNotification(){
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Player Custom")
                .setContentText("hola amigo")
                .setSmallIcon(R.drawable.ic_album)
                .setContentIntent(pendingIntent)
                .build();
    }

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public int getArrayPosition() {
        return arrayPosition;
    }

    public boolean isPlaying(){
        return player.isPlaying();
    }

    public int getSongDuration(){
        return player.getDuration();
    }

    public String getSongTitle(){
        return songs.get(arrayPosition).getTitle();
    }

    public void getMusicFromExternal(){
        String title;
        String artist;
        String album;
        int duration;
        String path;
        String imagePath;

        ContentResolver contentResolver = getContentResolver();
        Uri uri = EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor songCursor = contentResolver.query(uri, null, selection, null, sortOrder);
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

}