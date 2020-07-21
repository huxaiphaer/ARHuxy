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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class Utils {

    File[] allImagesList;
    File[] allVideosList;


    public static void DownloadImage(Context context, String MyUrl) {

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

                    Toast.makeText(context, "downloading  ...." + root, Toast.LENGTH_LONG).show();

                    FileOutputStream out = new FileOutputStream(myDir);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    System.out.println("Done downloading ");
                    Toast.makeText(context, "Done downloading  ...." + root, Toast.LENGTH_LONG).show();

                    out.flush();
                    out.close();
                } catch (Exception e) {
                }
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                Toast.makeText(context, "Something wrong happened", Toast.LENGTH_LONG).show();
                System.out.println(" bitmap failed ");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                Toast.makeText(context, "Loading ....", Toast.LENGTH_LONG).show();
                System.out.println("On prepare ");
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

        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, "/NewVisionVideos/" + strDate + System.currentTimeMillis() + ".mp4");

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);

    }

    public void ListAllFiles(Context context) {

        List<File> imageNames = new ArrayList<>();
        List<File> videoNames = new ArrayList<>();
        Map<File, File> map;
        File[] allImagesList;
        File[] allVideosList;


        //Handling Images
        String root = context.getExternalFilesDir(null).toString();
        File allImages = new File(root, "NewVisionARImages");
        allImagesList = allImages.listFiles();

        File allVideos = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES + "/NewVisionVideos/");
        allVideosList = allVideos.listFiles();

        for (int i = 0; i < allVideosList.length; i++) {
            videoNames.add(allVideosList[i]);
        }
        for (int i = 0; i < allImagesList.length; i++) {
            imageNames.add(allImagesList[i]);
        }


        map = IntStream.range(0, imageNames.size())
                .collect(
                        HashMap::new,
                        (m, i) -> m.put(imageNames.get(i), videoNames.get(i)),
                        Map::putAll
                );

    }


}
