package com.shliama.augmentedvideotutorial;

import android.os.Bundle;
import android.os.Environment;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class LauncherActivity extends AppCompatActivity {

    private static String VIDEO_URL = "https://res.cloudinary.com/do6g6dwlz/video/upload/v1594290013/l4jvaiwfj9qfssn0kfrl.mp4";
    private static String IMAGE_URL = "https://res.cloudinary.com/do6g6dwlz/image/upload/v1594038092/khxcksgccvojkjpjttpa.jpg";
    boolean done = false;
    private ProgressBar pb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        pb = (ProgressBar) findViewById(R.id.pb);


        Utils utils = new Utils(LauncherActivity.this);

        try {

            // Remove images
            String imagesRoot = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
            File imagesDir = new File(imagesRoot, "NewVisionARImages");
            utils.deleteRecursive(imagesDir);

            // Remove videos.
            String videoRoot = this.getExternalFilesDir(Environment.DIRECTORY_MOVIES).toString();
            File videosDir = new File(videoRoot, "NewVisionVideos");
            utils.deleteRecursive(videosDir);

            utils.DownloadVideoAndImage(this, VIDEO_URL, IMAGE_URL);


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}