package com.shliama.augmentedvideotutorial;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {


    private Activity activity;
    private ProgressBar pb;


    public Utils() {
    }


    public Utils(Activity activity) {
        this.activity = activity;
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }


    private void DownloadImage(Context context, String MyUrl) {

        Picasso.get().load(MyUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {

                try {

                    String root = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
                    File myDir = new File(root, "NewVisionARImages");

                    if (!myDir.exists()) {
                        myDir.mkdirs();
                    }

                    myDir = new File(myDir, System.currentTimeMillis() + ".jpg");


                    FileOutputStream out = new FileOutputStream(myDir);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);


                    out.flush();
                    out.close();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Done Setting up AR  ....", Toast.LENGTH_LONG).show();
                            Intent i = new Intent(activity, MainActivity.class);
                            activity.startActivity(i);

                            activity.finish();
                        }
                    }, 9000);


                } catch (Exception e) {
                }
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                Toast.makeText(context, "Something wrong happened", Toast.LENGTH_LONG).show();
                System.out.println(" bitmap failed ");
                pb = activity.findViewById(R.id.pb);
                pb.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                Toast.makeText(context, "Loading AR....", Toast.LENGTH_LONG).show();
                System.out.println("On prepare ");
            }
        });
    }

    public void DownloadVideoAndImage(Context context, String videoURL, String imageURL) throws InterruptedException {

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(videoURL));
        Date date = new Date();
        SimpleDateFormat videoName = new SimpleDateFormat("yyyymmdd_HHmmss");
        String strDate = videoName.format(date);

        request.setDescription("Setting up the AR");
        request.setTitle("AR loading ...");

        pb = activity.findViewById(R.id.pb);
        pb.setVisibility(View.VISIBLE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }

        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, "/NewVisionVideos/" + strDate + System.currentTimeMillis() + ".mp4");

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);

        DownloadImage(context, imageURL);

        pb.setVisibility(View.GONE);

    }

}


