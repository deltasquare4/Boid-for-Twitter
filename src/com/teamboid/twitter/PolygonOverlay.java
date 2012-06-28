package com.teamboid.twitter;

import twitter4j.GeoLocation;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Paint.Style;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class PolygonOverlay extends Overlay {
	Path path;
	GeoLocation[] points;
	
	public PolygonOverlay(GeoLocation[] points){
		this.points = points;
	}
	
	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
            long when){
		if(shadow == false){
			//if(path == null){
				path = new Path();
				
				Projection pr = mapView.getProjection();
				Point ap = new Point();
				
				GeoPoint googlePoint = BetterMapView.fromTwitter(points[0]);
				pr.toPixels(googlePoint, ap);
				path.moveTo(ap.x, ap.y);
				
				for(GeoLocation point : points){
					googlePoint = BetterMapView.fromTwitter(point);
					
					pr.toPixels(googlePoint, ap);
					path.lineTo(ap.x, ap.y);
					Log.d("d", ap.x + " " + ap.y);
				}
			//}
			
			Paint p = new Paint();
			p.setStyle(Style.FILL);
			p.setColor(Color.parseColor("#AA0333"));
			p.setAlpha(170);
			p.setAntiAlias(true);
			canvas.drawPath(path, p);
			
			p.setAlpha(240);
			p.setStyle(Style.STROKE);
			canvas.drawPath(path, p);
			
			Log.d("d", "POLYGON DRAW");
		}
		return super.draw(canvas, mapView, shadow, when);
	}
}
