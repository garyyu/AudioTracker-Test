This is a delay test project on Android using AudioTracker and AudioRecorder.

It's based on this example project for usage of AudioTracker: http://audioprograming.wordpress.com/2012/10/18/a-simple-synth-in-android-step-by-step-guide-using-the-java-sdk/.
I add the AudioRecorder for recording. Generally, I use a 48kHz audio file to play out and in the same record the input from microphone. Then I save the microphone recording
audio into a file, so I can read the delay from this file.

The building procedure:
$ ant debug

I'm using a Nexus 7 (2013) for this test and result is about 40~50ms, which is quite good result compared to the test using OpenSL-ES which I did before (see project: 
https://github.com/garyyu/OpenSL-ES-Android-DelayTest for detail).

But still a problem, I faced a suddenly delay increasing during the recording procedure. In my test, it happened on 6'33"012 (on captured file: tracker-audiotest-45msdelay.wav), 
and delay increase from 45ms to 340ms. A possible useful hint is: I see this trace when problem happen:
	W/AudioTrack(  607): AUDIO_OUTPUT_FLAG_FAST denied by client due to mismatching sample rate (44100 vs 48000)
So far, I'm not clear what does that mean, I'm using 48kHz sample rate and never change it during this recording!



