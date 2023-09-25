package com.app.sudoku;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final int EASY = 0;
    private static final int MEDIUM = 1;
    private static final int HARD = 2;
    private static final int EXPERT = 3;

    private int difficultyLevel = EASY;
    private SudokuAdapter adapter;
    private int[][] userSolution = new int[9][9];
    private int[][] correctSolution = new int[9][9];
    private int answerValue = 0;

    private TextView focusedTextView = null;
    private int focusRow = 0;
    private int focusCol = 0;
    private int hintCount = 1;
    private Chronometer chronometer;
    private int wrongAnswerCount = 0;
    private TextView wrongAnswerMsg;
    private Snackbar snackbar;
    private String snackbarLastMsg = "";
    private AdView mAdView;
    private RewardedAd rewardedAd;
    private InterstitialAd mInterstitialAd;
    private final String TAG = "MainActivity";
    private SharedPreferences gameinfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //廣告
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        initInterstitialAd();
        InitRewardedAd();

        //
        gameinfo = getSharedPreferences("gameinfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = gameinfo.edit();
        int playTimes = gameinfo.getInt("playTimes",0);
        editor.putInt("playTimes", playTimes + 1 );
        editor.commit();



        // 獲取難度
        difficultyLevel = getIntent().getIntExtra("difficultyLevel", 0);

        GridView gameBoard = findViewById(R.id.gameBoard);


        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat(getResources().getString(R.string.GameTime) + ": %s");
        chronometer.start(); // 開始計時


        wrongAnswerMsg = findViewById(R.id.wrongAnswerMsg);
        wrongAnswerMsg.setText(getResources().getString(R.string.mistake) + wrongAnswerCount + " / 3");

        SudokuGenerator generator = new SudokuGenerator();
        int[][] sudokuBoard = generator.getSudokuBoard();
        adapter = new SudokuAdapter(this, userSolution);
        gameBoard.setAdapter(adapter);

        ImageView btn_hint = findViewById(R.id.btn_hint);
        ImageView btn_back= findViewById(R.id.btn_back);
        btn_hint.setOnClickListener(v -> {
            generateHint();
        });
        btn_back.setOnClickListener(v -> {
            showExitGameDialog();
        });

        gameBoard.setOnItemClickListener((parent, view, position, id) -> {
            int row = position / 9;
            int col = position % 9;

            // 判斷是否有焦點，回復原本樣式
            if (focusedTextView != null) {
                int gridStyle =  adapter.calculateGridColor(focusRow, focusCol);
                focusedTextView.setBackgroundResource(gridStyle);
            }


            focusedTextView = (TextView) view;

            if (!focusedTextView.getText().toString().equals("")) {
                focusedTextView = null;
                return;
            }

            focusRow = row;
            focusCol = col;
            focusedTextView.setBackgroundResource(R.drawable.textview_focused_background);
        });

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });


    }

    private void InitRewardedAd()
    {



        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-8275397647362849/9617706466",
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.
                        Log.d(TAG, loadAdError.toString());
                        rewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d(TAG, "Ad was loaded.");

                        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdClicked() {
                                // Called when a click is recorded for an ad.
                                Log.d(TAG, "Ad was clicked.");
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                // Set the ad reference to null so you don't show the ad a second time.
                                Log.d(TAG, "Ad dismissed fullscreen content.");
                                rewardedAd = null;
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when ad fails to show.
                                Log.e(TAG, "Ad failed to show fullscreen content.");
                                rewardedAd = null;
                            }

                            @Override
                            public void onAdImpression() {
                                // Called when an impression is recorded for an ad.
                                Log.d(TAG, "Ad recorded an impression.");
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when ad is shown.
                                Log.d(TAG, "Ad showed fullscreen content.");
                            }
                        });
                }
        });


    }

    private void initInterstitialAd()
    {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this,"ca-app-pub-8275397647362849/9617706466", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");

                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                            @Override
                            public void onAdClicked() {
                                // Called when a click is recorded for an ad.
                                Log.d(TAG, "Ad was clicked.");
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                // Set the ad reference to null so you don't show the ad a second time.
                                Log.d(TAG, "Ad dismissed fullscreen content.");
                                mInterstitialAd = null;
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when ad fails to show.
                                Log.e(TAG, "Ad failed to show fullscreen content.");
                                mInterstitialAd = null;
                            }

                            @Override
                            public void onAdImpression() {
                                // Called when an impression is recorded for an ad.
                                Log.d(TAG, "Ad recorded an impression.");
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when ad is shown.
                                Log.d(TAG, "Ad showed fullscreen content.");
                            }
                        });

                        int playTimes = gameinfo.getInt("playTimes",0);
                        if(playTimes % 3 == 0)
                        {
                            if (mInterstitialAd != null) {
                                mInterstitialAd.show(MainActivity.this);
                            } else {
                                Log.d("TAG", "The interstitial ad wasn't ready yet.");
                            }
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, loadAdError.toString());
                        mInterstitialAd = null;


                        //LoadAdsErrorDialog();
                    }
                });


    }

    @Override
    public void onBackPressed() {

        showExitGameDialog();

    }

    private void LoadAdsErrorDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.Msg);
        builder.setMessage(R.string.ErrorAdsMsg);
        builder.setPositiveButton(R.string.Exit, (dialog, which) -> {
            Intent intent = new Intent(MainActivity.this, StartActivity.class);
            startActivity(intent);
            finish();

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showExitGameDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.Msg);
        builder.setMessage(R.string.AskExitGame);
        builder.setPositiveButton(R.string.Exit, (dialog, which) -> {
            Intent intent = new Intent(MainActivity.this, StartActivity.class);
            startActivity(intent);
            finish();

        });
        builder.setNegativeButton(R.string.Continue, (dialog, which) -> {

            dialog.cancel();

        });


        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void onAnswerClick(View view) {

        TextView textView = (TextView) view;
        String buttonText = textView.getText().toString();



        if (focusedTextView == null) {

            ShowSnackbar(getString(R.string.InputMsg),Snackbar.LENGTH_SHORT);
            return;
        }
        answerValue = Integer.parseInt(buttonText);

        if (answerValue != 0) {
            int selectedNumber = answerValue;
            // 驗證數字輸入
            if (isValidInput(focusRow, focusCol, selectedNumber)
                    && selectedNumber == correctSolution[focusRow][focusCol]) {
                userSolution[focusRow][focusCol] = selectedNumber;
                // 修改有焦點的
                adapter.notifyDataSetChanged(); // 通知刷新畫面
                SudokuSolver solver = new SudokuSolver(); //求解用class
                boolean isWin =  solver.isSudokuSolutionValid(userSolution);
                if(isWin)showWinGameDialog();

            } else {
                wrongAnswerCount++;

                if(wrongAnswerCount < 3)
                {
                    ShowSnackbar(getString(R.string.WrongAnswer),Snackbar.LENGTH_SHORT);
                    wrongAnswerMsg.setText(getResources().getString(R.string.mistake) + wrongAnswerCount + " / 3");
                }
                else {
                    chronometer.stop();
                    showLostGameDialog();
                }
            }
        }

    }

    private void showWinGameDialog() {

        chronometer.stop();
        long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
        int elapsedSeconds = (int) (elapsedMillis / 1000);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.Msg);
        builder.setMessage(getString(R.string.win_split1)+elapsedSeconds+ getString(R.string.win_split2));
        builder.setPositiveButton(R.string.Share, (dialog, which) -> {
            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT,  getString(R.string.ShareAppName));
                String shareMessage= "";
                shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID +"\n\n";
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(shareIntent, "choose one"));
            } catch(Exception e) {
                //e.toString();
            }

        });
        builder.setNegativeButton(R.string.Exit, (dialog, which) -> {


            Intent intent = new Intent(MainActivity.this, StartActivity.class);
            startActivity(intent);
            finish();
        });


        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showLostGameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.Msg);
        builder.setMessage(R.string.lost);
        builder.setPositiveButton(R.string.WatchAdsContinue, (dialog, which) -> {
            //看廣告
            if (rewardedAd != null) {
                Activity activityContext = MainActivity.this;
                rewardedAd.show(activityContext, rewardItem -> {
                    // Handle the reward.
                    Log.d(TAG, "The user earned the reward.");
                    int rewardAmount = rewardItem.getAmount();
                    String rewardType = rewardItem.getType();

                    //計時開始，並增加一個提示
                    chronometer.start();

                    InitRewardedAd();

                });
            }

            wrongAnswerCount = 0;
            wrongAnswerMsg.setText(getResources().getString(R.string.mistake) + wrongAnswerCount + " / 3");

        });
        builder.setNegativeButton(R.string.Exit, (dialog, which) -> {

            Intent intent = new Intent(MainActivity.this, StartActivity.class);
            startActivity(intent);
            finish();
        });


        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showHintDialog() {
        chronometer.stop();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.Msg);
        builder.setMessage(R.string.NoHint);
        builder.setPositiveButton(R.string.WatchAds, (dialog, which) -> {
            //看廣告
            if (rewardedAd != null) {
                Activity activityContext = MainActivity.this;
                rewardedAd.show(activityContext, rewardItem -> {
                    // Handle the reward.
                    Log.d(TAG, "The user earned the reward.");
                    int rewardAmount = rewardItem.getAmount();
                    String rewardType = rewardItem.getType();

                    //並增加一個提示
                    hintCount++;

                    InitRewardedAd();
                });
            }
            chronometer.start();
            dialog.dismiss();
        });
        builder.setNegativeButton(R.string.Continue, (dialog, which) -> {
            chronometer.start();
            dialog.dismiss();
        });


        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public class SudokuGenerator {
        private static final int SIZE = 9;
        private int[][] board;

        public SudokuGenerator() {

            board = new int[SIZE][SIZE];
            generateFirstRow(); // 隨機生成第一排數字
            board = generateSudokuBoard(difficultyLevel);

            // 複製一份給使用者輸入使用
            for (int i = 0; i < SIZE; i++) {
                System.arraycopy(board[i], 0, userSolution[i], 0, SIZE);
            }
            removeNumbers(difficultyLevel);


        }

        private void generateFirstRow() {
            Random random = new Random();
            for (int i = 1; i <= SIZE; i++) {
                int num;
                do {
                    num = random.nextInt(SIZE) + 1;
                } while (!isValid(board, 0, i - 1, num));
                board[0][i - 1] = num;
            }
        }

        private boolean isValid(int[][] board, int row, int col, int num) {
            // 檢查行是否有相同數字
            for (int i = 0; i < SIZE; i++) {
                if (board[row][i] == num) {
                    return false;
                }
            }

            // 檢查列是否有相同數字
            for (int i = 0; i < SIZE; i++) {
                if (board[i][col] == num) {
                    return false;
                }
            }

            // 檢查3x3方格是否有相同數字
            int startRow = row - row % 3;
            int startCol = col - col % 3;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[startRow + i][startCol + j] == num) {
                        return false;
                    }
                }
            }

            return true;
        }

        private boolean solveSudoku(int row, int col) {
            if (row == SIZE) {
                row = 0;
                if (++col == SIZE) {
                    return true;
                }
            }
            if (board[row][col] != 0) {
                return solveSudoku(row + 1, col);
            }
            for (int val = 1; val <= SIZE; ++val) {
                if (isValid(board, row, col, val)) {
                    board[row][col] = val;
                    if (solveSudoku(row + 1, col)) {
                        return true;
                    }
                    board[row][col] = 0;
                }
            }
            return false;
        }


        private boolean fillSudokuBoard(int[][] board) {
            int SIZE = board.length;

            for (int row = 1; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (board[row][col] == 0) {
                        for (int num = 1; num <= SIZE; num++) {
                            if (isValidMove(board, row, col, num)) {
                                board[row][col] = num; // 寫入數字

                                if (fillSudokuBoard(board)) { // 填充下一個
                                    return true;
                                }

                                board[row][col] = 0; // 無法填入，則清除
                            }
                        }
                        return false; // 填入失敗
                    }
                }
            }
            return true; // 全部成功
        }

        private boolean isValidMove(int[][] board, int row, int col, int num) {
            // 檢查行、列、3x3方格
            return !usedInRow(board, row, num) && !usedInCol(board, col, num) && !usedInBox(board, row - row % 3, col - col % 3, num);
        }

        private boolean usedInRow(int[][] board, int row, int num) {
            for (int col = 0; col < board.length; col++) {
                if (board[row][col] == num) {
                    return true;
                }
            }
            return false;
        }

        private boolean usedInCol(int[][] board, int col, int num) {
            for (int row = 0; row < board.length; row++) {
                if (board[row][col] == num) {
                    return true;
                }
            }
            return false;
        }

        private boolean usedInBox(int[][] board, int boxStartRow, int boxStartCol, int num) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    if (board[row + boxStartRow][col + boxStartCol] == num) {
                        return true;
                    }
                }
            }
            return false;
        }


        private void removeNumbers(int difficultyLevel) {
            Random random = new Random();
            int minNumbersToRemove = 0;
            int maxNumbersToRemove = 0;


            if (difficultyLevel == EASY) {
                minNumbersToRemove = 20;
                maxNumbersToRemove = 30;
            } else if (difficultyLevel == MEDIUM) {
                minNumbersToRemove = 30;
                maxNumbersToRemove = 40;
            } else if (difficultyLevel == HARD) {
                minNumbersToRemove = 40;
                maxNumbersToRemove = 50;
            }else if (difficultyLevel == EXPERT) {
                minNumbersToRemove = 64;
                maxNumbersToRemove = 64;
            }


            int numbersToRemove = random.nextInt(maxNumbersToRemove - minNumbersToRemove + 1) + minNumbersToRemove;


            Set<Integer> removedIndices = new HashSet<>();

            while (removedIndices.size() < numbersToRemove) {
                int row = random.nextInt(SIZE);
                int col = random.nextInt(SIZE);

                // 确保不删除同一位置的数字
                int index = row * SIZE + col;
                if (!removedIndices.contains(index)) {
                    removedIndices.add(index);
                    correctSolution[row][col] = userSolution[row][col];
                    userSolution[row][col] = 0;
                }
            }


        }

        public int[][] getSudokuBoard() {
            return board;
        }

        private int[][] generateSudokuBoard(int difficultyLevel) {
            int[][] sudokuBoard = new int[SIZE][SIZE];

            SudokuSolver solver = new SudokuSolver();

            while (true) {
                // 填滿數字
                fillSudokuBoard(sudokuBoard);

                // 隨機刪除數字
                removeNumbers(difficultyLevel);

                // 檢查是否唯一解答
                if (solver.hasUniqueSolution(sudokuBoard)) {
                    return sudokuBoard;
                }

            }
        }
    }

    public class SudokuSolver {
        private static final int SIZE = 9;
        public boolean hasUniqueSolution(int[][] sudoku) {
            List<int[][]> solutions = new ArrayList<>();
            solveSudoku(sudoku, solutions);
            return solutions.size() == 1;
        }

        private void solveSudoku(int[][] sudoku, List<int[][]> solutions) {
            int n = sudoku.length;
            int m = sudoku[0].length;
            int[] emptyCell = findEmptyCell(sudoku, n, m);

            if (emptyCell == null) {
                // 找到一个解
                int[][] solution = new int[n][m];
                for (int i = 0; i < n; i++) {
                    System.arraycopy(sudoku[i], 0, solution[i], 0, m);
                }
                solutions.add(solution);
                return;
            }

            int row = emptyCell[0];
            int col = emptyCell[1];

            for (int num = 1; num <= 9; num++) {
                if (isValidMove(sudoku, row, col, num)) {
                    sudoku[row][col] = num;
                    solveSudoku(sudoku, solutions);
                    sudoku[row][col] = 0; // 回溯
                }
            }
        }
        private int[] findEmptyCell(int[][] sudoku, int n, int m) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    if (sudoku[i][j] == 0) {
                        return new int[]{i, j};
                    }
                }
            }
            return null; // 所有单元格都已填充
        }

        public boolean solveSudoku(int[][] board) {
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (board[row][col] == 0) {
                        for (int num = 1; num <= SIZE; num++) {
                            if (isValidMove(board, row, col, num)) {
                                board[row][col] = num;
                                if (solveSudoku(board)) {
                                    return true;
                                }
                                board[row][col] = 0; // 回溯
                            }
                        }
                        return false; // 如果无法填入任何数字，返回false
                    }
                }
            }
            return true; // 所有单元格都已填充，返回true
        }

        public boolean isSudokuSolutionValid(int[][] userSolution) {
            // 檢查是否填滿
            for (int row = 0; row < 9; row++) {
                for (int col = 0; col < 9; col++) {
                    if (userSolution[row][col] == 0) {
                        // 發現尚未填寫
                        return false;
                    }
                }
            }

            // 检查行
            for (int row = 0; row < 9; row++) {
                boolean[] used = new boolean[10];
                for (int col = 0; col < 9; col++) {
                    int num = userSolution[row][col];
                    if (used[num]) {
                        // 在行中发现重复数字
                        return false;
                    }
                    used[num] = true;
                }
            }

            // 检查列
            for (int col = 0; col < 9; col++) {
                boolean[] used = new boolean[10];
                for (int row = 0; row < 9; row++) {
                    int num = userSolution[row][col];
                    if (used[num]) {
                        // 在列中发现重复数字
                        return false;
                    }
                    used[num] = true;
                }
            }

            // 檢查九宮格
            for (int boxRow = 0; boxRow < 3; boxRow++) {
                for (int boxCol = 0; boxCol < 3; boxCol++) {
                    boolean[] used = new boolean[10];
                    for (int row = 3 * boxRow; row < 3 * boxRow + 3; row++) {
                        for (int col = 3 * boxCol; col < 3 * boxCol + 3; col++) {
                            int num = userSolution[row][col];
                            if (used[num]) {
                                // 發現重複數字
                                return false;
                            }
                            used[num] = true;
                        }
                    }
                }
            }

            // 通過以上全部，答案正確
            return true;
        }

        private boolean isValidMove(int[][] board, int row, int col, int num) {
            return !usedInRow(board, row, num) && !usedInCol(board, col, num) && !usedInBox(board, row - row % 3, col - col % 3, num);
        }

        private boolean usedInRow(int[][] board, int row, int num) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col] == num) {
                    return true;
                }
            }
            return false;
        }

        private boolean usedInCol(int[][] board, int col, int num) {
            for (int row = 0; row < SIZE; row++) {
                if (board[row][col] == num) {
                    return true;
                }
            }
            return false;
        }

        private boolean usedInBox(int[][] board, int boxStartRow, int boxStartCol, int num) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    if (board[row + boxStartRow][col + boxStartCol] == num) {
                        return true;
                    }
                }
            }
            return false;
        }
    }


    public class SudokuAdapter extends BaseAdapter {
        private Context context;
        private int[][] data; // 棋盤數據
        private int filledNumberColor = Color.parseColor("#008000");
        ; // 已填数字的颜色

        public SudokuAdapter(Context context, int[][] data) {
            this.context = context;
            this.data = data;
        }

        @Override
        public int getCount() {
            return 81; // 9x9
        }

        @Override
        public Object getItem(int position) {
            return data[position / 9][position % 9];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // 在这里创建并返回每个单元格的视图
            TextView textView = new TextView(context);
            textView.setLayoutParams(new GridView.LayoutParams(GridView.AUTO_FIT, GridView.AUTO_FIT)); // 调整布局参数以适应您的需求
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(24);


            int row = position / 9;
            int col = position % 9;
            int cellValue = data[row][col];

            int gridStyle = calculateGridColor(row, col);
            textView.setBackgroundResource(gridStyle);

            if (cellValue != 0) {
                textView.setText(String.valueOf(cellValue));
                // 答案正確設定為綠色
                if (userSolution[row][col] == correctSolution[row][col]) {
                    textView.setTextColor(filledNumberColor);
                } else {
                    textView.setTextColor(Color.BLACK);
                }


            } else {

                return textView;
            }
            return textView;

        }
        // 計算3x3背景顏色
        public int calculateGridColor(int row, int col) {
            int gridRow = row / 3;
            int gridCol = col / 3;


            // 根據3x3格子的位置返回對應的背景顏色
            int gridStyle =  (gridRow * 3 + gridCol) % 2 ;
            // 返回不同樣式 Drawable 資源
            switch (gridStyle) {
                case 1:
                    return R.drawable.textview_background1; // 藍色
                case 2:
                    return R.drawable.textview_background2; // 白色
                default:
                    return R.drawable.textview_background2;
            }

        }


    }
    // 验证用户输入的数字是否有效
    private boolean isValidInput(int row, int col, int num) {
        // 验证数字是否在1到9的范围内
        if (num < 1 || num > 9) {
            return false;
        }

        // 验证数字在当前行、列和3x3宫格内是否唯一
        return isValidInRow(row, num) && isValidInColumn(col, num) && isValidInBox(row, col, num);
    }

    // 验证数字在当前行是否唯一
    private boolean isValidInRow(int row, int num) {
        for (int col = 0; col < 9; col++) {
            if (userSolution[row][col] == num) {
                return false;
            }
        }
        return true;
    }

    // 验证数字在当前列是否唯一
    private boolean isValidInColumn(int col, int num) {
        for (int row = 0; row < 9; row++) {
            if (userSolution[row][col] == num) {
                return false;
            }
        }
        return true;
    }

    // 验证数字在当前3x3宫格内是否唯一
    private boolean isValidInBox(int row, int col, int num) {
        int boxStartRow = row - row % 3;
        int boxStartCol = col - col % 3;
        for (int i = boxStartRow; i < boxStartRow + 3; i++) {
            for (int j = boxStartCol; j < boxStartCol + 3; j++) {
                if (userSolution[i][j] == num) {
                    return false;
                }
            }
        }
        return true;
    }

    private void generateHint() {

        if( hintCount == 0 )
        {
            showHintDialog();
            return;
        }

        Random random = new Random();
        int row, col;

        do {
            row = random.nextInt(9);
            col = random.nextInt(9);
        } while (userSolution[row][col] != 0);

        // 将正确解决方案的数字填入
        userSolution[row][col] = correctSolution[row][col];
        adapter.notifyDataSetChanged(); // 刷新界面以显示提示
        hintCount--;
    }

    public void ShowSnackbar( String msg, int length) {

        snackbar = Snackbar.make(findViewById(android.R.id.content), msg, length);


        //與上一次訊息相同(用來判斷重複點擊相同按鈕)
        if(snackbarLastMsg.equals(msg)) snackbar.dismiss();



        View snackbarView = snackbar.getView();
        //layout置中
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                (int) getResources().getDimension(R.dimen.snackbar_width) * msg.length(), // 使用资源文件中定义的宽度
                snackbarView.getLayoutParams().height);
        params.gravity = Gravity.CENTER;
        snackbarView.setLayoutParams(params);
        //文字置中
        TextView message = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        //View.setTextAlignment需要SDK >= 17
        message.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        message.setGravity(Gravity.CENTER);
        //message.setMaxLines(1);

        snackbar.show();

        snackbarLastMsg = msg;

    }
}