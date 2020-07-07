package com.shliama.augmentedvideotutorial;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {


    public static void DownloadImage(Context context, String MyUrl) {
        Picasso.get().load(MyUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {

                try {
                    String root = context.getExternalFilesDir(null).toString();
                    File myDir = new File(root, "NewVisionAR");

                    if (!myDir.exists()) {
                        myDir.mkdirs();
                    }

                    String name = new SimpleDateFormat("yyyymmdd_HHmmss", Locale.getDefault()) + ".jpg";
                    myDir = new File(myDir, name);

                    Toast.makeText(context, "Load ...." + root, Toast.LENGTH_LONG).show();

                    System.out.println("--Huxy path " + myDir);
                    FileOutputStream out = new FileOutputStream(myDir);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);

                    out.flush();
                    out.close();
                } catch (Exception e) {
                }
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                Toast.makeText(context, "Something wrong happened", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                Toast.makeText(context, "Loading ....", Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void DownloadVideo(Context context, String URL) {

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(URL));
        Date date = new Date();
        SimpleDateFormat videoName = new SimpleDateFormat("yyyymmdd_HHmmss");
        String strDate = videoName.format(date);
        request.setDescription("Setting up the AR");
        request.setTitle("AR setup ...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/NewVisionAR/" + "test.mp4");

        System.out.println("video path " + context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/NewVisionAR/" + "strDate" + ".mp4");
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);

    }


}
