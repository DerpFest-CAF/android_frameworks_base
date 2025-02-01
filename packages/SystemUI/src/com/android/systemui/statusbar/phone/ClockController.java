/*
 * Copyright (C) 2018-2023 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.systemui.Dependency;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.res.R;
import com.android.systemui.statusbar.phone.ui.StatusBarIconController;
import com.android.systemui.statusbar.policy.Clock;

import org.derpfest.providers.DerpFestSettings;

public class ClockController {

    private static final String TAG = "ClockController";

    private static final int CLOCK_POSITION_RIGHT = 0;
    private static final int CLOCK_POSITION_CENTER = 1;
    private static final int CLOCK_POSITION_LEFT = 2;

    private Context mContext;
    private Clock mActiveClock, mCenterClock, mLeftClock, mRightClock;

    private int mClockPosition;
    private boolean mDenyListed;
    private boolean showClockBg;

    public ClockController(Context context, View statusBar) {
        mContext = context;

        mCenterClock = statusBar.findViewById(R.id.clock_center);
        mLeftClock = statusBar.findViewById(R.id.clock);
        mRightClock = statusBar.findViewById(R.id.clock_right);

        mActiveClock = mLeftClock;

        Uri iconHideList = Settings.Secure.getUriFor(StatusBarIconController.ICON_HIDE_LIST);
        Uri statusBarClock = Settings.System.getUriFor(
                DerpFestSettings.System.STATUS_BAR_CLOCK);
        Uri clockBgChip = Settings.System.getUriFor(DerpFestSettings.System.STATUSBAR_CLOCK_CHIP);
        ContentObserver contentObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                if (iconHideList.equals(uri)) {
                    mDenyListed = StatusBarIconController.getIconHideList(mContext,
                            Settings.Secure.getString(mContext.getContentResolver(),
                                    StatusBarIconController.ICON_HIDE_LIST)).contains("clock");
                } else if (statusBarClock.equals(uri)) {
                    mClockPosition = Settings.System.getInt(mContext.getContentResolver(),
                            DerpFestSettings.System.STATUS_BAR_CLOCK, CLOCK_POSITION_LEFT);
                } else if (clockBgChip.equals(uri)) {
                    showClockBg = Settings.System.getInt(mContext.getContentResolver(),
                            DerpFestSettings.System.STATUSBAR_CLOCK_CHIP, 0) == 1;
                }
                updateActiveClock();
            }
        };
        mContext.getContentResolver().registerContentObserver(iconHideList, false, contentObserver);
        mContext.getContentResolver().registerContentObserver(statusBarClock, false,
                contentObserver);
        mContext.getContentResolver().registerContentObserver(clockBgChip, false, contentObserver);
        contentObserver.onChange(true, iconHideList);
        contentObserver.onChange(true, statusBarClock);
        contentObserver.onChange(true, clockBgChip);
    }

    public Clock getClock() {
        switch (mClockPosition) {
            case CLOCK_POSITION_RIGHT:
                return mRightClock;
            case CLOCK_POSITION_CENTER:
                return mCenterClock;
            case CLOCK_POSITION_LEFT:
            default:
                return mLeftClock;
        }
    }

    private void updateActiveClock() {
        mContext.getMainExecutor().execute(() -> {
            mActiveClock.setClockVisibleByUser(false);
            removeDarkReceiver();
            mActiveClock = getClock();
            mActiveClock.setClockVisibleByUser(true);
            addDarkReceiver();

            // Override any previous setting
            mActiveClock.setClockVisibleByUser(!mDenyListed);

            // Add background chip
            mActiveClock.setBackgroundResource(R.drawable.sb_date_bg);
            mActiveClock.setPadding(10,2,10,2);
        });
    }

    public void addDarkReceiver() {
        Dependency.get(DarkIconDispatcher.class).addDarkReceiver(mActiveClock);
    }

    public void removeDarkReceiver() {
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(mActiveClock);
    }

    public void onDensityOrFontScaleChanged() {
        mActiveClock.onDensityOrFontScaleChanged();
    }

    private void addBackgroundChip(View vClock) {
        if (showClockBg) {
            vClock.setBackgroundResource(R.drawable.sb_date_bg);
            mActiveClock.setPadding(10,2,10,2);
        } else {
            int clockPaddingStart = mContext.getResources().getDimensionPixelSize(
                    R.dimen.status_bar_clock_starting_padding);
            int clockPaddingEnd = mContext.getResources().getDimensionPixelSize(
                    R.dimen.status_bar_clock_end_padding);
            int leftClockPaddingStart = mContext.getResources().getDimensionPixelSize(
                    R.dimen.status_bar_left_clock_starting_padding);
            int leftClockPaddingEnd = mContext.getResources().getDimensionPixelSize(
                    R.dimen.status_bar_left_clock_end_padding);
            if (vClock.getId() == R.id.clock) {
                vClock.setBackgroundResource(0);
                vClock.setPaddingRelative(leftClockPaddingStart, 0, leftClockPaddingEnd, 0);
            } else if (vClock.getId() == R.id.clock_center) {
                vClock.setBackgroundResource(0);
                vClock.setPaddingRelative(0,0,0,0);
            } else if (vClock.getId() == R.id.clock_right) {
                vClock.setBackgroundResource(0);
                vClock.setPaddingRelative(clockPaddingStart, 0, clockPaddingEnd, 0);
            }
        }
    }
}
