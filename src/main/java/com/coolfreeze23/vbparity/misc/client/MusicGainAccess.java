package com.coolfreeze23.vbparity.misc.client;

/** Native {@code SoundEngine.updateCategoryVolume(MUSIC, gain)}: a music-wide fade gain kept by the sound engine. */
public interface MusicGainAccess {
    void vbp$setMusicGain(float gain);
}
