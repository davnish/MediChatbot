package com.example.beast.chatbot;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.beast.weka.PredictionActivity;
import com.example.beast.weka.WekaData;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.android.GsonFactory;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class DialogActivity extends AppCompatActivity implements AIListener{

    public static final String TAG = DialogActivity.class.getName();

    RecyclerView recyclerView;
    EditText editText;
    RelativeLayout addBtn;
    DatabaseReference ref;
    FirebaseRecyclerAdapter<ChatMessage, chat_rec> adapter;
    Boolean flagFab = true;

    private Gson gson = GsonFactory.getGson();

    private AIService aiService;

    TextToSpeech tts;

    private static final Pattern END_OF_SENTENCE = Pattern.compile("\\.\\s+");

    String[] symptomsArray = new String[]{"fever", "vomiting", "nausea", "sweating", "dizziness"};

    public static final String My_SYMPTOMS = "Symptoms";

    ArrayList<String> mySymptoms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);


        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        editText = (EditText) findViewById(R.id.editText);
        addBtn = (RelativeLayout) findViewById(R.id.addBtn);

        tts=new TextToSpeech(DialogActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if (status == TextToSpeech.SUCCESS) {
                    // Setting speech language
                    int result = tts.setLanguage(Locale.US);
                    // If your device doesn't support language you set above
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Cook simple toast message with message
                        Toast.makeText(getApplicationContext(), "Language not supported",
                                Toast.LENGTH_LONG).show();
                        Log.e("TTS", "Language is not supported");
                    }
                    // Enable the button - It was disabled in main.xml (Go back and
                    // Check it)
                    else {
                        Log.e("TTS", "Language is supported");

                    }
                    // TTS is not initialized properly
                } else {
                    Toast.makeText(getApplicationContext(), "TTS Initilization Failed", Toast.LENGTH_LONG).show();
                    Log.e("TTS", "Initilization Failed");
                }
            }

        });

        recyclerView.setHasFixedSize(true);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        ref = FirebaseDatabase.getInstance().getReference();
        ref.keepSynced(true);

        final AIConfiguration config = new AIConfiguration("77d599a4ce584aa6aa89c9e81ad0dd65",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        final AIDataService aiDataService = new AIDataService(config);

        final AIRequest aiRequest = new AIRequest();


        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String message = editText.getText().toString().trim();

                if (!message.equals("")) {

                    ChatMessage chatMessage = new ChatMessage(message, "user");
                    ref.child("chat").push().setValue(chatMessage);

                    aiRequest.setQuery(message);
                    new AsyncTask<AIRequest, Void, AIResponse>() {

                        @Override
                        protected AIResponse doInBackground(AIRequest... aiRequests) {
                            final AIRequest request = aiRequests[0];
                            try {
                                final AIResponse response = aiDataService.request(aiRequest);
                                return response;
                            } catch (AIServiceException e) {
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(AIResponse response) {
                            if (response != null) {

                                Result result = response.getResult();
                                String reply = result.getFulfillment().getSpeech();
                                ChatMessage chatMessage = new ChatMessage(reply, "bot");
                                ref.child("chat").push().setValue(chatMessage);
                                ConvertTextToSpeech(reply);

                                splitAndSearch(result.getResolvedQuery(), symptomsArray);
                                if(reply.toLowerCase().equals("bye")){
                                    Intent intent = new Intent(getBaseContext(), PredictionActivity.class);
                                    intent.putStringArrayListExtra("Symptoms", mySymptoms);
                                    startActivity(intent);
                                }
                            }
                        }
                    }.execute(aiRequest);
                } else {
                    aiService.startListening();
                }

                editText.setText("");
            }
        });


        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ImageView fab_img = (ImageView) findViewById(R.id.fab_img);
                Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.ic_send_white_24dp);
                Bitmap img1 = BitmapFactory.decodeResource(getResources(), R.drawable.ic_mic_white_24dp);


                if (s.toString().trim().length() != 0 && flagFab) {
                    ImageViewAnimatedChange(DialogActivity.this, fab_img, img);
                    flagFab = false;

                } else if (s.toString().trim().length() == 0) {
                    ImageViewAnimatedChange(DialogActivity.this, fab_img, img1);
                    flagFab = true;

                }


            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        adapter = new FirebaseRecyclerAdapter<ChatMessage, chat_rec>(ChatMessage.class, R.layout.msglist, chat_rec.class, ref.child("chat")) {
            @Override
            protected void populateViewHolder(chat_rec viewHolder, ChatMessage model, int position) {

                if (model.getMsgUser().equals("user")) {


                    viewHolder.rightText.setText(model.getMsgText());

                    viewHolder.rightText.setVisibility(View.VISIBLE);
                    viewHolder.leftText.setVisibility(View.GONE);
                } else {
                    viewHolder.leftText.setText(model.getMsgText());

                    viewHolder.rightText.setVisibility(View.GONE);
                    viewHolder.leftText.setVisibility(View.VISIBLE);
                }
            }
        };

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                int msgCount = adapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisiblePosition == -1 ||
                        (positionStart >= (msgCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    recyclerView.scrollToPosition(positionStart);

                }

            }
        });

        recyclerView.setAdapter(adapter);


    }

    public void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, R.anim.zoom_out);
        final Animation anim_in = AnimationUtils.loadAnimation(c, R.anim.zoom_in);
        anim_out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setImageBitmap(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }

    @Override
    public void onResult(final AIResponse response) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Variables
                Log.d(TAG, "onResult");

                gson.toJson(response);

                final Status status = response.getStatus();
                Log.i(TAG, "Status code: " + status.getCode());
                Log.i(TAG, "Status type: " + status.getErrorType());

                final Result result = response.getResult();

                Log.i(TAG, "Result: " + result.toString());
                Log.i(TAG, "Received success response");
                Log.i(TAG, "Resolved query: " + result.getResolvedQuery()); //sent message

                Log.i(TAG, "Action: " + result.getAction());
                final String speech = result.getFulfillment().getSpeech(); //fulfillment message from bot
                Log.i(TAG, "Speech: " + speech);

                String message = result.getResolvedQuery();
                ChatMessage userMessage = new ChatMessage(message, "user");
                ref.child("chat").push().setValue(userMessage);

                ChatMessage botMessage = new ChatMessage(speech, "bot");
                ref.child("chat").push().setValue(botMessage);
                ConvertTextToSpeech(speech);

                final Metadata metadata = result.getMetadata();
                if (metadata != null) {
                    Log.i(TAG, "Intent id: " + metadata.getIntentId());
                    Log.i(TAG, "Intent name: " + metadata.getIntentName()); //intent name as specified in dialogflow
                }

                final HashMap<String, JsonElement> params = result.getParameters();
                if (params != null && !params.isEmpty()) {
                    Log.i(TAG, "Parameters: ");
                    for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                        Log.i(TAG, String.format("%s: %s",
                                entry.getKey(), entry.getValue().toString()));
                    }
                }

                splitAndSearch(result.getResolvedQuery(), symptomsArray);

                if(speech.toLowerCase().equals("bye")){
                    Intent intent = new Intent(getBaseContext(), PredictionActivity.class);
                    //intent.putExtra("Symptoms", symptomsArray);
                    intent.putStringArrayListExtra("Symptoms", mySymptoms);
//                    Bundle b = new Bundle();
//                    b.putStringArray("Symptoms", symptomsArray);
//                    intent.putExtra("BUNDLE", b);
                    startActivity(intent);
                }

            }

        });
    }

    @Override
    public void onError(final ai.api.model.AIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, error.toString());
            }
        });
    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }

    private void ConvertTextToSpeech(final String text){
        //String text = editText.getText().toString();
        if("".equals(text)){
            String message = "Content not available";
            tts.speak(message,TextToSpeech.QUEUE_FLUSH, null);
        }
        else{
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);

        }
    }

    @Override
    public void onDestroy() {
        // Don't forget to shutdown!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    /*
    StringTokenizer is a legacy class that is retained for compatibility reasons although its use is discouraged in new code.
    It is recommended that anyone seeking this functionality use the split method of String or the java.util.regex package instead.
     */
    public void splitAndSearch(String sentString, String[] symp){

        /* final String lcword = symp.toLowerCase(); */
        for(String sentence : END_OF_SENTENCE.split(sentString)){
            for (String aSymp : symp) {
                if (sentence.toLowerCase().contains(aSymp.toLowerCase())) {
                    Log.i("Contains symptom: ", sentence);
                    mySymptoms.add(aSymp);
                    String[] tokens = sentence.split("\\s");
                    for (String token : tokens) {
                        Log.i("Tokens:" + "\n", token);
                    }
                }
            }
        }

        WekaData myData = new WekaData(1, mySymptoms.toArray(new String[mySymptoms.size()]));
        Log.i("Weka data", mySymptoms.toString());
    }
}

