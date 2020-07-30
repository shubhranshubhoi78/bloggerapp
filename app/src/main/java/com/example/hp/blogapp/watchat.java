package com.example.hp.blogapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.assistant.v1.Assistant;
import com.ibm.watson.developer_cloud.assistant.v1.model.InputData;
import com.ibm.watson.developer_cloud.assistant.v1.model.MessageOptions;
import com.ibm.watson.developer_cloud.assistant.v1.model.MessageResponse;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.SynthesizeOptions;

import java.io.InputStream;
import java.util.ArrayList;


public class watchat extends AppCompatActivity {

    String imageurl="https://www.google.com/imgres?imgurl=https%3A%2F%2Fhe-s3.s3.amazonaws.com%2Fmedia%2Favatars%2Faayush349%2Fresized%2F180%2Fe1dc576dscn0118.jpg&imgrefurl=https%3A%2F%2Fwww.hackerearth.com%2F%40aayush349&docid=4kBVQWwWZRO_BM&tbnid=pHb32vwFAb-vFM%3A&vet=10ahUKEwjH8pSckYjnAhW_yDgGHZVKCyEQMwg_KAAwAA..i&w=180&h=180&bih=715&biw=1440&q=aayush%20hindwan&ved=0ahUKEwjH8pSckYjnAhW_yDgGHZVKCyEQMwg_KAAwAA&iact=mrc&uact=8";
    private RecyclerView recyclerView;
    private ChatAdapter mAdapter;
    private ArrayList messageArrayList;
    private EditText inputMessage;
    private ImageButton btnSend;
    private ImageButton btnRecord;
    //private Map<String,Object> context = new HashMap<>();
    com.ibm.watson.developer_cloud.assistant.v1.model.Context context = null;
    StreamPlayer streamPlayer;
    private boolean initialRequest;
    private boolean permissionToRecordAccepted = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String TAG = "MainActivity";
    private static final int RECORD_REQUEST_CODE = 101;
    private boolean listening = false;
    private SpeechToText speechService;
    private MicrophoneInputStream capture;
    private SpeakerLabelsDiarization.RecoTokens recoTokens;
    private MicrophoneHelper microphoneHelper;
    final TextToSpeech textService = new TextToSpeech();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAnimation();
        setContentView(R.layout.activity_watchat);

        inputMessage = findViewById(R.id.message);
        btnSend = findViewById(R.id.btn_send);
        //btnRecord= findViewById(R.id.btn_record);
       // String customFont = "Montserrat-Regular.ttf";
       // Typeface typeface = Typeface.createFromAsset(getAssets(), customFont);
        //inputMessage.setTypeface(typeface);
        recyclerView = findViewById(R.id.recycler_view);

        messageArrayList = new ArrayList<>();
        mAdapter = new ChatAdapter(messageArrayList);
        microphoneHelper = new MicrophoneHelper(this);


        LinearLayoutManager layoutManager = new LinearLayoutManager(watchat.this);
        layoutManager.setStackFromEnd(true);


          recyclerView.setLayoutManager(layoutManager);

          recyclerView.setItemAnimator(new DefaultItemAnimator());
          recyclerView.setAdapter(mAdapter);
          this.inputMessage.setText("");
          this.initialRequest = true;

        sendMessage();

        //Watson Text-to-Speech Service on IBM Cloud

        //Use "apikey" as username and apikey values as password
        textService.setUsernameAndPassword("apikey", "yZfWjj710Je9XbHzEj1B-ebCHeZM52Jla-b9z6qtioVn");
        textService.setEndPoint("https://api.eu-gb.text-to-speech.watson.cloud.ibm.com/instances/744a99e9-20e4-4353-b172-1d6f432abcce");

        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied");
            makeRequest();
        }


        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        Message audioMessage;
                        try {

                            audioMessage =(Message) messageArrayList.get(position);
                            streamPlayer = new StreamPlayer();
                            if(audioMessage != null && !audioMessage.getMessage().isEmpty()) {
                                SynthesizeOptions synthesizeOptions = new SynthesizeOptions.Builder()
                                        .text(audioMessage.getMessage())
                                        .voice(SynthesizeOptions.Voice.EN_US_LISAVOICE)
                                        .accept(SynthesizeOptions.Accept.AUDIO_WAV)
                                        .build();
                                streamPlayer.playStream(textService.synthesize(synthesizeOptions).execute());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }

            @Override
            public void onLongClick(View view, int position) {
                recordMessage();

            }
        }));

        btnSend.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(checkInternetConnection()) {
                    sendMessage();
                }
            }
        });

        // btnRecord.setOnClickListener(new View.OnClickListener() {
        //  @Override public void onClick(View v) {
        // recordMessage();
        // }
        // });
    };
    public void setAnimation(){
        if(Build.VERSION.SDK_INT>20) {
            Slide slide = new Slide();
            slide.setSlideEdge(Gravity.LEFT);
            slide.setDuration(400);
            slide.setInterpolator(new DecelerateInterpolator());
            getWindow().setExitTransition(slide);
            getWindow().setEnterTransition(slide);
        }
    }

    // Speech to Text Record Audio permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case RECORD_REQUEST_CODE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permission has been denied by user");
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                }
                return;
            }
            case MicrophoneHelper.REQUEST_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission to record audio denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
        // if (!permissionToRecordAccepted ) finish();

    }

    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                MicrophoneHelper.REQUEST_PERMISSION);
    }


    // Sending a message to Watson Conversation Service
    private void sendMessage() {

        final String inputmessage = this.inputMessage.getText().toString().trim();
        if(inputmessage.equals("new"))
        {
        }
        if(!watchat.this.initialRequest) {
            Message inputMessage = new Message();
            inputMessage.setMessage(inputmessage);
            inputMessage.setId("1");
            messageArrayList.add(inputMessage);
        }
        else
        {
            Message inputMessage = new Message();
            inputMessage.setMessage(inputmessage);
            inputMessage.setId("100");
            this.initialRequest = false;
            Toast.makeText(getApplicationContext(),"Tap on the message for Voice",Toast.LENGTH_LONG).show();

        }

        this.inputMessage.setText("");
        mAdapter.notifyDataSetChanged();

        Thread thread = new Thread(new Runnable(){
            public void run() {
                try {

                    Assistant assistantservice = new Assistant("2019-02-28");
                    //If you like to use USERNAME AND PASSWORD
                    //Your Username: "apikey", password: "<APIKEY_VALUE>"
                    assistantservice.setUsernameAndPassword("apikey", "fAs3WLETcGAEiVeK7xIIG3HCB9C5yj2kmRsjPrYLZDse");

                    //TODO: Uncomment this line if you want to use API KEY
                    //assistantservice.setApiKey("vjstzh5mrjSkQXJRiJ86X46YdvR4yjD1ETwCTnR8SOon");

                    //Set endpoint which is the URL. Default value: https://gateway.watsonplatform.net/assistant/api
                    assistantservice.setEndPoint("https://api.eu-gb.assistant.watson.cloud.ibm.com/instances/24b769b5-7eef-4e22-a37d-9eda78364d1b");
                    InputData input = new InputData.Builder(inputmessage).build();

                    //WORKSPACES are now SKILLS
                    MessageOptions messageOptions = new MessageOptions.Builder("5c75f18a-39bd-42fe-9bd8-ca19916bc6b8")
                            .input(input)
                            .build();
                    MessageResponse response = assistantservice.message(messageOptions).execute();
                    Log.i(TAG, "run: "+response);

                    String outputText = "";
                    int length=response.getOutput().getText().size();
imageurl="https://www.google.com/imgres?imgurl=https%3A%2F%2Fhe-s3.s3.amazonaws.com%2Fmedia%2Favatars%2Faayush349%2Fresized%2F180%2Fe1dc576dscn0118.jpg&imgrefurl=https%3A%2F%2Fwww.hackerearth.com%2F%40aayush349&docid=4kBVQWwWZRO_BM&tbnid=pHb32vwFAb-vFM%3A&vet=10ahUKEwjH8pSckYjnAhW_yDgGHZVKCyEQMwg_KAAwAA..i&w=180&h=180&bih=715&biw=1440&q=aayush%20hindwan&ved=0ahUKEwjH8pSckYjnAhW_yDgGHZVKCyEQMwg_KAAwAA&iact=mrc&uact=8";
                    if(response.getOutput().getGeneric().size()==2)
                    { imageurl=response.getOutput().getGeneric().get(1).getSource(); }



                    Log.i(TAG, "run_aayush: "+length);
                    if(length>1) {
                        for (int i = 0; i < length; i++) {
                            outputText += '\n' + response.getOutput().getText().get(i).trim();
                        }
                    }
                    else
                        outputText = response.getOutput().getText().get(0);

                    Log.i(TAG, "runaayu: "+response);
                    //Passing Context of last conversation
                    if(response.getContext() !=null)
                    {
                        //context.clear();
                        context = response.getContext();

                    }
                    Message outMessage=new Message();
                    if(response!=null)
                    {
                        if(response.getOutput()!=null && response.getOutput().containsKey("text"))
                        {
                            ArrayList responseList = (ArrayList) response.getOutput().get("text");
                            if(null !=responseList && responseList.size()>0){
                                outMessage.setMessage(outputText);
                                outMessage.setUrl(imageurl);
                                outMessage.setId("2");
                                Log.i(TAG, "runau: "+outputText);
                            }
                            messageArrayList.add(outMessage);
                            /*

                            streamPlayer = new StreamPlayer();
                            if(outMessage != null && !outMessage.getMessage().isEmpty()) {

                                SynthesizeOptions synthesizeOptions = new SynthesizeOptions.Builder()
                                        .text(outMessage.getMessage())
                                        .voice(SynthesizeOptions.Voice.EN_US_LISAVOICE)
                                        .accept(SynthesizeOptions.Accept.AUDIO_WAV)
                                        .build();
                                streamPlayer.playStream(textService.synthesize(synthesizeOptions).execute());

                            }

                             */




                        }

                        runOnUiThread(new Runnable() {
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                                if (mAdapter.getItemCount() > 1) {
                                    recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount()-1);

                                }

                            }
                        });


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

    }
    //Record a message via Watson Speech to Text
    private void recordMessage() {
        speechService = new SpeechToText();
        //Use "apikey" as username and apikey as your password
        speechService.setUsernameAndPassword("apikey", "_DF2Wu4HAh6HSKtbc1SfkMaLn857BlSTAqIvzu2eqey5");
        //Default: https://stream.watsonplatform.net/text-to-speech/api
        speechService.setEndPoint("https://api.eu-gb.speech-to-text.watson.cloud.ibm.com/instances/4641d806-96aa-44f5-969c-86fd28165947");

        if(listening != true) {
            capture = microphoneHelper.getInputStream(true);
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        speechService.recognizeUsingWebSocket(getRecognizeOptions(capture), new MicrophoneRecognizeDelegate());
                    } catch (Exception e) {
                        showError(e);
                    }
                }
            }).start();
            listening = true;
            Toast.makeText(watchat.this,"Listening....Click to Stop", Toast.LENGTH_LONG).show();

        } else {
            try {
                microphoneHelper.closeInputStream();
                listening = false;
                Toast.makeText(watchat.this,"Stopped Listening....Click to Start", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Check Internet Connection
     * @return
     */
    private boolean checkInternetConnection() {
        // get Connectivity Manager object to check connection
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        // Check for network connections
        if (isConnected){
            return true;
        }
        else {
            Toast.makeText(this, " No Internet Connection available ", Toast.LENGTH_LONG).show();
            return false;
        }

    }

    //Private Methods - Speech to Text
    private RecognizeOptions getRecognizeOptions(InputStream audio) {
        return new RecognizeOptions.Builder()
                .audio(audio)
                .contentType(ContentType.OPUS.toString())
                .model("en-US_BroadbandModel")
                .interimResults(true)
                .inactivityTimeout(2000)
                //TODO: Uncomment this to enable Speaker Diarization
                //.speakerLabels(true)
                .build();
    }

    private class MicrophoneRecognizeDelegate extends BaseRecognizeCallback {

        @Override
        public void onTranscription(SpeechRecognitionResults speechResults) {
            System.out.println(speechResults);
            //TODO: Uncomment this to enable Speaker Diarization
            /*SpeakerLabelsDiarization.RecoTokens recoTokens = new SpeakerLabelsDiarization.RecoTokens();
            if(speechResults.getSpeakerLabels() !=null)
            {
                recoTokens.add(speechResults);
                Log.i("SPEECHRESULTS",speechResults.getSpeakerLabels().get(0).toString());


            }*/
            if(speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
                String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
                showMicText(text);
            }
        }

        @Override public void onConnected() {

        }

        @Override public void onError(Exception e) {
            showError(e);
            enableMicButton();
        }

        @Override public void onDisconnected() {
            enableMicButton();
        }

        @Override
        public void onInactivityTimeout(RuntimeException runtimeException) {

        }

        @Override
        public void onListening() {

        }

        @Override
        public void onTranscriptionComplete() {

        }
    }

    private void showMicText(final String text) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                inputMessage.setText(text);
            }
        });
    }

    private void enableMicButton() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                btnRecord.setEnabled(true);
            }
        });
    }

    private void showError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(watchat.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

}
