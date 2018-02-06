package com.faendir.rhino_android;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.faendir.rhinotest.R;

import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * @author F43nd1r
 * @since 06.02.18
 */
class OptimizationComparisonTask extends AsyncTask<Integer, Void, List<OptimizationComparisonTask.Result>> {
    private final TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
    private final TableRow.LayoutParams rowParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
    private final ProgressDialog progressDialog;
    private final RhinoAndroidHelper rhinoAndroidHelper;
    private final WeakReference<Context> context;
    private final String code;

    OptimizationComparisonTask(Context context, RhinoAndroidHelper rhinoAndroidHelper) {
        this.rhinoAndroidHelper = rhinoAndroidHelper;
        this.context = new WeakReference<>(context);
        this.progressDialog = new ProgressDialog(context);
        StringBuilder builder = new StringBuilder();
        try {
            InputStream codeStream = context.getResources().openRawResource(R.raw.compute_pi);
            BufferedReader codeReader = new BufferedReader(new InputStreamReader(codeStream));
            builder.ensureCapacity(codeStream.available());
            String line;
            while ((line = codeReader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        } catch (IOException ignored) {
        }
        this.code = builder.toString();
    }

    @Override
    protected List<Result> doInBackground(Integer... params) {
        org.mozilla.javascript.Context context = rhinoAndroidHelper.enterContext();
        Scriptable scope = new ImporterTopLevel(context);
        //warm up
        for (int i : params) {
            context.setOptimizationLevel(i);
            for (int j = 0; j < 10; j++) {
                context.compileString(code, "compute_pi", 1, null);
            }
        }
        List<Result> times = new ArrayList<>();
        for (int i : params) {
            context.setOptimizationLevel(i);
            long start = System.nanoTime();
            Script script = context.compileString(code, "compute_pi", 1, null);
            long compilation = System.nanoTime() - start;
            long execution = (long) LongStream.generate(() -> {
                long s = System.nanoTime();
                script.exec(context, scope);
                return System.nanoTime() - s;
            }).skip(5).limit(25).average().orElse(1);
            times.add(new Result(i, compilation / 1000, execution / 1000));
        }
        org.mozilla.javascript.Context.exit();
        return times;
    }

    @Override
    protected void onPreExecute() {
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    protected void onPostExecute(List<Result> times) {
        progressDialog.dismiss();
        Context context = this.context.get();
        if (context != null) {
            TableLayout tableLayout = new TableLayout(context);
            tableLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            tableLayout.setDividerDrawable(new ColorDrawable(Color.GRAY));
            tableLayout.setShowDividers(TableLayout.SHOW_DIVIDER_MIDDLE);
            long baseCompilationTime = times.stream().mapToLong(Result::getCompilationTime).max().orElse(1);
            long baseExecutionTime = times.stream().mapToLong(Result::getExecutionTime).max().orElse(1);
            long baseSum = times.stream().mapToLong(Result::getSum).max().orElse(1);
            List<Result> percentages = times.stream().map(result -> result.asPercentageOf(baseCompilationTime, baseExecutionTime, baseSum)).collect(Collectors.toList());
            tableLayout.addView(getRowWithTexts(context, "Optimization", times.stream().mapToLong(Result::getOptimizationLevel).toArray()));
            tableLayout.addView(getRowWithTexts(context, "Compilation (μs)", times.stream().mapToLong(Result::getCompilationTime).toArray()));
            tableLayout.addView(getRowWithTexts(context, "Compilation (%)", percentages.stream().mapToLong(Result::getCompilationTime).toArray()));
            tableLayout.addView(getRowWithTexts(context, "Execution (μs)", times.stream().mapToLong(Result::getExecutionTime).toArray()));
            tableLayout.addView(getRowWithTexts(context, "Execution (%)", percentages.stream().mapToLong(Result::getExecutionTime).toArray()));
            tableLayout.addView(getRowWithTexts(context, "Sum (μs)", times.stream().mapToLong(Result::getSum).toArray()));
            tableLayout.addView(getRowWithTexts(context, "Sum (%)", percentages.stream().mapToLong(Result::getSum).toArray()));
            HorizontalScrollView scrollView = new HorizontalScrollView(context);
            scrollView.addView(tableLayout);
            new AlertDialog.Builder(context).setView(scrollView).setPositiveButton("Close", null).show();
        }
    }

    private TextView getTextViewForText(Context context, String text) {
        TextView textView = new TextView(context);
        textView.setLayoutParams(rowParams);
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(5, 0, 5, 0);
        return textView;
    }

    private TableRow getRowWithTexts(Context context, String title, long... values) {
        TableRow tableRow = new TableRow(context);
        tableRow.setLayoutParams(tableParams);
        tableRow.setDividerDrawable(new ColorDrawable(Color.GRAY));
        tableRow.setShowDividers(TableLayout.SHOW_DIVIDER_MIDDLE);
        TextView titleView = getTextViewForText(context, title);
        titleView.setTypeface(null, Typeface.BOLD);
        tableRow.addView(titleView);
        for (long value : values) {
            tableRow.addView(getTextViewForText(context, String.valueOf(value)));
        }
        return tableRow;
    }

    static class Result {
        private final int optimizationLevel;
        private final long compilationTime;
        private final long executionTime;
        private final long sum;

        Result(int optimizationLevel, long compilationTime, long executionTime) {
            this(optimizationLevel, compilationTime, executionTime, compilationTime + executionTime);
        }

        private Result(int optimizationLevel, long compilationTime, long executionTime, long sum) {
            this.optimizationLevel = optimizationLevel;
            this.compilationTime = compilationTime;
            this.executionTime = executionTime;
            this.sum = sum;
        }

        int getOptimizationLevel() {
            return optimizationLevel;
        }

        long getCompilationTime() {
            return compilationTime;
        }

        long getExecutionTime() {
            return executionTime;
        }

        long getSum() {
            return sum;
        }

        Result asPercentageOf(long compilationTime, long executionTime, long sum) {
            return new Result(optimizationLevel, this.compilationTime * 100 / compilationTime, this.executionTime * 100 / executionTime, this.sum * 100 / sum);
        }
    }
}
