package com.MultiCamPack;

import android.app.Activity;
import android.app.Service;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;


import com.github.kohiyadav.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.kohiyadav.libffmpeg.FFmpeg;
import com.github.kohiyadav.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.kohiyadav.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.kohiyadav.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.util.ArrayList;

/*
    FFMpegService, created by Frankie McDonagh
    Date:

    This Activity contains code from from:
    programming experts, https://www.youtube.com/watch?v=0akRhC9njlg
 */

//See above re: programming experts
public class FFMpegService extends Service {
    FFmpeg ffMpeg;

    ArrayList<ProductionVideoModel> productionVideoModelArrayList = new ArrayList<>();
    int[] progressValues = new int[0];
    Callbacks activity;

    public MutableLiveData<Integer> percentage;
    IBinder myBinder = new LocalBinder();

    public void onStart(Intent intent, int startId){
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null){
            Bundle bundle = intent.getExtras();
            if(bundle != null) {
                productionVideoModelArrayList = bundle.getParcelableArrayList("productionList");
                progressValues = new int[productionVideoModelArrayList.size()];
            }
            for(int counter = 0; counter < productionVideoModelArrayList.size(); counter++) {
                ProductionVideoModel pvm = productionVideoModelArrayList.get(counter);
                String[] command = pvm.getCommand();
                Double duration = pvm.getDuration();
                try {
                    LoadFFMpegBinary();
                    executeFFmpegCommand(command, duration, counter);
                } catch (FFmpegNotSupportedException e) {
                    e.printStackTrace();
                } catch (FFmpegCommandAlreadyRunningException e) {
                    e.printStackTrace();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void executeFFmpegCommand(String[] command, final Double duration, final int counter) throws FFmpegCommandAlreadyRunningException {
        ffMpeg.execute(command, new ExecuteBinaryResponseHandler(){
            @Override
            public void onFailure(String message) {
                super.onFailure(message);
                Log.i("FFMpeg command failed:",message);
            }

            @Override
            public void onSuccess(String message) {
                super.onSuccess(message);
            }

            @Override
            //See above re: programming experts
            public void onProgress(String message) {
                String[] arr;
                if(message.contains("time=")){
                    arr = message.split("time=");
                    String var1 = arr[1];
                    String var2[] = var1.split(":");
                    String[] var3 = var2[2].split(" ");
                    String seconds = var3[0];
                    int hours = Integer.parseInt(var2[0]);
                    hours = hours * 3600;
                    int min = Integer.parseInt(var2[1]);
                    float sec = Float.valueOf(seconds);
                    float timeInSec = hours+min+sec;
                    int progress = (int)((timeInSec/duration)*100);
                    progressValues[counter] = progress;
                    percentage.setValue(getTotalProgressValue());
                }
            }

            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onFinish() {

            }
        });


    }
    private int getTotalProgressValue(){
        int total = 0;
        for (int progressValue : progressValues) {
            total += progressValue;
        }

        return total / progressValues.length;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            LoadFFMpegBinary();
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
        }
        percentage = new MutableLiveData<>();
    }

    private void LoadFFMpegBinary() throws FFmpegNotSupportedException {
        if(ffMpeg != null) { return; }

        ffMpeg = FFmpeg.getInstance(this);
        ffMpeg.loadBinary(new FFmpegLoadBinaryResponseHandler() {
            @Override
            public void onFailure() {
                Log.i("FFMpeg binary", "Failed to load binary");
            }

            @Override
            public void onSuccess() {
                Log.i("FFMpeg binary", "Successfully loaded binary");
            }

            @Override
            public void onStart() {
                Log.i("FFMpeg binary", "Started to load binary");
            }

            @Override
            public void onFinish() {
                Log.i("FFMpeg binary", "Finished to load binary");
            }
        });
    }

    public FFMpegService(){
        super();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    public class LocalBinder extends Binder
    {
        public FFMpegService getServiceInstance()
        {
            return FFMpegService.this;
        }
    }

    public void registerClient(Activity activity){
        this.activity = (Callbacks)activity;
    }

    public MutableLiveData<Integer> getPercentages() {
        return percentage;
    }

    public interface Callbacks{
        void updateClient(float data);
    }
}