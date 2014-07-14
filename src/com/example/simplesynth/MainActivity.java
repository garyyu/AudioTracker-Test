package com.example.simplesynth;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.io.*;

public class MainActivity extends Activity {

	private static final String TAG ="tracker-audiotest";

	Thread playing;
	Thread recording;

	int sr = 48000;
	boolean isRunning = true;
	SeekBar fSlider;
	double sliderval;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// point the slider to the GUI widget
		fSlider = (SeekBar) findViewById(R.id.frequency);

		// create a listener for the slider bar;
		OnSeekBarChangeListener listener = new OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) { }
			public void onStartTrackingTouch(SeekBar seekBar) { }
			public void onProgressChanged(SeekBar seekBar, 
					int progress,
					boolean fromUser) {
				if(fromUser) sliderval = progress / (double)seekBar.getMax();
				Log.v(TAG,"onProgressChanged(): slider val = "+ progress + "/" + seekBar.getMax());
			}
		};

		// set the listener on the slider
		fSlider.setOnSeekBarChangeListener(listener);

		/*--- start a new thread to record audio ---*/
		recording = new Thread() {
			public void run() {

				Log.v(TAG,"Recording Thread::run(): enter.");
				// set process priority
				setPriority(Thread.MAX_PRIORITY);

				// set the buffer size
				int samplesize = 0;
				int buffsize = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
				samplesize = (buffsize >> 1);
				Log.v(TAG,"Recording Thread::run(): AudioRecord.getMinBufferSize ="+ buffsize + ". samplesize="+ samplesize);

				// create an AudioRecord object
				AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
						sr, AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT, 2*buffsize);

				byte  bsamples[] = new byte[buffsize];
				int i=0,j=0;

				// start audio
				Log.v(TAG,"Recording Thread::run(): start audioRecord recording.");
				audioRecord.startRecording();

				try{
					FileOutputStream TxInFile=new FileOutputStream("/sdcard/txin.pcm");

					while(isRunning)
					{
						int byteRead = audioRecord.read(bsamples, 0, buffsize) ;
						if (byteRead<=0){
							Log.v(TAG,"Recording Thread::run(): audioRecord.read failure. ret="+byteRead);
						}

						TxInFile.write(bsamples);
						//Log.v(TAG,"Thread::run(): audioTrack.write done");
					}

					TxInFile.close();

				} catch ( IOException e){
					Log.v(TAG,"Recording Thread::run():File Exception");
				}

				audioRecord.stop();
				audioRecord.release();

				Log.v(TAG,"Recording Thread::run(): audioRecord stopped and released done.");
			}
		};

		/*--- start a new thread to play audio ---*/
		playing = new Thread() {
			public void run() {

				Log.v(TAG,"Playing Thread::run(): enter.");
				// set process priority
				setPriority(Thread.MAX_PRIORITY);

				// set the buffer size
				int samplesize = 0;
				int buffsize = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				samplesize = (buffsize >> 1);
				Log.v(TAG,"Playing Thread::run(): AudioTrack.getMinBufferSize ="+ buffsize + ". samplesize="+ samplesize);

				// create an audiotrack object
				AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
						sr, AudioFormat.CHANNEL_OUT_MONO,
						AudioFormat.ENCODING_PCM_16BIT, 2*buffsize,
						AudioTrack.MODE_STREAM);

				byte  bsamples[] = new byte[buffsize];
				int i=0,j=0;

				// start audio
				Log.v(TAG,"Playing Thread::run(): start audioTrack playing.");
				audioTrack.play();
				int silenceFilling = 0;

				try{
					FileInputStream  RxInFile=new FileInputStream ("/sdcard/rxin.pcm");
					FileOutputStream RxInFileLoop=new FileOutputStream("/sdcard/rxin-loop.pcm");

					while(isRunning)
					{
						if (silenceFilling > 0){
							for (i=0; i<buffsize; i++) bsamples[i] = 0;
							silenceFilling -= buffsize;
						}
						else
						{
							int readSize = RxInFile.read(bsamples);
							if (readSize < buffsize){
								RxInFile.close();
								RxInFile=new FileInputStream ("/sdcard/rxin.pcm");

								//Insert 5 seconds silent
								silenceFilling = 5 * sr * 2;
								for (i=0; i<buffsize; i++) bsamples[i] = 0;
							}
						}

						int byteWriten = audioTrack.write(bsamples, 0, bsamples.length);
						if (byteWriten<=0){
							Log.v(TAG,"Playing Thread::run(): audioTrack.write failure. ret="+byteWriten);
						}
						RxInFileLoop.write(bsamples);
						//Log.v(TAG,"Thread::run(): audioTrack.write done");
					}

					RxInFile.close();
					RxInFileLoop.close();

				} catch ( IOException e){
					Log.v(TAG,"Playing Thread::run():File Exception");
				}

				audioTrack.stop();
				audioTrack.release();

				Log.v(TAG,"Playing Thread::run(): audioTrack stopped and released done.");
			}
		};


		Log.v(TAG,"Recording Thread starting.");
		recording.start();        
		Log.v(TAG,"Playing Thread starting.");
		playing.start();        


		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
			.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onDestroy(){
		Log.v(TAG,"onDestroy() enter.");
		super.onDestroy();
		isRunning = false;
		try {
			playing.join();
			recording.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		playing = null;
		recording = null;
		Log.v(TAG,"onDestroy() left.");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

}
