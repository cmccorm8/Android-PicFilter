package mccormick.cmccorm8.picfilter;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    PicFrag picFrag;
    String TAG ="MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(savedInstanceState == null)
        {
            picFrag = new PicFrag();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, picFrag).commit();
        }
    }
}