package com.coolfreeze23.vbparity.misc.client;

/** Client state for the native "Now Playing" toast rules. */
public final class MusicToastState {
    private MusicToastState() {}

    /** Native {@code !toastShown}: a song started while music was muted, so raising the slider should show its toast. */
    public static boolean toastPending;
    /** True while PauseScreen.render runs: native ticks the pause-menu music notes in tick(), not per frame. */
    public static boolean inPauseScreenRender;
}
