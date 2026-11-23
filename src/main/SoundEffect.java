package main;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Short sound effects loaded from res/Audio/. Music is handled separately by Music.java. */
public class SoundEffect {
    private static final String AUDIO_DIRECTORY = "/Audio/";
    private final Map<String, Clip> clips = new HashMap<>();
    private boolean enabled = true;
    private float volume = 0.7f;
    private Clip footsteps;

    /** Play a one-shot effect. A sound already playing is restarted rather than layered over itself. */
    public synchronized void play(String fileName) {
        if (!enabled || fileName == null || fileName.trim().isEmpty()) return;
        Clip clip = getClip(fileName);
        if (clip == null) return;
        clip.stop();
        clip.setFramePosition(0);
        applyVolume(clip);
        clip.start();
    }

    /** Start or continue the looping footstep effect. */
    public synchronized void startFootsteps(String fileName) {
        if (!enabled) return;
        if (footsteps == null || !footsteps.isOpen()) {
            footsteps = getClip(fileName);
        }
        if (footsteps != null && !footsteps.isRunning()) {
            applyVolume(footsteps);
            footsteps.setFramePosition(0);
            footsteps.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public synchronized void stopFootsteps() {
        if (footsteps != null && footsteps.isRunning()) {
            footsteps.stop();
            footsteps.setFramePosition(0);
        }
    }

    private Clip getClip(String fileName) {
        Clip existing = clips.get(fileName);
        if (existing != null && existing.isOpen()) return existing;

        String path = AUDIO_DIRECTORY + fileName;
        try (InputStream raw = getClass().getResourceAsStream(path)) {
            if (raw == null) {
                System.err.println("SoundEffect: file not found: " + path);
                return null;
            }
            try (AudioInputStream audio = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(raw))) {
                Clip clip = AudioSystem.getClip();
                clip.open(audio);
                clips.put(fileName, clip);
                return clip;
            }
        } catch (Exception ex) {
            System.err.println("SoundEffect: could not load " + path);
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) stopFootsteps();
    }

    public synchronized void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        for (Clip clip : clips.values()) applyVolume(clip);
    }

    private void applyVolume(Clip clip) {
        if (clip == null || !clip.isOpen()) return;
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float db = volume <= 0.0001f ? gain.getMinimum()
                    : (float) (20.0 * Math.log10(volume));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
        }
    }

    public synchronized void close() {
        for (Clip clip : clips.values()) {
            clip.stop();
            clip.close();
        }
        clips.clear();
        footsteps = null;
    }
}
