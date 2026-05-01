package com.mygdx.game.net;

/**
 * Callback for diagnostic roundtrip measurements.
 * Implemented by the HTML platform layer to push data into {@code window.baischDiag}.
 * No-op on non-browser platforms.
 */
public interface DiagListener {
    void onRoundtrip(int seq, int ms);
}
