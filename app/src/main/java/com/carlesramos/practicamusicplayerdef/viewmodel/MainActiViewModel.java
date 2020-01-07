package com.carlesramos.practicamusicplayerdef.viewmodel;

import androidx.lifecycle.ViewModel;

public class MainActiViewModel extends ViewModel {
    private int seeckBarPosition;
    private int songDuration;

    public int getSeeckBarPosition() {
        return seeckBarPosition;
    }

    public void setSeeckBarPosition(int seeckBarPosition) {
        this.seeckBarPosition = seeckBarPosition;
    }

    public int getSongDuration() {
        return songDuration;
    }

    public void setSongDuration(int songDuration) {
        this.songDuration = songDuration;
    }

}
