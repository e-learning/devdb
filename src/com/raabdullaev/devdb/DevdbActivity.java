package com.raabdullaev.devdb;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.raabdullaev.devdb.R;

import android.view.WindowManager.LayoutParams;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class DevdbActivity extends Activity{
    Button btnLoad;
    ListView lv;
    List<Map<String, String>> items;
    SimpleAdapter adapter;
    LayoutInflater inflater; 
    View progress;
    DownloadLinksTask dlt;
    OnItemClickListener deviceSelectListener;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initElements();
        setOnClickEvents();
        
        if(dlt!=null){
            if(dlt.getStatus() == AsyncTask.Status.FINISHED){
                lv.setAdapter(adapter);
                items.addAll(dlt.bitems);
                adapter.notifyDataSetChanged();
            }
            else {
                lv.addFooterView(progress);
                lv.setAdapter(adapter);
                btnLoad.setEnabled(false);
                dlt.attach(this);
            }
        }
    }
    
    private void initElements(){    	
    	btnLoad = (Button) findViewById(R.id.button1);
        lv = (ListView) findViewById(R.id.listView1);
        items = new ArrayList<Map<String, String>>();
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        progress = inflater.inflate(R.layout.footer, null);
        dlt = (DownloadLinksTask) getLastNonConfigurationInstance();
       
        adapter = new SimpleAdapter(
        	this, 
            items,
            android.R.layout.simple_list_item_2, 
            new String[] { "text", "link" }, 
            new int[] { android.R.id.text1, android.R.id.text2 }
        );
    }
   
    private void addRows(Elements links){
    	TableLayout table = (TableLayout) findViewById(R.id.table0);
		for (Element link : links) {
			
            String[] propertyAndValueArr = link.text().split(":");
            
			if(propertyAndValueArr.length>0 && propertyAndValueArr[0].equals("Ссылки")) break;
            
			TableRow row = new TableRow(this);
			row.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT)); 
			
			TextView t1 = new TextView(this);
            t1.setText(propertyAndValueArr[0]);
            t1.setLayoutParams(new  TableRow.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
            t1.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(t1);
            
            row.setBackgroundColor(0xf2222222);
            
            if(propertyAndValueArr.length > 1){
            	TextView t2 = new TextView(this);
                t2.setText(propertyAndValueArr[1]);
                t2.setLayoutParams(new  TableRow.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
                t2.setGravity(Gravity.CENTER_VERTICAL);
                row.addView(t2);
                
                row.setBackgroundColor(0xff222222);
            }
            
            table.addView(row);
        }
    }
    
    private void setOnClickEvents(){
        btnLoad.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                btnLoad.setEnabled(false);
                items.removeAll(items);
                adapter.notifyDataSetChanged();
                dlt = new DownloadLinksTask(DevdbActivity.this);
                dlt.execute("http://devdb.ru/pda");
            }
        });
        
        class GetModelInfoThread extends Thread{
        	int pos;
        	Elements links;
        	ArrayList<Bitmap> myBitmaps;
        	
        	
        	GetModelInfoThread(int pos){
        		this.pos = pos;
        	}
        	
        	public void run(){
        		HashMap<String, String> hashMap = ((HashMap<String, String>)lv.getItemAtPosition(pos));
            	
            	String url = null;
            	for(String key: hashMap.keySet()){
            		url = hashMap.get(key);
            	}
            	
            	try {
            		Document doc = Jsoup.connect(url).get();
            		links = doc.select("div#container2 tr");
            		
            		Elements photos = doc.select("div#container2 img[src]");
            		myBitmaps = new ArrayList<Bitmap>();
            		
            		for(Element photo : photos){
            			String srcUrlString = photo.attr("src");
            			
            			if(srcUrlString.contains("http")){ // В списке много мусора, но все ссылки локальные, кроме картинок
            				URL srcUrl = new URL(srcUrlString);
            				HttpURLConnection connection = (HttpURLConnection) srcUrl.openConnection();
            				connection.setDoInput(true);
            				connection.connect();
            				InputStream input = connection.getInputStream();
            				myBitmaps.add(BitmapFactory.decodeStream(input));
            			}
                	}
            	} catch(Exception e){
            		e.printStackTrace();
            	}
        	}
        }
        
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	HashMap<String, String> hashMap = ((HashMap<String, String>)lv.getItemAtPosition(position));
            	
            	String url = null;
            	for(String key: hashMap.keySet()){
            		url = hashMap.get(key);
            	}
            	
                items.removeAll(items);
                adapter.notifyDataSetChanged();
                dlt = new DownloadLinksTask(DevdbActivity.this);
                dlt.execute(url);
                
                lv.setOnItemClickListener(new OnItemClickListener() {
                	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                		GetModelInfoThread modelInfoThread = new GetModelInfoThread(position);
                		modelInfoThread.start();
                		try{
                			modelInfoThread.join();
                		} catch(Exception e){
                			e.printStackTrace();
                		}
                		
                		setContentView(R.layout.device);
                		
                		addImgs(modelInfoThread.myBitmaps);
                		addRows(modelInfoThread.links);
                	}
                });
            }
        });
    }
    
    private void addImgs(ArrayList<Bitmap> myBitmaps){
    	Gallery gallery = (Gallery) findViewById(R.id.gallery);
		gallery.setAdapter(new ImageAdapter(this, myBitmaps));
    }
    
    void onComplete(List result){
        items.addAll(result);
        lv.removeFooterView(progress);
        adapter.notifyDataSetChanged();
        btnLoad.setEnabled(true);        
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        dlt.detach();
        return dlt;
    }

    public class ImageAdapter extends BaseAdapter {
        private Context myContext;
        private ArrayList<Bitmap> arrImg;
           
        public ImageAdapter(Context c, ArrayList<Bitmap> arrImg) { 
           this.myContext = c; 
           this.arrImg = arrImg;
        }

        /** Returns the amount of images we have defined. */
        public int getCount() { return arrImg.size(); }
        
        /* Use the array-Positions as unique IDs */
        public Object getItem(int position) { return position; }
        public long getItemId(int position) { return position; }

        /** Returns a new ImageView to
         * be displayed, depending on
         * the position passed. */
         public View getView(int position, View convertView, ViewGroup parent) {
        	 ImageView i = new ImageView(this.myContext);
               
             i.setImageBitmap(arrImg.get(position));
             i.setScaleType(ImageView.ScaleType.FIT_CENTER);
             i.setLayoutParams(new Gallery.LayoutParams(150, 150));
               
             return i;
         }

         /** Returns the size (0.0f to 1.0f) of the views
            * depending on the 'offset' to the center. */
         public float getScale(boolean focused, int offset) {
             /* Formula: 1 / (2 ^ offset) */
             return Math.max(0, 1.0f / (float)Math.pow(2, Math.abs(offset)));
       }
    }
    
    private class DownloadLinksTask extends AsyncTask<String, Void, List> {
        List<Map<String, String>> bitems = new ArrayList<Map<String, String>>();
        DevdbActivity act=null;
        
        DownloadLinksTask(DevdbActivity activity) {    
            attach(activity);    
        }
        
        void attach(DevdbActivity activity) {            
            this.act=activity;    
        }
        
        void detach() {    
            act=null;        
        }    
        
        @Override
        protected void onPreExecute() {
            if(lv!=null) {
                lv.addFooterView(progress);
                lv.setAdapter(adapter);
            }
        }
        
        @Override
        protected List doInBackground(String... urls) {
            try{
                Document doc = Jsoup.connect(urls[0]).get();
                Elements links = doc.select(".brandlist_left a[href]");
                
                for (Element link : links) {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("text", link.text());
                    map.put("link", link.attr("href"));
                    bitems.add(map);
                }

            }catch(Exception e){
                e.printStackTrace();
            }
            
            return bitems;
        }
        
        @Override
        protected void onPostExecute(List result) {
            if(act!=null) act.onComplete(result);
        }
    }
}