package com.colin.framework.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class PalLog {
	
	
	public static final String TAG = "com.windo.log";

	public static final int TO_CONSOLE = 0x1;
	public static final int TO_SCREEN = 0x10;
	public static final int TO_FILE = 0x100;
	public static final int FROM_LOGCAT = 0x1000;

	public static final int DEBUG_ALL = TO_CONSOLE | TO_FILE ;

	private static final String LOG_FOLDER = "/logs";
	private static final String LOG_TEMP_FILE = "log.temp";
	private static final String LOG_LAST_FILE = "log_last.txt";
	private static final String LOG_NOW_FILE = "log_now.txt";

	private static final int LOG_MAXSIZE = 2 * 1024 * 1024; 
	
//	public static boolean DEBUG = KaihuManager.OPEN_LOG;
	
	int LOG_LEVEL = Log.VERBOSE;

	private Object lockObj = new Object();

	PaintLogThread mPaintLogThread = null;
	
	OutputStream mLogStream;
	
	long mFileSize;
	
	Context mContext;

	static PalLog mLog;
	

	public static PalLog getInstance(){
		if(mLog == null){
            synchronized (PalLog.class){
                if(mLog==null){
			        mLog = new PalLog();
                }
            }
		}
		return mLog;
	}
	
	
	public void setContext(Context context){
		mContext = context.getApplicationContext();
	}
	
	public static void d(String tag, String msg, int toWhat) {
		getInstance().log(tag, msg, toWhat, Log.DEBUG);
	}
	
	public static void d(String tag, String msg) {
		getInstance().log(tag, msg, DEBUG_ALL, Log.DEBUG);
	}

	public static void v(String tag, String msg) {
		getInstance().log(tag, msg, DEBUG_ALL, Log.VERBOSE);
	}

	public static void v(String tag, String msg, int toWhat) {
		getInstance().log(tag, msg, toWhat, Log.VERBOSE);
	}

	public static void e(String tag, String msg) {
		getInstance().log(tag, msg, DEBUG_ALL, Log.ERROR);
	}

	public static void i(String tag, String msg) {
		getInstance().log(tag, msg, DEBUG_ALL, Log.INFO);
	}

	public static void w(String tag, String msg) {
		getInstance().log(tag, msg, DEBUG_ALL, Log.WARN);
	}

	protected  void log(String tag, String msg, int outdest, int level) {
		if (tag == null)
			tag = "TAG_NULL";
		if (msg == null)
			msg = "MSG_NULL";

		if (level >= LOG_LEVEL) {

			if ((outdest & TO_CONSOLE) != 0) {
				LogToConsole(tag, msg, level);
			}

			if ((outdest & TO_SCREEN) != 0) {
				LogToScreen(tag, msg, level);
			}

			if ((outdest & TO_FILE) != 0) {
				LogToFile(tag, msg, level);
			}
			

			if ((outdest & FROM_LOGCAT) != 0) {

				 if(mPaintLogThread == null){
					 mPaintLogThread = new PaintLogThread();
    				 mPaintLogThread.start();
				 }
			}
		}

	}

//	Calendar mDate = Calendar.getInstance();
	Date mDate = new Date();
	SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
	StringBuffer mBuffer = new StringBuffer();

	
	private String getLogStr(String tag, String msg, int leve) {

//		mDate.setTimeInMillis(System.currentTimeMillis());
		mDate.setTime(System.currentTimeMillis());

		mBuffer.setLength(0);
		mBuffer.append(sdf.format(mDate));
		mBuffer.append(" ");
		mBuffer.append(levelToStr(leve));
		mBuffer.append("/");
		mBuffer.append(tag);
		mBuffer.append(":");
		mBuffer.append(msg);

		return mBuffer.toString();
	}

	private String levelToStr(int level) {
		String str = "";
		switch (level) {
			case Log.DEBUG:
				str = "D";
				break;
			case Log.ERROR:
				str = "E";
				break;
			case Log.INFO:
				str = "I";
				break;
			case Log.VERBOSE:
				str = "V";
				break;
			case Log.WARN:
				str = "W";
				break;
			default:
				break;
		}
		return str;
	}

	
	private void LogToConsole(String tag, String msg, int level) {
		tag = "PalLog/" + tag;
		switch (level) {
		case Log.DEBUG:
			Log.d(tag, msg);
			break;
		case Log.ERROR:
			Log.e(tag, msg);
			break;
		case Log.INFO:
			Log.i(tag, msg);
			break;
		case Log.VERBOSE:
			Log.v(tag, msg);
			break;
		case Log.WARN:
			Log.w(tag, msg);
			break;
		default:
			break;
		}
	}

	
	private void LogToFile(String tag, String msg, int level) {
		synchronized (lockObj) {
			OutputStream outStream = openLogFileOutStream();
			if (outStream != null) {
				try {
					byte[] d = getLogStr(tag, msg, level).getBytes("utf-8");
					
					if (mFileSize < LOG_MAXSIZE) {
						outStream.write(d);
						outStream.write("\r\n".getBytes());
						outStream.flush();
						mFileSize += d.length;
					} else {
						closeLogFileOutStream();
						if(renameLogFile()){
							LogToFile(tag, msg, level);	
						}
					}
				} catch (Exception e) {
					
					e.printStackTrace();
				}

			}
		}
	}

	private void LogToScreen(String tag, String msg, int level) {

	}

	
	public  void backLogFile() {
		if(mContext == null){
			return ;
		}
		File cacheFolder = getLogFolder();
		synchronized (lockObj) {
			try {
				closeLogFileOutStream();
				
				File destFile = new File(cacheFolder,LOG_NOW_FILE);
				if (destFile.exists()) {
					destFile.delete();
				}
				
				try {
					destFile.createNewFile();
				} catch (IOException e1) {
					
					e1.printStackTrace();
					return;
				}

				File srcfile1 = new File(cacheFolder,LOG_TEMP_FILE);
				File srcfile2 = new File(cacheFolder,LOG_LAST_FILE);
				copyFile(srcfile1,destFile,false);
				copyFile(srcfile2,destFile,true);
				
				openLogFileOutStream();

			} catch (IOException e) {
				
				e.printStackTrace();
				Log.w("NeteaseLog", "backLogFile fail:" + e.toString());
			}
		}
	}

	

	
	private OutputStream openLogFileOutStream() {
		if (mLogStream == null && mContext != null) {
			try {
				
				File file = new File(getLogFolder(),LOG_TEMP_FILE);
				if (file.exists()) {
					mLogStream = new FileOutputStream(file, true);
					mFileSize = file.length();
				} else {
					
					mLogStream = new FileOutputStream(file);
					mFileSize = 0;
				}
			} catch (FileNotFoundException e) {
				
				e.printStackTrace();
			} 
		}
		return mLogStream;
	}
	
	
	
	
	protected File getLogFolder() {

		File folder = null;
		
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			File dir = mContext.getExternalFilesDir("");
//			File sdcard = Environment.getExternalStorageDirectory();
			folder = new File(dir, LOG_FOLDER);
			if(!folder.exists()){
				folder.mkdirs();
			}
		}else{
			folder = mContext.getFilesDir();
			if(!folder.exists()){
				folder.mkdirs();
			}
		}

		return folder;
	}
























	
	private void closeLogFileOutStream() {
		try {
			if (mLogStream != null) {
				mLogStream.close();
				mLogStream = null;
				mFileSize = 0;
			}
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	
	

	
	private void copyFile(File src, File dest, boolean destAppend)
			throws IOException {

		if (!destAppend && dest.exists()) {
			dest.delete();
		}
		long total = src.length();
		FileOutputStream out = new FileOutputStream(dest, destAppend);
		FileInputStream in = new FileInputStream(src);
		long count = 0;
		byte[] temp = new byte[1024 * 10];
		while (count < total) {
			int size = in.read(temp);
			out.write(temp, 0, size);
			count += size;
		}
		in.close();
		in = null;
		out.close();
		out = null;

	}
	
	
	private boolean renameLogFile() {
		
		synchronized (lockObj) {
			
			File file = new File(getLogFolder(),LOG_TEMP_FILE);
			File destFile = new File(getLogFolder(),LOG_LAST_FILE);
			if (destFile.exists()) {
				destFile.delete();
			}
			file.renameTo(destFile);
			if(file.exists()){	
				return file.delete();	
			}else{
				return true;
			}
		}
	}

	
	public  boolean zipLogFile(String zipFileName) {
		if(mContext == null){
			return false;
		}
		
		backLogFile();
		File zipfile = new File(zipFileName);
		if (zipfile.exists()) {
			zipfile.delete();
		}
		File srcfile = new File(getLogFolder(),LOG_NOW_FILE);
		boolean ret = zip(srcfile, zipfile);
		
		srcfile = null;
		zipfile = null;
		return ret;
	}
	

	public void close() {

		if (mPaintLogThread != null) {
			mPaintLogThread.shutdown();
			mPaintLogThread = null;
		}
		closeLogFileOutStream();
		
	}

	class PaintLogThread extends Thread {

		Process mProcess;
		boolean mStop = false;

		public void shutdown() {
			Log.i("PaintLogThread", "shutdown");
			mStop = true;
			if (mProcess != null) {
				mProcess.destroy();
				mProcess = null;
			}
		}

		public void run() {
			
			try {
				Log.i("PaintLogThread", "shutdown");
				ArrayList<String> commandLine = new ArrayList<String>();
				commandLine.add("logcat");
				

				commandLine.add("-v");
				commandLine.add("time");

				
				
				
				

				mProcess = Runtime.getRuntime()
								  .exec(commandLine.toArray(new String[commandLine.size()]));

				BufferedReader bufferedReader = new BufferedReader
				
				(new InputStreamReader(mProcess.getInputStream()));

				String line = null;
				while (!mStop) {
					line = bufferedReader.readLine();
					if (line != null) {
						LogToFile("SysLog", line, Log.VERBOSE);
					} else {
						if (line == null) {
							Log.i("PaintLogThread:", "readLine==null");
							break;
							
							
							
						}
						

					}
				}

				bufferedReader.close();
				if (mProcess != null)
					mProcess.destroy();
				mProcess = null;
				mPaintLogThread = null;
				Log.i("PaintLogThread:", "end PaintLogThread:");

			} catch (Exception e) {
				
				e.printStackTrace();
				Log.d("NeteaseLog", "logcatToFile Exception:" + e.toString());
			}
		}
	}

	private  boolean zip(File unZip, File zip) {
		if (!unZip.exists())
			return false;
		if (!zip.getParentFile().exists())
			zip.getParentFile().mkdir();

		try {
			FileInputStream in = new FileInputStream(unZip);
			FileOutputStream out = new FileOutputStream(zip);

			ZipOutputStream zipOut = new ZipOutputStream(out);

			
			byte[] buf = new byte[1024];

			int readCnt = 0;

			zipOut.putNextEntry(new ZipEntry(unZip.getName()));
			while ((readCnt = in.read(buf)) > 0) {
				zipOut.write(buf, 0, readCnt);
			}
			zipOut.closeEntry();

			in.close();
			zipOut.close();

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

}
