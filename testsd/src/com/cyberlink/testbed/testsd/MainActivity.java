package com.cyberlink.testbed.testsd;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	public static final File INVALID_DIRECTORY = new File("");
	private static final String ASSET_FILE_NAME ="test01.mp3";
	private TextView mPrimary;
	private TextView mSecondary;
	private Button mBtnShowList;
	
	private class OnClickBtnShowListener implements OnClickListener {
		@Override
		public void onClick(View arg0) {
			String message = "";
			List<File> storages = getAvailableStorage();
			if (storages.size() > 0) {
				mPrimary.setText(storages.get(0).getAbsolutePath());
				String destPath = storages.get(0).getAbsolutePath()+File.separator+ASSET_FILE_NAME;
				copyTestFile(destPath);
				File destFile = new File(destPath);
				if (destFile.exists()) {
					message += "Primary: Successfully !\n";
				} else {
					message += "Primary: Failed !\n";
				}
			}
			
			if (storages.size() == 1) {
				mSecondary.setText("N/A");
			} else if (storages.size() > 1) {
				mSecondary.setText(storages.get(1).getAbsolutePath());				
				String destPath = storages.get(1).getAbsolutePath()+File.separator+ASSET_FILE_NAME;
				copyTestFile(destPath);
				
				File destFile = new File(destPath);
				if (destFile.exists()) {
					message += "Secondary: Successfully !\n";
				} else {
					message += "Secondary: Failed !\n";
				}
			}
			
			AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
			.setTitle("Copy File Test")
			.setMessage(message)
			.setPositiveButton("OK",null)
			.create();
			dialog.show();
		}
	}
	
	private void copyTestFile(String destPath) {
		
		// Delete exist file
		File f = new File(destPath);
		if (f.exists()) {
			f.delete();
		}
		
		try {
			InputStream src = getResources().getAssets().open(ASSET_FILE_NAME);
			FileOutputStream dest = new FileOutputStream(destPath);
        	copyFile(src, dest);
        	src.close();
        	src = null;
        	dest.flush();
        	dest.close();
        	dest = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean writeTestFile(File folder) {
		File[] files = folder.listFiles();
		for (File f : files) {
			f.delete();
		}
		
		Date d = Calendar.getInstance().getTime();
		File f = new File (folder.getAbsolutePath() + File.separator + d.toString());
		try {
			f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		if (!f.exists()) {
			return false;
		}
		return true;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mPrimary = (TextView) findViewById(R.id.primary_folder);
		mSecondary = (TextView) findViewById(R.id.secondary_folder);
		mBtnShowList = (Button) findViewById(R.id.btn_list);
		mBtnShowList.setOnClickListener(new OnClickBtnShowListener());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
    
    private List<File> getAvailableStorage() {
    	
    	List<File> mounts = new ArrayList<File>();
    	
    	Context ct = MainActivity.this.getApplicationContext();
    	if (ct == null) {
    		return mounts;
    	}
    	
		// https://developer.android.com/about/versions/android-4.4.html
		// http://source.android.com/devices/tech/storage/#multiple-external-storage-devices
		// Starting in Android 4.4, multiple external storage devices are surfaced to developers through 
		// Context.getExternalFilesDirs(), Context.getExternalCacheDirs(), and Context.getObbDirs()
    	if (Build.VERSION.SDK_INT >= 19) {
    		File[] files = ct.getExternalFilesDirs(null);
    		for (File f: files) {
    			if (f!=null) {
    				mounts.add(f);
    			}
    		}
    		return mounts;
    	}
    	
    	File primary = ct.getExternalFilesDir(null);
    	if (primary != null) {
    		mounts.add(primary);
    	}
    	
    	// Non-official method, try to enumerate mounted storages
    	Set<File> mountDirs = getMounts();
		String appExternalFileDir = ct.getExternalFilesDir(null).getAbsolutePath();
		for (File dir : mountDirs) {
			if (!dir.getAbsolutePath().equals("/mnt/sdcard") &&
				!dir.getAbsolutePath().equals(Environment.getExternalStoragePublicDirectory("").getAbsolutePath()) &&
					dir.getAbsolutePath().toLowerCase().contains("sd") &&
					dir.exists() && dir.canRead()) {
				File directory = new File(dir.getAbsolutePath() + appExternalFileDir.substring(appExternalFileDir.indexOf("/Android")));
		        if (!directory.exists()) {
		        	directory.mkdirs();
		        }
		        
		        if (directory.exists()) {
		        	mounts.add(directory);
		        }
			}
		}
		return mounts;
	}
    
    private Set<File> getMounts() {
    	Set<File> mounts = new HashSet<File>();
    	Scanner scanner = null;
        try {
            scanner = new Scanner(new File("/proc/mounts"));
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.startsWith("/dev/block/vold/")) {
                    String[] lineElements = line.split(" ");
                    String element = lineElements[1];
                    mounts.add(new File(element));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	if (scanner != null) {
        		scanner.close();
        	}
        }
        return mounts;
    }
    
    private boolean copyFile(InputStream src, OutputStream dest) {
    	final int BUF_SIZE_IN_BYTE = 8192;
    	BufferedInputStream bis = null;
    	BufferedOutputStream bos = null;
		try {
			bis = new BufferedInputStream(src);
			bos = new BufferedOutputStream(dest);
            byte[] buf = new byte[BUF_SIZE_IN_BYTE];
            int len;
            while((len = bis.read(buf, 0, BUF_SIZE_IN_BYTE)) > 0) {
            	bos.write(buf, 0, len);
            }
            bos.close();
            bis.close();
		} catch (IOException e) {
            if (bos != null) {
                try {
                	bos.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            if (bis != null) {
                try {
                	bis.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            return false;
		}
		return true;
    }
}
