package com.faendir.rhinotest;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.faendir.rhino_android.RhinoAndroidHelper;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;

public class MainActivity extends Activity {

    private Context context;
    private Scriptable scope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        context = RhinoAndroidHelper.prepareContext();
        context.setOptimizationLevel(1);
        scope = new ImporterTopLevel(context);
    }

    private void toastScript( String script) {
        try {
            Object result = context.evaluateString(scope, script, "<hello_world>", 1, null);
            Toast.makeText(this, Context.toString(result), Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void button(View v){
        toastScript(((EditText)findViewById(R.id.editText)).getText().toString());
    }

}
