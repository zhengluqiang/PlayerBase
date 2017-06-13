/*
 * Copyright 2017 jiajunhui<junhui_jia@163.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.kk.taurus.playerbase.widget;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.kk.taurus.playerbase.callback.OnAdListener;
import com.kk.taurus.playerbase.callback.OnErrorListener;
import com.kk.taurus.playerbase.callback.OnPlayerEventListener;
import com.kk.taurus.playerbase.setting.BaseAdVideo;
import com.kk.taurus.playerbase.setting.PlayData;
import com.kk.taurus.playerbase.setting.VideoData;

import java.util.List;

/**
 * Created by Taurus on 2017/3/24.
 */

public abstract class BaseAdPlayer extends BaseSettingPlayer {

    private PlayData mPlayData;
    private OnAdListener mOnAdListener;
    private int mAdIndex = -1;
    private List<BaseAdVideo> adVideos;

    public BaseAdPlayer(@NonNull Context context) {
        super(context);
    }

    public BaseAdPlayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseAdPlayer(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onPlayerEvent(int eventCode, Bundle bundle) {
        super.onPlayerEvent(eventCode, bundle);
        onHandleAdPlay(eventCode, bundle);
    }

    @Override
    protected void onErrorEvent(int eventCode, Bundle bundle) {
        super.onErrorEvent(eventCode, bundle);
        onHandleErrorEvent(eventCode, bundle);
    }

    protected boolean isDataSourceAvailable(){
        return dataSource!=null;
    }

    public void retryPlayData(){
        prepareStartPlayData();
    }

    @Override
    public void playData(@NonNull PlayData data, OnAdListener onAdListener) {
        this.mPlayData = data;
        this.mOnAdListener = onAdListener;
        //notify event on set play data
        Bundle bundle = new Bundle();
        bundle.putSerializable(OnPlayerEventListener.BUNDLE_KEY_VIDEO_DATA,data);
        onPlayerEvent(OnPlayerEventListener.EVENT_CODE_PLAYER_ON_SET_PLAY_DATA,bundle);
        prepareStartPlayData();
    }

    private void prepareStartPlayData() {
        if(this.mPlayData.isNeedAdPlay()){
            this.adVideos = this.mPlayData.getAdVideos();
            mAdIndex = 0;
            //notify ad prepared
            onNotifyAdPrepared(adVideos);
            BaseAdVideo firstAd = adVideos.get(mAdIndex);
            //-----go play first ad-----
            if(mOnAdListener!=null){
                mOnAdListener.onAdPlay(this,firstAd);
            }else{
                startPlayAdVideo(firstAd);
            }
        }else{
            mAdIndex = -1;
            //-----call back play all complete-----
            onNotifyAdFinish(null,true);

            //-----call back on video start-----
            if(mOnAdListener!=null){
                mOnAdListener.onVideoStart(this,mPlayData.getData());
            }else{
                setAndStartVideo(mPlayData.getData());
            }
        }
    }

    public final void setAndStartVideo(final VideoData data) {
        stop();
        post(new Runnable() {
            @Override
            public void run() {
                updatePlayerType(data.getPlayerType());
                setDataSource(data);
                start(data.getStartPos());
            }
        });
    }

    public final void startPlayAdVideo(final BaseAdVideo adVideo) {
        stop();
        post(new Runnable() {
            @Override
            public void run() {
                updatePlayerType(adVideo.getPlayerType());
                setDataSource(adVideo);
                start(adVideo.getStartPos());
                onNotifyAdStart(adVideo);
            }
        });
    }


    private void onHandleAdPlay(int eventCode, Bundle bundle) {
        switch (eventCode){
            case OnPlayerEventListener.EVENT_CODE_PLAY_COMPLETE:
                if(isAdData()){
                    judgeAdPlay();
                }
                break;
        }
    }

    private boolean isAdData(){
        return dataSource!=null && (dataSource instanceof BaseAdVideo);
    }

    private void judgeAdPlay() {
        if(!isAdListFinish()){
            BaseAdVideo adVideo = adVideos.get(mAdIndex);
            mAdIndex++;
            if(!isAdListFinish()){
                //-----call back play complete-----
                onNotifyAdFinish(adVideo,false);
                if(mOnAdListener!=null){
                    mOnAdListener.onAdPlayComplete(adVideo,false);
                }
                BaseAdVideo playAd = adVideos.get(mAdIndex);

                //-----go play next ad-----
                if(mOnAdListener!=null){
                    mOnAdListener.onAdPlay(this,playAd);
                }else{
                    startPlayAdVideo(playAd);
                }
            }else{
                //-----call back play all complete-----
                onNotifyAdFinish(adVideo,true);
                if(mOnAdListener!=null){
                    mOnAdListener.onAdPlayComplete(adVideo,true);
                }

                //-----call back on video start-----
                if(mOnAdListener!=null){
                    mOnAdListener.onVideoStart(this,mPlayData.getData());
                }else{
                    setAndStartVideo(mPlayData.getData());
                }
            }
        }
    }

    private void onHandleErrorEvent(int eventCode, Bundle bundle) {
        if(eventCode!=OnErrorListener.ERROR_CODE_NET_ERROR){
            judgeAdPlay();
        }
    }

    @Override
    public boolean isAdListFinish(){
        if(adVideos==null || mAdIndex < 0)
            return true;
        if(adVideos.size()<=0)
            return true;
        return mAdIndex > adVideos.size()-1;
    }
}
