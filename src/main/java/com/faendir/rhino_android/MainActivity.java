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
            case R.id.button3:
                new OptimizationComparisonTask(this, rhinoAndroidHelper).execute(-1, 0, 1);
                break;
        }
    }

}
