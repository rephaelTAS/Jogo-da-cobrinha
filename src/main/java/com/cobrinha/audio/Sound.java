package com.cobrinha.audio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * Efeitos sonoros sintetizados em tempo real — nenhum arquivo de áudio é
 * necessário. A síntese roda em uma thread daemon, e qualquer indisponibilidade
 * de áudio (comum em servidores e contêineres) apenas desliga o som.
 */
public final class Sound {

    private static final float SAMPLE_RATE = 44_100f;
    private static final AudioFormat FORMAT =
            new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "cobrinha-som");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    private volatile boolean audioAvailable = true;
    private SourceDataLine line;

    public void playStart() {
        submit(() -> play(synth(430, 720, 130, 0.13, 0.0)));
    }

    public void playEatSmall(int smallsEaten) {
        double base = 690 + Math.min(smallsEaten, 12) * 28;
        submit(() -> play(synth(base, base * 1.38, 80, 0.13, 0.035)));
    }

    public void playEatBig() {
        submit(() -> {
            play(synth(760, 1140, 110, 0.16, 0.0));
            play(synth(1140, 1520, 160, 0.15, 0.0));
        });
    }

    public void playBonusSpawn() {
        submit(() -> play(synth(520, 1040, 220, 0.12, 0.05)));
    }

    public void playBonusExpire() {
        submit(() -> play(synth(460, 150, 280, 0.10, 0.0)));
    }

    public void playWrap() {
        submit(() -> play(synth(560, 290, 150, 0.07, 0.0)));
    }

    public void playMazeShift() {
        submit(() -> play(synth(175, 80, 300, 0.12, 0.0)));
    }

    public void playGameOver() {
        submit(() -> play(synth(310, 78, 520, 0.18, 0.0)));
    }

    public void playWin() {
        submit(() -> {
            play(synth(660, 880, 130, 0.14, 0.0));
            play(synth(990, 1320, 220, 0.14, 0.0));
        });
    }

    public void playUiClick() {
        submit(() -> play(synth(1080, 940, 42, 0.055, 0.0)));
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean value) {
        enabled.set(value);
    }

    public void toggle() {
        enabled.set(!enabled.get());
    }

    public void close() {
        worker.shutdownNow();
        closeLine();
    }

    private void submit(Runnable task) {
        if (!enabled.get() || !audioAvailable) {
            return;
        }
        try {
            worker.execute(() -> {
                if (enabled.get() && audioAvailable) {
                    task.run();
                }
            });
        } catch (RuntimeException ignored) {
            // Executor encerrado durante o fechamento da janela.
        }
    }

    private void play(byte[] pcm) {
        if (!enabled.get() || !audioAvailable) {
            return;
        }
        try {
            if (line == null || !line.isOpen()) {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(FORMAT);
                line.start();
            }
            line.write(pcm, 0, pcm.length);
            line.drain();
        } catch (Exception e) {
            audioAvailable = false;
            closeLine();
        }
    }

    private void closeLine() {
        SourceDataLine current = line;
        line = null;
        if (current != null) {
            try {
                current.stop();
                current.close();
            } catch (Exception ignored) {
                // Nada a fazer ao encerrar.
            }
        }
    }

    /**
     * Gera um chirp senoidal com um leve segundo harmônico, ataque rápido e
     * decaimento exponencial. Frequências em Hz e duração em milissegundos.
     */
    private static byte[] synth(double startHz, double endHz, int durationMs,
                                double volume, double vibrato) {
        int samples = Math.max(1, (int) (SAMPLE_RATE * durationMs / 1000.0));
        byte[] data = new byte[samples * 2];
        double phase = 0;
        double attackSamples = SAMPLE_RATE * 0.006;
        for (int i = 0; i < samples; i++) {
            double t = i / (double) samples;
            double frequency = startHz + (endHz - startHz) * t;
            if (vibrato > 0) {
                frequency += Math.sin(2 * Math.PI * 9 * i / SAMPLE_RATE) * vibrato * frequency;
            }
            phase += 2 * Math.PI * frequency / SAMPLE_RATE;
            double attack = Math.min(1.0, i / attackSamples);
            double decay = Math.pow(1.0 - t, 2.1);
            double wave = Math.sin(phase) + 0.22 * Math.sin(2 * phase);
            short value = (short) (wave * attack * decay * volume * Short.MAX_VALUE);
            data[i * 2] = (byte) (value & 0xFF);
            data[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
        }
        return data;
    }
}
