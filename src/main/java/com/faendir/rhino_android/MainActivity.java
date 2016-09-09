package com.faendir.rhino_android;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.faendir.rhinotest.R;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {

    private Context context;
    private Scriptable scope;
    private RhinoAndroidHelper rhinoAndroidHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        rhinoAndroidHelper = new RhinoAndroidHelper(this);
        context = rhinoAndroidHelper.enterContext();
        context.setOptimizationLevel(1);
        scope = new ImporterTopLevel(context);
    }

    private void toastScript(String script) {
        try {
            Object result = context.evaluateString(scope, script, "<hello_world>", 1, null);
            Toast.makeText(this, Context.toString(result), Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void button(View v) {
        switch (v.getId()) {
            case R.id.button:
                toastScript(((EditText) findViewById(R.id.editText)).getText().toString());
                break;
            case R.id.button2:
                try {
                    rhinoAndroidHelper.loadClassJar(copyAsset("HelloWorld.jar"));
                    rhinoAndroidHelper.loadClassJar(copyAsset("HelloWorld2.jar"));
                    context.evaluateString(scope, "com.faendir.helloworld.Main.main(null);", "<hello_world>", 1, null);
                    context.evaluateString(scope, "com.faendir.helloworld.Main2.main(null);", "<hello_world>", 1, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    private File copyAsset(String name){
        FileOutputStream out = null;
        InputStream in = null;
        File file = new File(getFilesDir(), name);
        try {
            out = new FileOutputStream(file);
            in = getAssets().open(name);
            byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data)) != -1) {
                out.write(data, 0, count);
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (out != null) {
                    out.close();
                }
                if(in != null){
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

}
