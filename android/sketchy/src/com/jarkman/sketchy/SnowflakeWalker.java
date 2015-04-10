package com.jarkman.sketchy;



import java.util.Vector;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.preference.PreferenceManager;


public class SnowflakeWalker {

	int maxX ; 
	int maxY;
	int mShortLineLimit = 5;  // must be at least 2 to avoid limit problems in simplify
	int mLineBendLimit = 1;  // limit on line wiggle displacement in simplification 
	
	
	Vector<Vector<Point>> mPointLists = new Vector<Vector<Point>>();
	
	Sketchy mSketchy;
	//private Bitmap mVectorBitmap;
	private Canvas mVectorCanvas;

	
	PointF origin;
	PointF top;

	
	int maxSteps = 120;
	
	float startFractions[] = new float[maxSteps];
	float lengthFractions[] = new float[maxSteps];
	int branches[] = new int[maxSteps];
	
	
	public SnowflakeWalker( Sketchy sketchy, Bitmap vectorImage, Canvas vectorCanvas )
	{
		maxX = sketchy.mImageWidth; 
		maxY = sketchy.mImageHeight;
		
		origin = new PointF( maxX/2, maxY/2 );
		top = new PointF( maxX/2, (7 * maxY) / 10  );
		
		mSketchy = sketchy;
		//mVectorBitmap = vectorImage;
		mVectorCanvas = vectorCanvas;
		
		
		 SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(sketchy);

	   	String shortLineLimitS = prefs.getString("shortLineLimit", "5");  // real default is set in Settings
	  	String lineBendLimitS = prefs.getString("lineBendLimit", "1");

	  	mShortLineLimit = (Integer.valueOf(shortLineLimitS) * mSketchy.mImageWidth) / Settings.mBaseImageWidth;

	  	mLineBendLimit = (Integer.valueOf(lineBendLimitS) * mSketchy.mImageWidth) / Settings.mBaseImageWidth;

	 	
	 	
	 	if( mShortLineLimit < 2 ) 
	 		mShortLineLimit = 2;
	 	
	 	if( mLineBendLimit < 1 ) 
	 		mLineBendLimit = 1;
		 		 	
	}
	
	public void generate()
	{
		mSend = null;
		
		if( ! mSketchy.mWorking )
			return;

		build();

		show();
		
		
		if( ! mSketchy.mWorking )
			return;
		

		show();
		
		
		sequence();

		if( ! mSketchy.mWorking )
			return;

		show();

		join();

		if( ! mSketchy.mWorking )
			return;
		
		show();
		
		
		report();
		
		if( ! mSketchy.mWorking )
			return;

		drawVectors();
		
		if( ! mSketchy.mWorking )
			return;

		buildVectorString();
	 
		
	}

	
	
	private void build()
	{
		
		// need to pregenerate all the generator numbers so they depend only on recursion depth
		for( int i = 0; i < maxSteps; i ++ )
		{
			startFractions[i] = (float) (0.2 + (float) 0.6 *  Math.random());
			//lengthFractions[i] = (float) ((float) Math.random() * startFractions[i] );
			lengthFractions[i] =  (float) ((0.6 + (float) 0.5 *  Math.random()) * (float) (0.9 * (0.5 - Math.abs(startFractions[i] - 0.6)))) ;
			branches[i] = 2 + (int) (Math.random() * 3.0);
			if( i < 3 && branches[i] < 1)
				branches[i] = 1;
				
		}
		
		
		

		// generate radial line
		addLine( origin, top, 0,0 );
		
	}
	
	int addLine( PointF p1, PointF p2, int depth, int step )
	{
		
		if( depth > 1 )
			return step+1;
		
		addSegments( p1, p2 );

		// ignore short lines
		if( Math.abs( p2.x-p1.x)+ Math.abs(p2.y-p1.y) < 12  )
			return step+1;

		
		
		
		if( step >= maxSteps -1 )
			return step+1;
		
		if( branches[step] < 1 )
			return step+1;
		
		
		
		// pairs of child lines, attached to this line somewhere near the middle, at 60 degrees one way or the other
		
		int numBranches = branches[step];
		
		for( int branch = 0; branch < numBranches && step < maxSteps; branch ++ )
		{
		
			PointF start = new PointF( p1.x + ((p2.x-p1.x) * startFractions[step]),  p1.y + ((p2.y-p1.y) * startFractions[step] ));
			PointF end0 =  new PointF( start.x + ((p2.x-p1.x) * lengthFractions[step]),  start.y + ((p2.y-p1.y) * lengthFractions[step] ));
			
			PointF end1 = rotateEnd( start, end0, -60);
			PointF end2 = rotateEnd( start, end0, 60);
			
			int oldStep = step;
			
			addLine( start, end1, depth+1, oldStep + 1);
			step = addLine( start, end2, depth+1, oldStep + 1);
			
			show();
		}
		

		
		
		return step;
		
	}
	
	PointF rotateEnd( PointF start, PointF end, int degrees )
	{
		float dx = end.x - start.x;
		float dy = end.y - start.y;
		
		double theta = degrees * 2 * Math.PI / 360.0;
		
		return new PointF( (float) (start.x + dx * Math.cos( theta ) - dy * Math.sin( theta )), 
							(float) (start.y + dx * Math.sin( theta ) + dy * Math.cos( theta )) );
	}
	
	private void addSegments( PointF p1, PointF p2 )
	{
		// add the six vectors
		for( int a = 0; a < 360; a += 60 )
		{
			PointF end1 = rotateEnd( origin, p1, a);
			PointF end2 = rotateEnd( origin, p2, a);
			
			Vector<Point> v = new Vector<Point>();
			v.add(new Point((int)end1.x, (int)end1.y));
			v.add(new Point((int)end2.x, (int)end2.y));
			
			mPointLists.add( v );
			
		}
	}
	
	private void show()
	{
		drawVectors();
		mSketchy.updateProgress(null);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	
	
	private void sequence()
	{
		
		// Move lines around in the list so that liens tend to follow on from each other
		
		for( int v1 = 0; v1 < mPointLists.size() - 1; v1 ++ )
		{

			Vector<Point> pointList1 = (Vector<Point>) mPointLists.get(v1);

			Point v1a = (Point) pointList1.get(0);
			Point v1b = (Point) pointList1.get(pointList1.size() - 1);
		
			int minDistance = this.maxX;
			int minV = -1;
			
			// find the line that has the nearest end to either of our ends
			for( int v2 = v1 + 1; v2 < mPointLists.size(); v2 ++ )
			{
	
				Vector<Point> pointList2 = (Vector<Point>) mPointLists.get(v2);

				Point v2a = (Point) pointList2.get(0);
				Point v2b = (Point) pointList2.get(pointList2.size() - 1);

				int distance = Math.min( Math.min( d( v1a, v2a ), d( v1a, v2b ) ), Math.min( d( v1b, v2a ), d( v1b, v2b ) ));
			
				if( distance < minDistance )
				{
					minDistance = distance;
					minV = v2;
					
				}
			}
			
			if( minV != -1 )
			{
				int v2 = minV;
				Vector<Point> pointList2 = (Vector<Point>) mPointLists.get(v2);

				Point v2a = (Point) pointList2.get(0);
				Point v2b = (Point) pointList2.get(pointList2.size() - 1);

				
				// change the direction of one or both lines to get the min end distance
				if( minDistance == d( v1a, v2a ))
				{
					reverse( v1 );
					
				}
				else if( minDistance ==  d( v1a, v2b ) )
				{
					reverse( v1 );
					reverse( v2 );
					
				}
				else if( minDistance ==  d( v1b, v2a ))
				{
					// already sensible
				}
				else if( minDistance ==  d( v1b, v2b ))
				{
					reverse( v2 );
				}
				
				// and move the second line to a better place in the list
				swap( v2, v1+1 );
				
			}
				
			if( v1%10 == 0 )
				mSketchy.updateProgress("Vectoriser sequence " + v1 + " / " + mPointLists.size() );

		  }
	}
	
	private void join()
	{
		
		// Join adjacent lines
		
		for( int v1 = 0; v1 < mPointLists.size() - 1;  )
		{

			Vector<Point> pointList1 = (Vector<Point>) mPointLists.get(v1);

			Point v1b = (Point) pointList1.get(pointList1.size() - 1);
		
			
			int v2 = v1 + 1;
			
			Vector<Point> pointList2 = (Vector<Point>) mPointLists.get(v2);

			Point v2a = (Point) pointList2.get(0);
			
			if( d( v1b, v2a ) < 2) // skip small gaps
			{
				append( v1, v2 );  // add the second lien to the first linesd
			}
			else
			{
				v1++;
			}
			
			if( v1%10 == 0 )
				mSketchy.updateProgress("Vectoriser join " + v1 + " / " + mPointLists.size() );

		}
	}

	private void dust()
	{
		// Join adjacent lines
		
		for( int v1 = 0; v1 < mPointLists.size() - 1;  )
		{

			Vector<Point> pointList1 = (Vector<Point>) mPointLists.get(v1);

			Point v1a = (Point) pointList1.get(0);
			Point v1b = (Point) pointList1.get(pointList1.size() - 1);
		
			int size = Math.max(d(v1a,v1b), pointList1.size());
			
			int rad = d( v1a, new Point( maxX/2, maxY/2));
			
			if( rad < maxX/4 ) // allow lines in middle of pic to be shorter - this is really a cheat to keep more eyes
				size = size * 2;
			
			if( size < mShortLineLimit )
			{
				mPointLists.remove(v1);
			}
			else
			{
				v1++;
			}
			
			if( v1%10 == 0 )
				mSketchy.updateProgress("Vectoriser dust " + v1 + " / " + mPointLists.size() );
	
		}
		
		mSketchy.updateProgress("Vectoriser dust " + mPointLists.size()  );

	}
	
	private void report()
	{
		int points = 0;
		
		for( int v1 = 0; v1 < mPointLists.size() - 1; v1++  )
		{

			Vector<Point> pointList1 = (Vector<Point>) mPointLists.get(v1);
			points += pointList1.size();
			
			
	
		}
		
		mSketchy.updateProgress("Vectoriser - " + mPointLists.size() + " vectors, " + points + " points" );

	}
	
	
	private void append( int v1, int v2 )
	{
		if( v1 == v2 )
			return;
		
		Vector<Point> list1 = (Vector<Point>) mPointLists.get(v1);
		Vector<Point> list2 = (Vector<Point>) mPointLists.get(v2);
		
		for( Point p: list2 )
			list1.add(p);
		
		mPointLists.remove(v2);

	}
	private void reverse( int v )
	{
		Vector<Point> list1 = (Vector<Point>) mPointLists.get(v);
		Vector<Point>list2 = new Vector<Point>();
		
		for( int i = list1.size() - 1; i >= 0 ; i -- )
			list2.add(list1.get(i));
		
		mPointLists.set(v, list2);

	}
	
	private void swap( int v1, int v2 )
	{
		if( v1 == v2 )
			return;
		
		Vector<Point> list1 = (Vector<Point>) mPointLists.get(v1);
		mPointLists.set(v1, mPointLists.get(v2));
		mPointLists.set(v2, list1);

	}
	
	private int d( Point a, Point b  )
	{
		return( Math.abs( a.x-b.x ) + Math.abs(a.y-b.y));
	}

	
	private boolean lineTooBent( Vector<Point> pointList, int startIndex, int endIndex, Point startPoint, Point endPoint )
	{
		// does this segment of the point list bend too much to be converted to a single segment ? 
		
		if( endIndex <= startIndex + 1 )
			return false; // two points is always a straight line
		
		
		for( int p = startIndex + 1; p < endIndex; p ++ )
		{
			if( pointTooFarFromLine( pointList.get(p), startPoint, endPoint ))
				return true;
		}
		
		return false;
	}

	
	private boolean pointTooFarFromLine( Point pt, Point p1, Point p2 )
	{
		double xt, yt, x1, y1, x2, y2;
		xt = pt.x;
		yt = pt.y;
		x1 = p1.x;
		y1 = p1.y;
		x2 = p2.x;
		y2 = p2.y;
		
		double num = Math.abs((x2-x1) * (y1-yt)  - (x1-xt)*(y2-y1));
		double dom = Math.sqrt( (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1) );
		
		if( Math.abs( dom ) < 0.1 )  // line of 0 length - shouldn't happen
			return false;
			
		return (num / dom) > (double) mLineBendLimit;
	}
	
	private void drawVectors()
	{

		//int translucent = Color.argb(128, 255,255,255);
		int translucent = Color.argb(255, 255,255,255);
		mVectorCanvas.drawColor(translucent);
		
		Paint blackPaint = new Paint(Color.BLACK);
		
		for( int v = 0; v < mPointLists.size(); v ++ )
		{
			
			Vector pointList = (Vector) mPointLists.get(v);
			
			Point start = (Point) pointList.get(0);
			
			for( int p = 1; p < pointList.size(); p ++ )	
		    {	
				Point end = (Point) pointList.get(p);
		    	mVectorCanvas.drawLine(start.x, start.y, end.x, end.y, blackPaint);
		    	
		    	start = end;
		    }
		  }
	}

	public byte[] mSend;
	int nextSend = 0;
	
	private void buildVectorString()
	{
		
		int points = 0;
		for( int v = 0; v < mPointLists.size(); v ++ )
		{
			Vector pointList = (Vector) mPointLists.get(v);
			points += (pointList.size() + 2);
		}
		
		
		if( points > 253 )  // Arduino can actually manage 300 points, but we send the number of points in a single byte, so the limit is set a bit lower...
			points = 253;
		
		mSend = new byte[ 1 + 3 * points ];
		nextSend = 0;
		mSend[nextSend++] = (byte) points;
		
		
		for( int v = 0; v < mPointLists.size(); v ++ )
		{
			
			Vector pointList = (Vector) mPointLists.get(v);
			
			Point start = (Point) pointList.get(0);
			
			sendPoint( start, -10 );
			
			for( int p = 0; p < pointList.size(); p ++ )	
		    {	
				Point point = (Point) pointList.get(p);
				sendPoint( point, 0 );
		    	
		    	start = point;
		    }
			
			sendPoint( start, -10 );
		  }
	
	}
	
	private void sendPoint( Point p, int z )
	{
		if( nextSend > mSend.length - 3 )
			return;
		
		int maxPenDisplacement = 80; // mechanical limit in mm to scale to
		// nb - our protocol uses bytes for mm - a bigger machine needs a better protocol!
		
		// scale x & y from 0->maxX to -maxPenDisplacement->+maxPendDisplacement, then add 127
		
		mSend[nextSend++] = (byte) (((p.x * 2* maxPenDisplacement ) / (maxX + 4) ) - maxPenDisplacement + 127);
		mSend[nextSend++] = (byte) (((p.y * 2 * maxPenDisplacement ) / (maxY + 4) ) - maxPenDisplacement + 127);
		mSend[nextSend++] = (byte) (z + 127); 
	}	
}
