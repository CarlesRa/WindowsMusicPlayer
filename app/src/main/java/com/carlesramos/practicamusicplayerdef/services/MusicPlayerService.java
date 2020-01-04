package com.carlesramos.practicamusicplayerdef.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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
    public static final String PLAY_THIS = "com.carlesramos.practicamusicplayer";
    public static final String EXTRA_CANCIONES = "com.carlesramos.extracanciones";
    public static final String GO_SEEKBAR = "com.carlesramos.goseekbar";

    private ArrayList<Song> songs;
    private MediaPlayer player;
    private boolean isPaused;
    private Intent takeYourSongsIntent;
    private Intent goSeekBarIntent;
    private long count;
    private int arrayPosition;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"onCreate");
        isPaused = false;
        player = new MediaPlayer();
        songs = new ArrayList<>();
        getMusicFromExternal();
        Collections.sort(songs);
        player.setWakeMode(MusicPlayerService.this, PowerManager.PARTIAL_WAKE_LOCK);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        takeYourSongsIntent = new Intent();
        takeYourSongsIntent.setAction(PLAY_THIS);
        count = 0;
        arrayPosition = 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        goSeekBarIntent = new Intent();
        goSeekBarIntent.setAction(GO_SEEKBAR);

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
        stopSelf();
        super.onDestroy();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        createNotification();
        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (count > 0) LocalBroadcastManager.getInstance(this).sendBroadcast(takeYourSongsIntent);
        count++;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    public class LocalBinder extends Binder {
        public MusicPlayerService getService(){
            return MusicPlayerService.this;
        }
    }

    public void play(Context context, String path, boolean isFromList){
    //public void play() {
        //TODO revisar los if jejjee....
        if (!player.isPlaying() && !isPaused) {
            player.reset();
            try {
                player.setDataSource(path);
                player.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (player.isPlaying()){
            player.reset();
            try {
                player.setDataSource(path);
                player.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        else if (!player.isPlaying() && isFromList){
            player.reset();
            try {
                player.setDataSource(path);
                player.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (!player.isPlaying() && isPaused){
            player.start();
            isPaused = false;

        }
        else if (!player.isPlaying()){
            try {
                player.setDataSource(path);
                player.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!player.isPlaying() && !isPaused){
            try {
                player.reset();
                player.setDataSource(path);
                player.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (isPaused){
            player.start();
        }
    }

    /*public void play(int arrayPosition){
        this.arrayPosition = arrayPosition;
        try {
            player.reset();
            player.setDataSource(songs.get(arrayPosition).getPath());
            player.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    public void pause(){
        player.pause();
        isPaused = true;
    }

    public void nextPrev(Context context, String path){
    //public void next(){
        try {
            player.reset();
            player.setDataSource(path);
            player.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /*public void prev(){
        arrayPosition--;
        if (arrayPosition < 0){
            arrayPosition = songs.size() - 1;
        }
        if (player.isPlaying()){
            player.stop();
            player.reset();
            try {
                player.setDataSource(songs.get(arrayPosition).getPath());
                player.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }*/

    public MediaPlayer getPlayer() {
        return player;
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
