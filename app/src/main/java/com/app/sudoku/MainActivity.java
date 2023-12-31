package com.app.sudoku;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {


    private SudokuAdapter adapter; // 適配器
    private int[][] userSolution = new int[9][9]; // 用戶填寫的數獨解答
    private int[][] correctSolution = new int[9][9]; // 正確的數獨解答
    private int answerValue = 0; // 答案數值

    private TextView focusedTextView = null; // 當前獲得焦點的文本視圖
    private int focusRow = 0; // 當前焦點所在行
    private int focusCol = 0; // 當前焦點所在列
    private int hintCount = 1; // 提示次數
    private Chronometer chronometer; // 計時器
    private int wrongAnswerCount = 0; // 錯誤答案次數
    private TextView wrongAnswerMsg; // 錯誤答案消息
    private Snackbar snackbar; // Snackbar
    private String snackbarLastMsg = ""; // 上一條Snackbar消息
    private RewardedAd rewardedAd; // 激勵廣告
    private InterstitialAd mInterstitialAd; // 插頁式廣告
    private final String TAG = "MainActivity"; // 日誌標籤
    private SharedPreferences gameinfo; // 遊戲信息存儲
    private String bannerAdId; // 橫幅廣告ID
    private String rewardedAdId; // 激勵廣告ID
    private String interstitialAdId; // 插頁式廣告ID
    private SudokuGenerator generator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // DEBUG
        if (BuildConfig.IS_DEBUG_MODE) {
            bannerAdId = "ca-app-pub-3940256099942544/6300978111";
            rewardedAdId = "ca-app-pub-3940256099942544/5224354917";
            interstitialAdId = "ca-app-pub-3940256099942544/1033173712";
        } else {

            bannerAdId = "ca-app-pub-8275397647362849/1721274267";
            rewardedAdId = "ca-app-pub-8275397647362849/8390624394";
            interstitialAdId = "ca-app-pub-8275397647362849/9617706466";
        }


        // 初始化廣告
        MobileAds.initialize(this, initializationStatus -> {

            InitBannerAd();
            initInterstitialAd();
            InitRewardedAd();
        });


        // 紀錄遊戲次數
        gameinfo = getSharedPreferences("gameinfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = gameinfo.edit();
        int playTimes = gameinfo.getInt("playTimes",0);
        editor.putInt("playTimes", playTimes + 1 );
        editor.commit();



        // 獲取難度
        int difficultyLevel = getIntent().getIntExtra("difficultyLevel", 0);

        // 初始化遊戲界面
        GridView gameBoard = findViewById(R.id.gameBoard);

        wrongAnswerMsg = findViewById(R.id.wrongAnswerMsg);
        wrongAnswerMsg.setText(getResources().getString(R.string.mistake) + wrongAnswerCount + " / 3");

        // 生成遊戲板
        generator = new SudokuGenerator(difficultyLevel);
        int[][] sudokuBoard = generator.getSudokuBoard();
        adapter = new SudokuAdapter(this, sudokuBoard);
        gameBoard.setAdapter(adapter);

        // 初始化計時器
        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat(getResources().getString(R.string.GameTime) + ": %s");
        chronometer.start();


        // 設置提示、返回按鈕
        ImageView btn_hint = findViewById(R.id.btn_hint);
        ImageView btn_back= findViewById(R.id.btn_back);
        btn_hint.setOnClickListener(v -> {
            generateHint();
        });
        btn_back.setOnClickListener(v -> {
            showExitGameDialog();
        });


    }

    private void InitBannerAd() {
        // 創建AdView並設置廣告單元ID
        AdView mAdView = new AdView(this);
        mAdView.setAdUnitId(bannerAdId); // 設置廣告單元ID

        AdSize adSize = AdSize.BANNER; // 或其他合適的廣告尺寸
        mAdView.setAdSize(adSize);

        // 設置AdView的佈局參數
        RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        adParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM); // 放置廣告在底部
        mAdView.setLayoutParams(adParams);

        // 添加AdView到佈局中
        RelativeLayout layout = findViewById(R.id.ad_container); // 替換為你的佈局ID
        layout.addView(mAdView);

        // 加載廣告
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void InitRewardedAd()
    {

        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, rewardedAdId,
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

        InterstitialAd.load(this,interstitialAdId, adRequest,
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

    private void LoadAdsErrorDialog(boolean isExit) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.Msg);
        builder.setMessage(R.string.ErrorAdsMsg);
        builder.setPositiveButton(R.string.Exit, (dialog, which) -> {
            dialog.dismiss();

            if(!isExit)return;

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
                //取消焦點
                focusedTextView = null;
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
            else {
                LoadAdsErrorDialog(true);
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
            else {
                LoadAdsErrorDialog(false);
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
        private static final int EASY = 0;
        private static final int MEDIUM = 1;
        private static final int HARD = 2;
        private static final int EXPERT = 3;

        private int[][] board;
        private Random random;


        public SudokuGenerator(int difficultyLevel) {
            board = new int[SIZE][SIZE];
            random = new Random();
            generateSudoku();
            removeNumbers(difficultyLevel);
        }

        private void generateSudoku() {
            SudokuSolver solver = new SudokuSolver();
            boolean uniqueSolutionFound = false;

            while (!uniqueSolutionFound) {
                clearBoard(); // 清空數獨板
                solveSudoku(0, 0);// 產生一個數獨

                if (solver.hasUniqueSolution(board)) {
                    uniqueSolutionFound = true; // 找到唯一解，退出循環
                }
            }
        }
        private void clearBoard() {
            // 將數獨板的所有儲存格設定為0
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    board[i][j] = 0;
                }
            }
        }

        private boolean solveSudoku(int row, int col) {
            if (row == SIZE) {
                row = 0;
                if (++col == SIZE) {
                    return true; // 解決整個數獨
                }
            }
            if (board[row][col] != 0) {
                return solveSudoku(row + 1, col);
            }

            int[] numbers = generateShuffledNumbers();
            SudokuSolver solver = new SudokuSolver();
            for (int num : numbers) {
                if (solver.isValidMove(board,row, col, num)) {
                    board[row][col] = num;
                    if (solveSudoku(row + 1, col)) {
                        return true;
                    }
                    board[row][col] = 0; // 回溯
                }
            }

            return false;
        }

        private int[] generateShuffledNumbers() {
            int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9};
            for (int i = numbers.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int temp = numbers[i];
                numbers[i] = numbers[j];
                numbers[j] = temp;
            }
            return numbers;
        }



        private void removeNumbers(int difficultyLevel) {
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
            } else if (difficultyLevel == EXPERT) {
                minNumbersToRemove = 64;
                maxNumbersToRemove = 64;
            }

            int numbersToRemove = random.nextInt(maxNumbersToRemove - minNumbersToRemove + 1) + minNumbersToRemove;

            Set<Integer> removedIndices = new HashSet<>();

            while (removedIndices.size() < numbersToRemove) {
                int row = random.nextInt(SIZE);
                int col = random.nextInt(SIZE);

                // 確保不刪除相同位置的數字
                int index = row * SIZE + col;
                if (!removedIndices.contains(index)) {
                    removedIndices.add(index);
                    correctSolution[row][col] = board[row][col];
                    board[row][col] = 0;
                }
            }
        }

        public int[][] getSudokuBoard() {
            userSolution = board;
            return board;
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
                // 找到一個解
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
            return null; // 所有單元格都已填滿
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

            // 檢查行
            for (int row = 0; row < 9; row++) {
                boolean[] used = new boolean[10];
                for (int col = 0; col < 9; col++) {
                    int num = userSolution[row][col];
                    if (used[num]) {
                        // 在行中發現重複數字
                        return false;
                    }
                    used[num] = true;
                }
            }

            // 檢查列
            for (int col = 0; col < 9; col++) {
                boolean[] used = new boolean[10];
                for (int row = 0; row < 9; row++) {
                    int num = userSolution[row][col];
                    if (used[num]) {
                        // 在列中發現重複數字
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

        public boolean isValidMove(int[][] board, int row, int col, int num) {
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
        private int filledNumberColor = Color.parseColor("#008000");// 已填入數字的顏色
        private int backgroundDrawable1;
        private int backgroundDrawable2;
        private int backgroundFocusDrawable;

        // 新增一個快取用來儲存已計算的儲存格背景顏色
        private SparseArray<Integer> gridColorCache = new SparseArray<>();

        public SudokuAdapter(Context context, int[][] data) {
            this.context = context;
            this.data = data;

            backgroundDrawable1 =  R.drawable.textview_background1; // 藍色
            backgroundDrawable2 =  R.drawable.textview_background2; // 白色
            backgroundFocusDrawable =  R.drawable.textview_focused_background;
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
            // 創建或複用每個單元格的視圖
            TextView textView;

            if (convertView == null) {
                // 如果沒有可複用的視圖，創建一個新的
                textView = new TextView(context);
                textView.setLayoutParams(new GridView.LayoutParams(GridView.AUTO_FIT, GridView.AUTO_FIT));
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(24);
            } else {
                // 否則，複用已存在的視圖
                textView = (TextView) convertView;
            }

            int row = position / 9;
            int col = position % 9;
            int cellValue = data[row][col];

            // 首先嘗試從快取中獲取背景顏色
            int cachedColor = gridColorCache.get(position, -1);

            if (cachedColor != -1 ) {
                // 如果快取中有背景顏色，則直接使用它
                textView.setBackgroundResource(cachedColor);
            } else {
                // 如果快取中沒有背景顏色，則計算並存儲到快取中
                int gridStyle = calculateGridColor(row, col);
                gridColorCache.put(position, gridStyle);
                textView.setBackgroundResource(gridStyle);
            }

            textView.setOnClickListener(v -> {

                // 處理格子點擊事件
                handleCellClick(row, col, textView);
            });

            if (cellValue != 0) {
                textView.setText(String.valueOf(cellValue));
                // 答案正確設定為綠色
                if (userSolution[row][col] == correctSolution[row][col]) {
                    textView.setTextColor(filledNumberColor);
                } else {
                    textView.setTextColor(Color.BLACK);
                }
            }

            return textView;
        }

        private void handleCellClick(int row, int col, TextView textView) {


            // 處理格子點擊事件
            if (focusedTextView != null) {
                int gridStyle = calculateGridColor(focusRow, focusCol);
                focusedTextView.setBackgroundResource(gridStyle);
            }

            focusedTextView = textView;

            if (!focusedTextView.getText().toString().equals("")) {
                focusedTextView = null;
                return;
            }

            focusRow = row;
            focusCol = col;

            focusedTextView.setBackgroundResource(backgroundFocusDrawable);

        }


        // 計算3x3背景顏色
        public int calculateGridColor(int row, int col) {
            int gridStyle = (row / 3 + col / 3) % 2;
            return (gridStyle == 1) ? backgroundDrawable1 : backgroundDrawable2;

        }


    }
    // 驗證使用者輸入的數字是否有效
    private boolean isValidInput(int row, int col, int num) {
        // 驗證數字是否在1到9的範圍內
        if (num < 1 || num > 9) {
            return false;
        }

        // 驗證數字在目前行、列和3x3宮格內是否唯一
        return isValidInRow(row, num) && isValidInColumn(col, num) && isValidInBox(row, col, num);
    }

    // 驗證數字在目前行是否唯一
    private boolean isValidInRow(int row, int num) {
        for (int col = 0; col < 9; col++) {
            if (userSolution[row][col] == num) {
                return false;
            }
        }
        return true;
    }

    // 驗證數字在目前列是否唯一
    private boolean isValidInColumn(int col, int num) {
        for (int row = 0; row < 9; row++) {
            if (userSolution[row][col] == num) {
                return false;
            }
        }
        return true;
    }

    // 驗證數字在目前3x3宮格內是否唯一
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

        // 填入正確答案
        userSolution[row][col] = correctSolution[row][col];
        adapter.notifyDataSetChanged(); // 刷新介面
        hintCount--;

        SudokuSolver solver = new SudokuSolver(); //求解用class
        boolean isWin =  solver.isSudokuSolutionValid(userSolution);
        if(isWin)showWinGameDialog();
    }


    public void ShowSnackbar( String msg, int length) {

        snackbar = Snackbar.make(findViewById(android.R.id.content), msg, length);


        //與上一次訊息相同(用來判斷重複點擊相同按鈕)
        if(snackbarLastMsg.equals(msg)) snackbar.dismiss();



        View snackbarView = snackbar.getView();
        //layout置中
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                (int) getResources().getDimension(R.dimen.snackbar_width) * msg.length(), // 使用文件中自定義寬度
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