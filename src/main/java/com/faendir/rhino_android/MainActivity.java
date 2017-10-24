package com.faendir.rhino_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.faendir.rhinotest.R;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
                new OptimizationComparisonTask().execute();
                break;
        }
    }

    private class OptimizationComparisonTask extends AsyncTask<Void, Void, List<Pair<Long, Long>>> {
        private final TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        private final TableRow.LayoutParams rowParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        private ProgressDialog progressDialog;

        @Override
        protected List<Pair<Long, Long>> doInBackground(Void... params) {
            Context context = rhinoAndroidHelper.enterContext();
            Scriptable scope = new ImporterTopLevel(context);
            List<Pair<Long, Long>> times = new ArrayList<>();
            for (int i = -1; i <= 1; i++) {
                context.setOptimizationLevel(i);
                long start = System.currentTimeMillis();
                Script script = context.compileString("var Pi=0;\n" +
                        "var n=1;\n" +
                        "for (i=0;i<100000;i++)\n" +
                        "{\n" +
                        "Pi=Pi+(4/n)-(4/(n+2))\n" +
                        "n=n+4\n" +
                        "}", "compute_pi", 1, null);
                long compilation = System.currentTimeMillis() - start;
                start = System.currentTimeMillis();
                script.exec(context, scope);
                long execution = System.currentTimeMillis() - start;
                times.add(Pair.create(compilation, execution));
            }
            return times;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(List<Pair<Long, Long>> times) {
            progressDialog.dismiss();
            TableLayout tableLayout = new TableLayout(MainActivity.this);
            tableLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            tableLayout.addView(getRowWithTexts("Optimization", "Compilation", "Execution", "Sum"));
            Pair<Long, Long> base = times.get(0);
            if(base.first == 0) base = Pair.create(1L, base.second);
            if(base.second == 0) base = Pair.create(base.first, 1L);
            long baseSum = base.first + base.second;
            for (int i = 0; i < times.size(); i++) {
                Pair<Long, Long> value = times.get(i);
                long sum = value.first + value.second;
                tableLayout.addView(getRowWithTexts(String.valueOf(i - 1),
                        value.first + "ms (" + (value.first * 100 / base.first) + "%) ",
                        value.second + "ms (" + (value.second * 100 / base.second) + "%) ",
                        sum + "ms (" + (sum * 100 / baseSum) + "%) "));
            }
            new AlertDialog.Builder(MainActivity.this)
                    .setView(tableLayout)
                    .setPositiveButton("Close", null)
                    .show();
        }

        private TextView getTextViewForText(String text) {
            TextView textView = new TextView(MainActivity.this);
            textView.setLayoutParams(rowParams);
            textView.setText(text);
            textView.setGravity(Gravity.CENTER);
            return textView;
        }

        private TableRow getRowWithTexts(String... texts) {
            TableRow tableRow = new TableRow(MainActivity.this);
            tableRow.setLayoutParams(tableParams);
            for (String text : texts) {
                tableRow.addView(getTextViewForText(text));
            }
            return tableRow;
        }
    }
}
