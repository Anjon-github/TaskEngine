package com.colin.framework.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class FileUtils {

    private static final String IMG_DIR = "images";
    private static final String VIDEO_DIR = "videos";
    private static final String DB_DIR = "databases";
    private static final String DOWNLOAD_DIR = "download";
    private static final String LOG_DIR = "logs";

    public static File getHttpCacheDir(Context con) {
        File dir = getDir(con, true, false);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        dir = new File(dir, "HttpCache");
        dir.mkdir();
        return dir;
    }

    public static File getFile(Context con, String name) {
        return new File(getDir(con), name);
    }

    public static File getLogDir(Context con) {
        File file = new File(getDir(con), LOG_DIR);
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    public static File getImageDir(Context con) {
        File dir = new File(getDir(con), IMG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static String md5(byte[] src) {
        String des = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(src);
            byte[] dis = md5.digest();
            des = hex(dis);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return des;
    }

    /**
     * 转16进制算法
     *
     * @param src
     * @return
     */
    public static String hex(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static File getDownloadFile(Context con, String url) {
        File dir = getDownloadDir(con);
        String md5 = md5(url.getBytes());
        String fileName = null;

        if(md5 != null && md5.length() != 0){
            fileName = md5;
        }else{
            int index = url.lastIndexOf("\\");
            fileName = url.substring(index);
        }

        if (fileName == null || fileName.length() == 0) {
            fileName = System.currentTimeMillis() + "";
        }

        Log.d("FileUtils", "fileName " + fileName);

        return new File(dir, fileName);
    }

    public static File getDownloadDir(Context con) {
        File dir = new File(getDir(con), DOWNLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getVideoDir(Context con, String phoneNum) {
        File dir = new File(getDir(con), VIDEO_DIR + File.separator + phoneNum);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getDBDir(Context con) {
        File dir = new File(getContextDir(con), DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getContextDir(Context con) {
        File dir = con.getFilesDir();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getDir(Context con) {
        return getDir(con, false, false);
    }

    /**
     * 获取存储目录
     *
     * @param con
     * @param isCacheDir 是否为缓存文件夹
     * @return
     */
    public static File getDir(Context con, boolean isCacheDir, boolean isInternalDir) {
        String state = Environment.getExternalStorageState();
        File dir = null;
        if (Environment.MEDIA_MOUNTED.equals(state) && !isInternalDir) {
            if (isCacheDir) {
                dir = con.getExternalCacheDir();
            } else {
                dir = con.getExternalFilesDir(null);
            }
        }
        //防止dir为空的情况
        if (dir == null) {
            if (isCacheDir) {
                dir = con.getCacheDir();
            } else {
                dir = con.getFilesDir();
            }
        }
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static void deleteFile(File file) {
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File fileTmp : files) {
                    deleteFile(fileTmp);
                }
                deleteFinalFile(file);
            } else {
                deleteFinalFile(file);
            }
        }
    }

    private static void deleteFinalFile(File file) {
        File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
        file.renameTo(to);
        selfDeleteFile(to);
    }

    public static String getContentFromAssetsFile(Context context, String fileName) {
        try {
            return getContentFromIs(context.getAssets().open(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getContentFromIs(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return stringBuilder.toString();
    }

    public static Properties getProperFromAssets(Context context, String fileName) throws IOException {
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(context.getAssets().open(fileName), "UTF-8");
            Properties properties = new Properties();
            properties.load(inputStreamReader);
            return properties;
        } finally {
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean copyFile(File source, File target) {
        FileChannel in = null;
        FileChannel out = null;
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            File parent = target.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                return false;
            }
            inStream = new FileInputStream(source);
            outStream = new FileOutputStream(target);
            in = inStream.getChannel();
            out = outStream.getChannel();
            return in.transferTo(0, in.size(), out) == in.size();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(inStream);
            close(in);
            close(outStream);
            close(out);
        }
        return false;
    }

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void selfDeleteFile(File file) {
        if (file != null && file.exists() && !file.delete()) {
            file.deleteOnExit();
        }
    }
}
