package com.yeasin.coloringblocks;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class GameActivity extends AppCompatActivity {

    RelativeLayout gameActivity;
    Point displaySize;

    TextView titleTextView;
    Button playButton, shareButton;

    Integer score, bestScore;
    SharedPreferences sharedPreferences;

    Timer blocksTimer;

    Shape player;
    boolean isGameStarted;

    AdView adView;

    class Shape extends View {
        public Integer color;
        Shape(Context context) {
            super(context);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        gameActivity = (RelativeLayout) findViewById(R.id.activity_game);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        sharedPreferences = getApplication().getSharedPreferences(getPackageName(), MODE_PRIVATE);
        bestScore = sharedPreferences.getInt("best_score", 0);

        titleTextView = (TextView) findViewById(R.id.text_view_title);
        titleTextView.setTypeface(Typeface.createFromAsset(getAssets(), getString(R.string.font_name)));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 60);

        player = new Shape(getApplicationContext());
        float playerSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
        RelativeLayout.LayoutParams playerLayoutParams = new RelativeLayout.LayoutParams((int) playerSize, (int) playerSize);
        player.setLayoutParams(playerLayoutParams);
        player.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.player));
        player.setX(displaySize.x / 2 - player.getLayoutParams().width / 2);
        player.setY(displaySize.y - displaySize.y / 4);
        gameActivity.addView(player);
        changePlayerColor();
        player.setVisibility(View.INVISIBLE);

        playButton = (Button) findViewById(R.id.button_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newGame();
            }
        });

        shareButton = (Button) findViewById(R.id.button_share);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent().setAction(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, String.format(Locale.getDefault(), getString(R.string.app_name) +  " http://play.google.com/store/apps/details?id=" + getPackageName())).setType("text/plain"));
            }
        });

        isGameStarted = false;

        adView = (AdView) findViewById(R.id.ad_view);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
        adView.loadAd(adRequest);
    }

    void changePlayerColor() {
        TypedArray colors = getResources().obtainTypedArray(R.array.colors);
        Integer color = colors.getColor(new Random().nextInt(colors.length()), 0);
        ((GradientDrawable) player.getBackground()).setColor(color);
        player.color = color;
        colors.recycle();
    }

    void newGame() {
        isGameStarted = true;
        score = 0;
        titleTextView.setText(String.format(Locale.getDefault(), "%d", score));
        player.setVisibility(View.VISIBLE);
        playButton.setVisibility(View.INVISIBLE);
        shareButton.setVisibility(View.INVISIBLE);
        blocksTimer = new Timer();
        blocksTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        titleTextView.setText(String.format(Locale.getDefault(), "%d", score++));

                        Integer blockSize = displaySize.x / 4;
                        RelativeLayout.LayoutParams blockLayoutParams = new RelativeLayout.LayoutParams(blockSize, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics()));
                        TypedArray colors = getResources().obtainTypedArray(R.array.colors);
                        ArrayList<Integer> colorsArrayList = new ArrayList<>();
                        for (int index = 0; index < colors.length(); index++) {
                            colorsArrayList.add(colors.getColor(index, 0));
                        }
                        List animations = new ArrayList();
                        for (Integer blockCount = 0; blockCount < 4; blockCount++) {
                            final Shape block = new Shape(getApplicationContext());
                            block.setLayoutParams(blockLayoutParams);
                            block.setX(blockSize * blockCount);
                            block.setY(-block.getLayoutParams().height);
                            Integer color = colorsArrayList.get(new Random().nextInt(colorsArrayList.size()));
                            block.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.block));
                            ((GradientDrawable) block.getBackground()).setColor(color);
                            block.color = color;
                            colorsArrayList.remove(color);
                            gameActivity.addView(block);
                            final ObjectAnimator blockAnimator = ObjectAnimator.ofFloat(block, "y", displaySize.y);
                            blockAnimator.setDuration(3000);
                            blockAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                    if (new Rect((int) block.getX(), (int) block.getY(), (int) block.getX() + block.getLayoutParams().width, (int) block.getY() + block.getLayoutParams().height).intersects((int) player.getX(), (int) player.getY(), (int) player.getX() + player.getLayoutParams().width, (int) player.getY() + player.getLayoutParams().height)) {
                                        if (isGameStarted == true) {
                                            if (block.color.equals(player.color)) {
                                                gameActivity.removeView(block);
                                                blockAnimator.cancel();
                                                changePlayerColor();
                                            }
                                            else {
                                                isGameStarted = false;
                                                endGame();
                                            }
                                        }
                                    }
                                }
                            });
                            blockAnimator.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    gameActivity.removeView(block);
                                }
                            });
                            animations.add(blockAnimator);
                        }
                        colors.recycle();
                        AnimatorSet blocksAnimatorSet = new AnimatorSet();
                        blocksAnimatorSet.playTogether(animations);
                        blocksAnimatorSet.start();
                        gameActivity.bringChildToFront(adView);
                    }
                });
            }
        }, 0, 1000);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && player.getVisibility() == View.VISIBLE) {
            ObjectAnimator playerAnimator = new ObjectAnimator();
            playerAnimator.setTarget(player);
            playerAnimator.setPropertyName("x");
            playerAnimator.setDuration(100);
            if (event.getX() <= displaySize.x / 4) {
                playerAnimator.setFloatValues(((displaySize.x / 4) / 2) - player.getLayoutParams().width / 2);
            }
            if (event.getX() > displaySize.x / 4 && event.getX() <= displaySize.x / 2) {
                playerAnimator.setFloatValues((displaySize.x / 2 - (displaySize.x / 4) / 2) - player.getLayoutParams().width / 2);
            }
            if (event.getX() > displaySize.x / 2 && event.getX() <= displaySize.x / 2 + displaySize.x / 4) {
                playerAnimator.setFloatValues(displaySize.x / 2 + displaySize.x / 8 - player.getLayoutParams().width / 2);
            }
            if (event.getX() > displaySize.x / 2 + displaySize.x / 4) {
                playerAnimator.setFloatValues((displaySize.x - (displaySize.x / 4) / 2) - player.getLayoutParams().width / 2);
            }
            playerAnimator.start();
        }
        return super.onTouchEvent(event);
    }

    void endGame() {

        if (blocksTimer != null) {
            blocksTimer.cancel();
        }

        if (bestScore < score) {
            bestScore = score;
            sharedPreferences.edit().putInt("best_score", bestScore).apply();
        }

        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        titleTextView.setText(String.format(Locale.getDefault(), "You scored %d, best score is %d", score, bestScore));
        playButton.setVisibility(View.VISIBLE);
        shareButton.setVisibility(View.VISIBLE);
        player.setVisibility(View.INVISIBLE);
    }

}
