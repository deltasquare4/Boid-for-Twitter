package com.teamboid.twitter.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EdgeEffect;

public class GlowableRelativeLayout extends View {
		
	public GlowableRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		glow = new EdgeEffect(context);
	}
	
	public void glow() {
		glow.onPull(pullAt);
		invalidate();
	}
	
	private float pullAt = 0.001F;
	private int drawNo = 1;
	private EdgeEffect glow;
	public static final float MAX_GLOW = 0.5F;
	
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);	
		if(!glow.isFinished()) {
			final int height = getHeight() - getPaddingTop() - getPaddingBottom();
            canvas.rotate(270);
            canvas.translate(-height + getPaddingTop(), 0);
			glow.setSize(height, getWidth());
			if(glow.draw(canvas)) {
				invalidate();
				drawNo += 1;
				if(drawNo == 5) { // Slows the increase down
					pullAt += 0.01;
					if(pullAt <= MAX_GLOW){
						glow.onPull(pullAt);
					}
					drawNo = 0;
				}
				if(pullAt >= MAX_GLOW){
					glow.onRelease();
				}
			}
		}
	}
}
