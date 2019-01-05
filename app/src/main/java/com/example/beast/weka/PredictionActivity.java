package com.example.beast.weka;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.beast.chatbot.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class PredictionActivity extends AppCompatActivity {

    private static final String WEKA_TEST = "WekaTest";

    private int correct = 0;

    TextView mTextViewSamples = null;
    Button buttonPredict;
    Button buttonLoad;

    private Classifier mClassifier = null;
    Attribute attributeS1, attributeS2, attributeS3, attributeS4, attributeS5;

    private Random mRandom = new Random();
    ArrayList<String> arraylist = new ArrayList<>();
    WekaData[] mSamples;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextViewSamples = (TextView) findViewById(R.id.predictionText);
        buttonPredict = (Button) findViewById(R.id.buttonPredict);
        buttonLoad = (Button) findViewById((R.id.buttonLoad));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        arraylist = getIntent().getStringArrayListExtra("Symptoms");

        String[] myArray = new String[arraylist.size()];
        myArray = arraylist.toArray(myArray);
        mSamples = new WekaData[]{
                new WekaData(1, myArray)
        };

        StringBuilder sb = new StringBuilder("Weka Data:\n");
        for (WekaData s : mSamples) {
            sb.append(s + "\n");
        }
        mTextViewSamples.setText(sb.toString());

        buttonLoad.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){
                        onClickButtonLoadModel(buttonLoad);
                    }
                }
        );
        buttonPredict.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){
                        onButtonClickPredict(buttonPredict);
                    }
                }
        );

        Log.d(WEKA_TEST, "onCreate() finished.");
    }

    public void onClickButtonLoadModel(View _v) {
        Log.d(WEKA_TEST, "onClickButtonLoadModel()");

        AssetManager assetManager = getAssets();
        try {
            mClassifier = (Classifier) weka.core.SerializationHelper.read(assetManager.open("disease.model"));

        } catch (Exception e) {
            // Weka "catch'em all!"
            e.printStackTrace();
        }

        Toast.makeText(this, "Model loaded.", Toast.LENGTH_SHORT).show();
    }

    public void onButtonClickPredict(View _v) {
        Log.d(WEKA_TEST, "onClickButtonPredict()");

        if (mClassifier == null) {
            Toast.makeText(this, "Model not loaded!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create list to hold nominal values
        final List<String> myNominalValues = new ArrayList<String>() {
            {
                add("watery diarrhea");
                add("abdominal cramps");
                add("nausea");
                add("vomiting");
                add("low grade fever");
                add("cough");
                add("wheezing");
                add("sneezing");
                add("running nose");
                add("scratchy throat");
                add("nasal congestion");
                add("fever");
                add("chest pain");
                add("shortness of breath");
                add("palpitation");
                add("sweating");
                add("dizziness");
                add("headache");
                add("fatigue");
                add("vision problem");
                add("neck pain");

            }

        };
        final List<String> classes = new ArrayList<String>() {
            {
                add("gastroenteritis"); // cls nr 1
                add("Influenza"); // cls nr 2
                add("urti"); // cls nr 3
                add("coronary artery disease"); //cls nr 4
                add("high blood pressure (hypertension"); //cls nr 5
            }
        };
        Log.d(WEKA_TEST, "list of classes done");

        try {
            ArrayList<Attribute> attributeList = new ArrayList<Attribute>(2) {
                {
                    attributeS1 = new Attribute("s_1", myNominalValues);
                    add(attributeS1);
                    attributeS2 = new Attribute("s_2", myNominalValues);
                    add(attributeS2);
                    attributeS3 = new Attribute("s_3", myNominalValues);
                    add(attributeS3);
                    attributeS4 = new Attribute("s_4", myNominalValues);
                    add(attributeS4);
                    attributeS5 = new Attribute("s_5", myNominalValues);
                    add(attributeS5);
                    Attribute attributeClass = new Attribute("@@class@@", classes);
                    add(attributeClass);
                }
            };
            Log.d(WEKA_TEST, "attribute arraylist done");
            // unpredicted data sets (reference to sample structure for new instances)
            Instances dataUnpredicted = new Instances("TestInstances", attributeList, 1);
            Log.d(WEKA_TEST, "data unpredicted instance");

            // last feature is target variable
            dataUnpredicted.setClassIndex(dataUnpredicted.numAttributes() - 1);
            Log.d(WEKA_TEST, "set class index for data unpredicted");

            // create new instance
            final WekaData s = mSamples[mRandom.nextInt(mSamples.length)];

            //final WekaData s = array[mRandom.nextInt(array.length)];
            DenseInstance newInstance = new DenseInstance(dataUnpredicted.numAttributes()) {
                {
                    setValue(attributeS1, s.wekaData[0]);
                    setValue(attributeS2, s.wekaData[1]);
                    setValue(attributeS3, s.wekaData[2]);
                    setValue(attributeS4, s.wekaData[3]);
                    setValue(attributeS5, s.wekaData[4]);

                }
            };
            Log.d(WEKA_TEST, "set value of attributes done");
            // reference to dataset
            newInstance.setDataset(dataUnpredicted);
            // predict new sample
            try {
                Log.d(WEKA_TEST, "going to classify");

                double result = mClassifier.classifyInstance(newInstance);
                String className = classes.get(Double.valueOf(result).intValue());
                String msg = "predicted: " + className;
                String actual = classes.get(s.label);
//                if(className == actual){
//                    correct++;
//                }
//                double accuracy = 100 * correct / dataUnpredicted.size(); //doesnot work

                Log.d(WEKA_TEST, msg);
                mTextViewSamples.setText("predicted: " + msg + "\n actual: " + actual );
                //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }

    }
}
