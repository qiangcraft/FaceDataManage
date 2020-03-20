package com.example.addface.env;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;


public class FileUtils {
    private static final String TAG = "FileUtils";
    private static final Logger LOGGER = new Logger();
    public static final String ROOT =
            Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "faceUtils";
    public static final String DATA_FILE = "face_data";

    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param bitmap The bitmap to save.
     * @param filename The location to save the bitmap to.
     */
    public static void saveBitmap(final Bitmap bitmap, final String filename) {
        Log.i(TAG, String.format("Saving %dx%d bitmap to %s.", bitmap.getWidth(), bitmap.getHeight(), ROOT));
        LOGGER.i("Saving %dx%d bitmap to %s.", bitmap.getWidth(), bitmap.getHeight(), ROOT);
        final File myDir = new File(ROOT);

        if (!myDir.mkdirs()) {
            Log.i(TAG, "Make dir failed");
        }

        final File file = new File(myDir, filename);
        if (file.exists()) {
            file.delete();
        }
        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            Log.e(TAG, "Save Bitmap Excetion!");
        }
    }
    public static Bitmap creatBitmap(Bitmap origin, RectF rectF){
        int left = rectF.left<0?0:(int)rectF.left;
        int top = rectF.top<0?0:(int)rectF.top;
        int width = rectF.left+rectF.width()>origin.getWidth()?(int)(origin.getWidth()-rectF.left):(int)rectF.width();
        int height = rectF.top+rectF.height()>origin.getHeight()?(int)(origin.getHeight()-rectF.top):(int)rectF.height();
        return Bitmap.createBitmap(origin,left,top,width,height);
    }
    public static void copyAsset(AssetManager mgr, String filename) {
        InputStream in = null;
        OutputStream out = null;

        try {
            File file = new File(ROOT + File.separator + filename);
            if (!file.exists()) {
                file.createNewFile();
            }

            in = mgr.open(filename);
            out = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int read;
            while((read = in.read(buffer)) != -1){
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            Log.e(TAG, "CopyAsset Excetion!");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "CopyAsset IOExcetion!");
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, "CopyAsset IOExcetion!");
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void appendText(String text, String filename) {
        try {
            if(readFileByLine(filename).size()>0){
                text = System.lineSeparator()+text;
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        try(FileWriter fw = new FileWriter(ROOT + File.separator + filename, true);
            PrintWriter out = new PrintWriter(new BufferedWriter(fw))) {
            out.print(text);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
            Log.e(TAG, "AppendText IOExcetion!");
        }
    }

    //用于读取assets中的data
    public static ArrayList<String> readFileByLine(String filename) throws FileNotFoundException {
        Scanner s = new Scanner(new File(ROOT + File.separator + filename));
        ArrayList<String> list = new ArrayList<>();
        while (s.hasNextLine()){
            list.add(s.nextLine());
        }
        s.close();
        return list;
    }

    //删除此人对应的data和label中的第index行
    public static void deleteDataByIndex(int index){
        String dataPath = ROOT + File.separator + DATA_FILE; // 数据文件路径
        FileModify obj = new FileModify();
        obj.write(dataPath, obj.read(dataPath, index)); // 读取数据文件

    }
}
