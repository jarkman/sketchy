package com.jarkman.sketchy;



import java.util.Vector;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.preference.PreferenceManager;

/***********************************************************
 * 
 * 
 * VectorWalker.java
 * 
 * This is a vectoriser I wrote for Sketchy. It is simple-minded, probably not very efficient, but gets the job done.
 *
 */
public class VectorWalker {

	int maxX ; 
	int maxY;
	int mShortLineLimit = 5;  // must be at least 2 to avoid limit problems in simplify
	int mLineBendLimit = 1;  // limit on line wiggle displacement in simplification 
	
	boolean [][] mPixels;
	boolean [][] mWalked;
	
	Vector<Vector<Point>> mPointLists = new Vector<Vector<Point>>();
	
	Sketchy mSketchy;
	private Bitmap mEdgeBitmap;
	private Bitmap mVectorBitmap;
	private Canvas mVectorCanvas;
	
	
	Point mDirections[] = new Point[8];
	

	

	public VectorWalker( Sketchy sketchy, Bitmap source, Bitmap vectorImage, Canvas vectorCanvas )
	{
		maxX = sketchy.mImageWidth; 
		maxY = sketchy.mImageHeight;
		
		
		mPixels = new boolean[maxX][maxY];
		mWalked = new boolean[maxX][maxY];

		
		mSketchy = sketchy;
		mEdgeBitmap = source;
		mVectorBitmap = vectorImage;
		mVectorCanvas = vectorCanvas;
		
		// clockwise sequence
		mDirections[0]=new Point(1, 0);
		mDirections[1]=new Point(1, 1);
		mDirections[2]=new Point(0, 1);
		mDirections[3]=new Point(-1, 1);
		mDirections[4]=new Point(-1, 0);
		mDirections[5]=new Point(-1, -1);
		mDirections[6]=new Point(0, -1);
		mDirections[7]=new Point(1, -1);
		
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
	
	public void raster2vector()
	{
		mSend = null;
		
		if( ! mSketchy.mWorking )
			return;

		load();
		
		if( ! mSketchy.mWorking )
			return;

		walk();
		
		if( ! mSketchy.mWorking )
			return;

		simplify();

		if( ! mSketchy.mWorking )
			return;

		drawVectors();
		
		if( ! mSketchy.mWorking )
			return;

		buildVectorString();
	 
		
	}

	private void load()
	{
		for( int x = 0; x < maxX; x ++ )
		{
			if( x % 20 == 0 )
				mSketchy.updateProgress("Vectoriser load " + x + " / " + maxX  );

			for( int y = 0; y < maxY; y ++)
			{
				int color = mEdgeBitmap.getPixel(x, y);
				mPixels[x][y] = color == -1; // as left by Canny
			}
		}
				
	
	}
	
	private void walk()
	{
		for( int x = 1; x < maxX-1; x ++ )
		{
			mSketchy.updateProgress("Vectoriser walk " + x + " / " + maxX + " (pointlists: " + mPointLists.size()+")" );


			for( int y = 1; y < maxY-1; y ++)
			{
				if( ! mWalked[x][y] )
					if( mPixels[x][y] )
						walkFrom( x, y );
			}
		}	
	
	}
	
	int nX, nY, nDirection;
	
	private void walkFrom( int x, int y )
	{
		mWalked[x][y] = true;
		Vector<Point> v = new Vector<Point>();
		v.add(new Point(x, y));
		
		int direction = 0;
		
		
		while( findNeighbour( x, y, direction ))
		{
			x = nX;
			y = nY;
			direction = nDirection;
			
			mWalked[x][y] = true;
			v.add(new Point(x, y));

		}
		
		//if( v.size() > mShortLineLimit ) // discard short lines
			mPointLists.add( v );
	}
	
	private boolean findNeighbour( int x, int y, int direction )
	{
		if( testNeighbour( x, y, direction))  // keep going the same way
			return true;
		
		// test neighbours, giving priority to the direction we are already headed
		return testNeighbour( x, y, direction - 1 ) ||
	 	       testNeighbour( x, y, direction + 1 ) ||
	 	       testNeighbour( x, y, direction - 2 ) ||	 	       
	 	       testNeighbour( x, y, direction + 2 ) ||
	 	       testNeighbour( x, y, direction - 3 ) ||
	 	       testNeighbour( x, y, direction + 3 ) ||
	 	       testNeighbour( x, y, direction - 4 ) ||
	 	       testNeighbour( x, y, direction + 4);
				
			
	}
	
	private boolean testNeighbour( int x, int y, int direction )
	{
		direction = (direction + 8) % 8;  // wrap to range
		int dx = mDirections[direction].x;
		int dy = mDirections[direction].y; 
		
		x += dx;
		y += dy;
		
		if( x < 0 || x >= maxX || y < 0 || y >= maxY )  // off the edge
			return false;
		
		if( mPixels[x][y] && ! mWalked[x][y] )  // a set pixel that we haven't already consumed! Hurrah!
		{
			nDirection = direction;
			nX = x;
			nY = y;
			return true;
		}
		
		return false;
	}
	
	private void show()
	{
		drawVectors();
		mSketchy.updateProgress(null);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	private void simplify()
	{
		
		show();
		
		straighten();
		
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

		
		dust();
		
		if( ! mSketchy.mWorking )
			return;
		
		show();

		report();
		
	}
	
	private void straighten()
	{
		int points = 0;
		
		for( int v = 0; v < mPointLists.size(); v ++ )
		{

			Vector<Point> pointList = (Vector<Point>) mPointLists.get(v);
			
			int startIndex = 0;
			Point startPoint = (Point) pointList.get(startIndex);
		
			Vector<Point> simpler = new Vector<Point>();
			simpler.add(startPoint);
			
			
			for( int p = 1; p < pointList.size(); p ++ )	
		    {	
				if( lineTooBent( pointList, startIndex, p, startPoint, (Point) pointList.get(p) ))
				{
					simpler.add(pointList.get(p-1));
					startIndex = p;
					startPoint = (Point) pointList.get(startIndex);
					
				}
		    		    	
		    }
			
			simpler.add(pointList.get(pointList.size()-1));  // loop will never add the last point
			
			points += simpler.size();
			
			mPointLists.set(v, simpler);
			
			if( v%10 == 0 )
				mSketchy.updateProgress("Vectoriser straighten " + v + " / " + mPointLists.size() + " (points: " + points +")" );

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
		
		points += 1; // for new initial point
		
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
		
		// Added initial send of first point 20/5/11 as a voodoo fix for a strange problem in which an extraneous line was drawn before proper drawing began
		
		Vector pointList0 = (Vector) mPointLists.get(0);		
		Point start0 = (Point) pointList0.get(0);
		sendPoint( start0, -15 );
		
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
