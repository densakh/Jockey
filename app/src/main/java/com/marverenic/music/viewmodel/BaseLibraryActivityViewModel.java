package com.marverenic.music.viewmodel;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableFloat;
import android.databinding.ObservableInt;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.view.View;

import com.android.databinding.library.baseAdapters.BR;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.player.PlayerController;

import javax.inject.Inject;

import timber.log.Timber;

public class BaseLibraryActivityViewModel extends BaseObservable {

    private Context mContext;

    @Inject PlayerController mPlayerController;

    private final int mExpandedHeight;
    private final ObservableInt mMiniplayerHeight;
    private final ObservableFloat mMiniplayerAlpha;
    private boolean mMiniplayerVisible;

    private boolean mAnimateSlideInOut;

    public BaseLibraryActivityViewModel(BaseActivity activity) {
        mContext = activity;
        JockeyApplication.getComponent(mContext).inject(this);

        mExpandedHeight = mContext.getResources().getDimensionPixelSize(R.dimen.miniplayer_height);
        mAnimateSlideInOut = false;
        mMiniplayerVisible = true;

        mMiniplayerHeight = new ObservableInt(0);
        mMiniplayerAlpha = new ObservableFloat(1.0f);

        setPlaybackOngoing(false);

        mPlayerController.getNowPlaying()
                .compose(activity.bindToLifecycle())
                .map(nowPlaying -> nowPlaying != null)
                .subscribe(this::setPlaybackOngoing, throwable -> {
                    Timber.e(throwable, "Failed to set playback state");
                });
    }

    private void setPlaybackOngoing(boolean isPlaybackOngoing) {
        if (mAnimateSlideInOut) {
            animateTranslation(isPlaybackOngoing);
        } else {
            mMiniplayerHeight.set((isPlaybackOngoing) ? mExpandedHeight : 0);
        }
    }

    private void animateTranslation(boolean isPlaybackOngoing) {
        int startOffset = mMiniplayerHeight.get();
        int endOffset;

        TimeInterpolator interpolator;
        if (isPlaybackOngoing) {
            endOffset = mExpandedHeight;
            interpolator = new LinearOutSlowInInterpolator();
        } else {
            endOffset = 0;
            interpolator = new FastOutLinearInInterpolator();
        }

        ObjectAnimator slideAnimation = ObjectAnimator.ofInt(
                mMiniplayerHeight, "", startOffset, endOffset);
        slideAnimation.setInterpolator(interpolator);
        slideAnimation.setDuration(225);
        slideAnimation.start();
    }

    @Bindable
    public ObservableInt getMiniplayerHeight() {
        return mMiniplayerHeight;
    }

    @Bindable
    public ObservableFloat getMiniplayerAlpha() {
        return mMiniplayerAlpha;
    }

    @Bindable
    public int getMiniplayerVisibility() {
        return (mMiniplayerVisible) ? View.VISIBLE : View.GONE;
    }

    public BottomSheetBehavior.BottomSheetCallback getBottomSheetCallback() {
        return new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                mMiniplayerVisible = newState != BottomSheetBehavior.STATE_EXPANDED;
                notifyPropertyChanged(BR.miniplayerVisibility);
                // TODO Hide content and release bindings in the playing page to avoid extra work
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                mMiniplayerAlpha.set(1.0f - slideOffset);
            }
        };
    }

    public void onActivityExitForeground() {
        mAnimateSlideInOut = false;
    }

    public void onActivityEnterForeground() {
        mAnimateSlideInOut = true;
    }

}
