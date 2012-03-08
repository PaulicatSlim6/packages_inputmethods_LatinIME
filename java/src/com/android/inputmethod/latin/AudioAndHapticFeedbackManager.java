/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.inputmethod.latin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.HapticFeedbackConstants;

import com.android.inputmethod.compat.VibratorCompatWrapper;
import com.android.inputmethod.keyboard.Keyboard;
import com.android.inputmethod.keyboard.KeyboardSwitcher;
import com.android.inputmethod.keyboard.LatinKeyboardView;

/**
 * This class gathers audio feedback and haptic feedback functions.
 *
 * It offers a consistent and simple interface that allows LatinIME to forget about the
 * complexity of settings and the like.
 */
public class AudioAndHapticFeedbackManager extends BroadcastReceiver {
    final private LatinIME mLatinIme;
    final private SettingsValues mSettingsValues;
    final private KeyboardSwitcher mKeyboardSwitcher;
    private AudioManager mAudioManager;
    final private VibratorCompatWrapper mVibrator;
    private boolean mSilentModeOn;

    public AudioAndHapticFeedbackManager(final LatinIME latinIme,
            final SettingsValues settingsValues, final KeyboardSwitcher keyboardSwitcher) {
        mLatinIme = latinIme;
        mSettingsValues = settingsValues;
        mKeyboardSwitcher = keyboardSwitcher;
        mSilentModeOn = true;
        mVibrator = VibratorCompatWrapper.getInstance(mLatinIme);
    }

    public void hapticAndAudioFeedback(final int primaryCode) {
        vibrate();
        playKeyClick(primaryCode);
    }

    private boolean isSoundOn() {
        return mSettingsValues.mSoundOn && !mSilentModeOn;
    }

    // update flags for silent mode
    private void updateRingerMode() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) mLatinIme.getSystemService(Context.AUDIO_SERVICE);
            if (mAudioManager == null) return;
        }
        mSilentModeOn = (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL);
    }

    private void playKeyClick(int primaryCode) {
        // if mAudioManager is null, we don't have the ringer state yet
        // mAudioManager will be set by updateRingerMode
        if (mAudioManager == null) {
            if (mKeyboardSwitcher.getKeyboardView() != null) {
                updateRingerMode();
            }
        }
        if (isSoundOn()) {
            final int sound;
            switch (primaryCode) {
            case Keyboard.CODE_DELETE:
                sound = AudioManager.FX_KEYPRESS_DELETE;
                break;
            case Keyboard.CODE_ENTER:
                sound = AudioManager.FX_KEYPRESS_RETURN;
                break;
            case Keyboard.CODE_SPACE:
                sound = AudioManager.FX_KEYPRESS_SPACEBAR;
                break;
            default:
                sound = AudioManager.FX_KEYPRESS_STANDARD;
                break;
            }
            mAudioManager.playSoundEffect(sound, mSettingsValues.mFxVolume);
        }
    }

    // TODO: make this private when LatinIME does not call it any more
    public void vibrate() {
        if (!mSettingsValues.mVibrateOn) {
            return;
        }
        if (mSettingsValues.mKeypressVibrationDuration < 0) {
            // Go ahead with the system default
            LatinKeyboardView inputView = mKeyboardSwitcher.getKeyboardView();
            if (inputView != null) {
                inputView.performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
        } else if (mVibrator != null) {
            mVibrator.vibrate(mSettingsValues.mKeypressVibrationDuration);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
            updateRingerMode();
        }
    }
}
