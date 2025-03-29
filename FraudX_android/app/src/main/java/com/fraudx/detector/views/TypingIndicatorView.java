package com.fraudx.detector.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.fraudx.detector.R;

public class TypingIndicatorView extends LinearLayout {
    private ImageView dot1, dot2, dot3;
    private Animation fadeInAnimation, fadeOutAnimation;
    private boolean isAnimating = false;

    public TypingIndicatorView(Context context) {
        super(context);
        init(context);
    }

    public TypingIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TypingIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.typing_indicator, this, true);
        
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        
        fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.dot_fade_in);
        fadeOutAnimation = AnimationUtils.loadAnimation(context, R.anim.dot_fade_out);
        
        fadeInAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                if (animation == fadeInAnimation && isAnimating) {
                    startFadeOutAnimation();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        
        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                if (animation == fadeOutAnimation && isAnimating) {
                    startFadeInAnimation();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }
    
    public void startAnimation() {
        isAnimating = true;
        startFadeInAnimation();
    }
    
    public void stopAnimation() {
        isAnimating = false;
        dot1.clearAnimation();
        dot2.clearAnimation();
        dot3.clearAnimation();
    }
    
    private void startFadeInAnimation() {
        dot1.startAnimation(fadeInAnimation);
        
        fadeInAnimation.setStartOffset(150);
        dot2.startAnimation(fadeInAnimation);
        
        fadeInAnimation.setStartOffset(300);
        dot3.startAnimation(fadeInAnimation);
        
        fadeInAnimation.setStartOffset(0);
    }
    
    private void startFadeOutAnimation() {
        dot1.startAnimation(fadeOutAnimation);
        
        fadeOutAnimation.setStartOffset(150);
        dot2.startAnimation(fadeOutAnimation);
        
        fadeOutAnimation.setStartOffset(300);
        dot3.startAnimation(fadeOutAnimation);
        
        fadeOutAnimation.setStartOffset(0);
    }
} 