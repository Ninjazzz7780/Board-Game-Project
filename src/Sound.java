import javax.sound.sampled.*;
import java.io.File;

public class Sound {
    private Clip backsoundClip;

    public void playBacksound(String filePath) {
        stopBacksound();

        try {
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                System.out.println("Sound Error: File backsound tidak ditemukan: " + filePath);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            backsoundClip = AudioSystem.getClip();
            backsoundClip.open(audioStream);

            if (backsoundClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) backsoundClip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-10.0f);
            }

            backsoundClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            System.out.println("Sound Error: Gagal memuat/memutar backsound: " + e.getMessage());
        }
    }

    public void playSoundEffect(String filePath) {
        try {
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                System.out.println("Sound Error: File sound effect tidak ditemukan: " + filePath);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);

            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-5.0f);
            }

            clip.start();

            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });

        } catch (Exception e) {
            System.out.println("Sound Error: Gagal memutar sound effect: " + e.getMessage());
        }
    }

    public void stopBacksound() {
        if (backsoundClip != null && backsoundClip.isRunning()) {
            backsoundClip.stop();
            backsoundClip.close();
        }
    }
}