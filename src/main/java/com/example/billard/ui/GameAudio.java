package com.example.billard.ui;

import com.example.billard.audio.SoundOutput;
import com.example.billard.audio.VolumeSettings;

/**
 * What the screens need for sound: where effects go (already scaled by the effects volume) and the volume settings
 * the player can change.
 */
public record GameAudio(SoundOutput effects, VolumeSettings volumes) {
}
