package com.gofu.cantonesechallenge;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class SongsManager {
	// Local Resources Path
    final String FILE_PATH = new String("android.resource://com.gofu.cantonesechallenge/raw/");
	private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();

	// Constructor
	public SongsManager(){

	}
	
	/**
	 * Function to read all audio files in local resources and store the details in ArrayList
	 * */
	public ArrayList<HashMap<String, String>> getPlayList(Context context){
        int identifier;
        String fileName;
        String packageName;
        String title;
        String path;

        // Fetch the resources under the raw folder into a string array
        Field[] fields=R.raw.class.getFields();

        // Put each resource into the song list
        for(Field field : fields){
            try {
                HashMap<String, String> song = new HashMap<String, String>();

                // Get the file name
                fileName = field.getName();

                // Get the package name (i.e. com.gofu.cantonesechallenge)
                packageName = context.getPackageName();

                // Get the resource id
                identifier = context.getResources().getIdentifier(fileName, "string", packageName);

                // Get the name of resource id
                title = context.getString(identifier);

                // Get the path of the file
                path = FILE_PATH+fileName;

                song.put("songTitle", title);
                song.put("songPath", path);
                songsList.add(song);
                Log.i("Raw asset added to playlist: ", fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // TO DO - Fetch the resources in external storage (e.g. SD card) into a song list

//        File dir = new File(Environment.getExternalStorageDirectory()+"/LearnCantonese");
//        File files[] = dir.listFiles();
//
//        for(File file : files) {
//            if (file.isFile()) {
//                try {
//                    HashMap<String, String> song = new HashMap<String, String>();
//
//                    // Get the file name without the extension
//                    title = file.getName().replaceFirst("[.][^.]+$", "");
//
//                    // Get the path of file
//                    path = file.getPath();
//
//                    song.put("songTitle", title);
//                    song.put("songPath", path);
//                    songsList.add(song);
//                    Log.i("Raw Asset: ", title);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }

        // return songs list array
		return songsList;
	}
	
	/**
	 * Class to filter files which are having a particular extension
	 * */
	class FileExtensionFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return (name.endsWith(".mp3") || name.endsWith(".MP3"));
		}
	}
}
