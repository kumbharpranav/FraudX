package com.fraudx.detector.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnimatedBackgroundView extends View {
    private static final int PARTICLE_COUNT = 15;
    private static final float MAX_PARTICLE_SIZE = 40f;
    private static final float MIN_PARTICLE_SIZE = 10f;
    private static final float MAX_SPEED = 1.5f;
    private static final long FRAME_DELAY = 16; // 60fps
    
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private final Runnable animator = new Runnable() {
        @Override
        public void run() {
            updateParticles();
            invalidate();
            // Schedule next animation frame
            postOnAnimation(this);
        }
    };
    
    private boolean isAnimating = false;
    
    public AnimatedBackgroundView(Context context) {
        super(context);
        init();
    }
    
    public AnimatedBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public AnimatedBackgroundView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // Setup paint
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        
        // Set hardware acceleration
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Start animation when view is attached
        startAnimation();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Stop animation when view is detached
        stopAnimation();
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Initialize particles when size is known
        initParticles();
    }
    
    private void initParticles() {
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(createRandomParticle());
        }
    }
    
    private Particle createRandomParticle() {
        int width = getWidth();
        int height = getHeight();
        
        if (width <= 0 || height <= 0) {
            return new Particle(0, 0, 0, 0, 0, Color.WHITE);
        }
        
        float x = random.nextFloat() * width;
        float y = random.nextFloat() * height;
        float size = MIN_PARTICLE_SIZE + random.nextFloat() * (MAX_PARTICLE_SIZE - MIN_PARTICLE_SIZE);
        float speedX = -MAX_SPEED + random.nextFloat() * (MAX_SPEED * 2);
        float speedY = -MAX_SPEED + random.nextFloat() * (MAX_SPEED * 2);
        
        // Ensure particles move at least a little
        if (Math.abs(speedX) < 0.2f) speedX = speedX < 0 ? -0.2f : 0.2f;
        if (Math.abs(speedY) < 0.2f) speedY = speedY < 0 ? -0.2f : 0.2f;
        
        // Different semi-transparent white shades
        int alpha = 10 + random.nextInt(30);
        int color = Color.argb(alpha, 255, 255, 255);
        
        return new Particle(x, y, size, speedX, speedY, color);
    }
    
    private void updateParticles() {
        int width = getWidth();
        int height = getHeight();
        
        if (width <= 0 || height <= 0) return;
        
        for (Particle particle : particles) {
            // Update position
            particle.x += particle.speedX;
            particle.y += particle.speedY;
            
            // Bounce off edges
            if (particle.x < 0) {
                particle.x = 0;
                particle.speedX = -particle.speedX;
            } else if (particle.x > width) {
                particle.x = width;
                particle.speedX = -particle.speedX;
            }
            
            if (particle.y < 0) {
                particle.y = 0;
                particle.speedY = -particle.speedY;
            } else if (particle.y > height) {
                particle.y = height;
                particle.speedY = -particle.speedY;
            }
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw each particle
        for (Particle particle : particles) {
            paint.setColor(particle.color);
            
            // Draw a square for each particle
            float left = particle.x - particle.size / 2;
            float top = particle.y - particle.size / 2;
            float right = particle.x + particle.size / 2;
            float bottom = particle.y + particle.size / 2;
            
            // Rotate the square by drawing a path
            Path path = new Path();
            float centerX = particle.x;
            float centerY = particle.y;
            float halfSize = particle.size / 2;
            
            // Create a rotated square
            path.moveTo(centerX - halfSize, centerY - halfSize);
            path.lineTo(centerX + halfSize, centerY - halfSize);
            path.lineTo(centerX + halfSize, centerY + halfSize);
            path.lineTo(centerX - halfSize, centerY + halfSize);
            path.close();
            
            // Draw the path
            canvas.save();
            canvas.rotate(particle.rotation, centerX, centerY);
            canvas.drawPath(path, paint);
            canvas.restore();
            
            // Update rotation for next frame
            particle.rotation += particle.rotationSpeed;
        }
    }
    
    public void startAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            postOnAnimation(animator);
        }
    }
    
    public void stopAnimation() {
        isAnimating = false;
        removeCallbacks(animator);
    }
    
    // Inner class to represent a particle
    private static class Particle {
        float x, y;
        float size;
        float speedX, speedY;
        int color;
        float rotation = 0;
        float rotationSpeed;
        
        Particle(float x, float y, float size, float speedX, float speedY, int color) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speedX = speedX;
            this.speedY = speedY;
            this.color = color;
            
            // Random rotation speed between -2 and 2 degrees per frame
            this.rotationSpeed = -2f + (float)(Math.random() * 4f);
        }
    }
} 