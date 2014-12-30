package com.example.printer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class DisplayPic extends Activity {
	private Context context; 
	private ArrayList<PicFileInfo> picFileList; 
	private ListView listView;
	private final int MENU_LIST1 = Menu.FIRST; 	 /* menu parameters*/
	private final String START_PIC_VIEWER_INTENT="com.example.printer.VIEW_MY_PIC";
	private final int THUMBNAIL_SIZE = 96;
	private Bitmap bMapImage, thumbImage; 
	private ProgressDialog progressDialog;
	private final int UPDATE_STATUS_COMPLETE=1000;
//	private final int RECEIVED_IP=2000; 
//	private final int IP_DISAPPEARED=6000; 
	private final String RECEIVED_IP="printer.com.example.received_ip"; 
//	private final String IP_DISAPPEARED="printer.com.example.ip_disappeared"; 
	private SignalReceiver myReceiver; 
	IntentFilter filterReceivedIP = new IntentFilter(RECEIVED_IP);
//	IntentFilter filterIPDisappeared = new IntentFilter(IP_DISAPPEARED);
	private boolean LOAD_PIC_THREAD_RUNNING=false; 
	private boolean CLICKED_VIEW_PIC=false; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d("Alex", "DisplayPic onCreate"); 
		context = this;
		picFileList = new ArrayList<PicFileInfo>(); 
		listView = (ListView) findViewById(R.id.listview);
		myReceiver = new SignalReceiver();  
//		ListenUDPBroadcast listenUDP = new ListenUDPBroadcast(handler); 
		ListenUDPBroadcast listenUDP = new ListenUDPBroadcast(context);
		new Thread(listenUDP).start();
//		ResponseCountdown responseCountdown = new ResponseCountdown(handler); 
//		ResponseCountdown responseCountdown = new ResponseCountdown(context); 
//		new Thread(responseCountdown).start(); 
		listView.setAdapter(new PicFileListAdapter(context, picFileList)); 
		clickItem(listView); 
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d("Alex", "DisplayPic onCreateOptionMenu"); 
		int idGroup1 = 0; 
		int orderMenuItem1 = Menu.NONE; 
		menu.add(idGroup1, MENU_LIST1, orderMenuItem1, R.string.show_pdf); 
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {			
		case MENU_LIST1:
			Intent intent = new Intent(this, DisplayPDF.class);
			startActivity(intent); 
			this.finish(); 
			break;
		default:
			Log.d("Error", "Menu error!"); 
		}
		return super.onOptionsItemSelected(item);
	}	

	protected void onPause() {
		Log.d("Alex", "DisplayPic onPause"); 
		super.onPause();
		unregisterReceiver(myReceiver);
	}
	
	protected void onResume() {
		Log.d("Alex", "DisplayPic onResume"); 
		super.onResume();
//		ListenUDPBroadcast.setHandler(handler);
//		ResponseCountdown.setHandler(handler);
		registerReceiver(myReceiver, filterReceivedIP);
		//registerReceiver(myReceiver, filterIPDisappeared);
		if (CLICKED_VIEW_PIC ==false) {
			picFileList.clear();
		} 
		if(LOAD_PIC_THREAD_RUNNING==false && CLICKED_VIEW_PIC==false) {
			Log.d("Alex", "run thread"); 
			progressDialog = ProgressDialog.show(context, getString(R.string.progress_dialog_title), getString(R.string.progress_dialog_content), false); //since it might take a while for info to load
//		getPicfiles(handler); 		
			ShowPictureList showPicList = new ShowPictureList(); 
			new Thread(showPicList). start();
			LOAD_PIC_THREAD_RUNNING=true; 
		}
		CLICKED_VIEW_PIC=false; 
	}
	
	public class ShowPictureList implements Runnable {
		public void run() {
			Log.d("Alex", "will call getPicFiles!"); 
			getPicfiles(handler);
		}
	}
	
	public void clickItem(ListView currentView) {
		currentView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				Log.d("Alex", "clickitem"); 
				String path=picFileList.get(position).getPath();
				String name=picFileList.get(position).getName(); 
//				Toast.makeText(context, path, Toast.LENGTH_SHORT).show();
				Intent viewPicIntent = new Intent(START_PIC_VIEWER_INTENT);
				viewPicIntent.putExtra("file_path", path);
				viewPicIntent.putExtra("file_name", name); 
				CLICKED_VIEW_PIC=true; 
				startActivity(viewPicIntent);
			}
		});
	}
	
	public static String getMimeType(String filePath)
	{
	    String type = null;
	    String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
	    if (extension != null) {
	        MimeTypeMap mime = MimeTypeMap.getSingleton();
	        type = mime.getMimeTypeFromExtension(extension);
	    }
	    return type;
	}
	
	public void getPicfiles(Handler h) {
		ContentResolver picFileResolver = context.getContentResolver();
//		Uri uri = MediaStore.Images.Media.getContentUri("External");
		Uri uri =MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		String[] projection = null;
		String selection = null;
		String[] selectionArgs =null;
		String sortOrder = null;
		
		Cursor picFileCr = picFileResolver.query(uri, projection, selection, selectionArgs, sortOrder);
//		Log.d("Alex", "picFileCr is: "+picFileCr); 
		if(picFileCr ==null) {
			Bitmap icon=BitmapFactory.decodeResource(getResources(), R.drawable.emo_im_embarrassed);
			picFileList.add(new PicFileInfo(getString(R.string.no_pic_found), "0", "", icon)); 
			listView.setEnabled(false);
		}
		if(picFileCr!=null && picFileCr.moveToFirst()){
			//get columns
//			int nameColumn = picFileCr.getColumnIndex(OpenableColumns.DISPLAY_NAME);
//			int sizeColumn = picFileCr.getColumnIndex(OpenableColumns.SIZE);
			int nameColumn = picFileCr.getColumnIndex(MediaStore.Images.Media.TITLE);
			int sizeColumn = picFileCr.getColumnIndex(MediaStore.Images.Media.SIZE);
			int pathColumn = picFileCr.getColumnIndex(MediaStore.Images.Media.DATA); 
			 
			//add picture file to list
			do {
				String thisName = picFileCr.getString(nameColumn);
				String thisSize = picFileCr.getString(sizeColumn);
				String thisPath = picFileCr.getString(pathColumn);
				String type = getMimeType(thisPath); 
//				Log.d("Alex", "type is: "+type); 
				if(type!=null && type.equals("image/jpeg")) {
					//make thumbnail for pics
					bMapImage = BitmapFactory.decodeFile(thisPath); 
					thumbImage = Bitmap.createScaledBitmap(bMapImage, THUMBNAIL_SIZE, THUMBNAIL_SIZE, false);

//					Log.d("Alex", "thisName is: "+thisName);
					/*
					Log.d("Alex", "thisSize is: "+thisSize);
					Log.d("Alex", "thisPath is: "+thisPath); */
					picFileList.add(new PicFileInfo(thisName, thisSize, thisPath, thumbImage));
				}
			}
			while (picFileCr.moveToNext());
//				Log.d("Alex", "picFileList size is: "+picFileList.size());
		}
		
//		Log.d("Alex", "will return message??");
		Message message = h.obtainMessage(UPDATE_STATUS_COMPLETE);
		h.sendMessage(message);
	}
	
	public class PicFileListAdapter extends BaseAdapter {
		private ArrayList<PicFileInfo> myPicList = null;
//		private LayoutInflater inflater; 
		private Context context;
		public PicFileListAdapter(Context ctx, ArrayList<PicFileInfo> inputList) {
			myPicList=inputList;
			context=ctx;
//			inflater=LayoutInflater.from(context);
		}
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return myPicList.size();
		}
		
		@Override
		public PicFileInfo getItem(int position) {
			// TODO Auto-generated method stub
			return myPicList.get(position);
		}
		
		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			//map to pic file layout
			
			ViewHolderPic viewHolderPic;
			if(convertView==null){
				// inflate the layout
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				convertView = inflater.inflate(R.layout.pic_file, parent, false);
				
				//set up the ViewHolder
				viewHolderPic = new ViewHolderPic();
				viewHolderPic.fileName = (TextView) convertView.findViewById(R.id.pic_name);
				viewHolderPic.fileSize = (TextView) convertView.findViewById(R.id.pic_size);
				viewHolderPic.fileThumbnail = (ImageView) convertView.findViewById(R.id.pic_thumbnail);  
				// store the holder with the view.
				convertView.setTag(viewHolderPic);
			}
			else{
				// we've just avoided calling findViewById() on resource every time but just use the viewHolder
				viewHolderPic = (ViewHolderPic) convertView.getTag();
			}
			// Pic file name and size based on the position
			PicFileInfo picFile = getItem(position);
			// assign values if the object is not null
			if(picFile != null) {
				// get the TextView from the ViewHolder and then set the text (item name) and tag (item ID) values
				viewHolderPic.fileName.setText(picFile.getName());
				viewHolderPic.fileSize.setText(picFile.getSize()+" bytes");
				viewHolderPic.fileThumbnail.setImageBitmap(picFile.getThumbnail());
			}
			return convertView;
		}
	}
	
	static class ViewHolderPic {
		TextView fileName;
		TextView fileSize;
		ImageView fileThumbnail;
	}
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
//			String ip;
			switch (msg.what) {
				case UPDATE_STATUS_COMPLETE:
					Log.d("Alex", "UPDATE_STATUS_COMPLETE"); 
					LOAD_PIC_THREAD_RUNNING=false;  
					Collections.sort(picFileList, new Comparator<PicFileInfo>() {
						public int compare(PicFileInfo a, PicFileInfo b){
							return a.getName().compareTo(b.getName()); 
						}
					});
					progressDialog.dismiss(); //info loaded ready, progress dialog can be dismissed.
					((BaseAdapter) listView.getAdapter()).notifyDataSetChanged(); //do this AFTER dismissing the progressDialog in order to avoid UI being too busy and getting stuck
					break;
					/*
				case RECEIVED_IP:
					Log.d("Alex", "DisplayPic RECEIVED_IP");
					ip = (String)msg.obj;
					Log.d("Alex", "DisplayPic RECEIVED_IP, ip is: "+ip); 
					PicViewer.setPrinterIP(ip); 
					ResponseCountdown.setFlag(true); 
					break;
				case IP_DISAPPEARED:
					Log.d("Alex", "IP_DISAPPEARED"); 
					ip = (String)msg.obj;
					Log.d("Alex", "IP_DISAPPEARED, ip is: "+ip);
					PicViewer.setPrinterIP(ip); 
					ResponseCountdown.setFlag(false); 
					break; */ 
			}
		}
	};
/*	
	private class MyReceiver extends BroadcastReceiver {
		@Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
			Log.d("Alex", "DisplayPic onReceive!"); 
            if(intent.getAction().equals(RECEIVED_IP)) {
            	Log.d("Alex", "DisplayPic got IP!");
            	String ip=intent.getStringExtra("IP"); 
            	PicViewer.setPrinterIP(ip); 
				ResponseCountdown.setFlag(true); 
            }
            else if(intent.getAction().equals(IP_DISAPPEARED)) {
            	Log.d("Alex", "DisplayPic no IP!");
            	PicViewer.setPrinterIP(null); 
				ResponseCountdown.setFlag(false); 
            }
            else Log.d("Alex", "DisplayPic receiver error!"); 
        }
    }
*/  
}
