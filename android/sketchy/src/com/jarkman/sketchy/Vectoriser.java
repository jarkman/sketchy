package com.jarkman.sketchy;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Vectoriser {
	/*
	 * http://cardhouse.com/computer/vectcode.htm
	 * 
	 * 
	 * 
	 * After trying this for a bit, I wrote my own vectoriser, in VectorWalker.java. 
	 * 
	 * This code is left in for sentimental reasons, but is not currently called.
	 */


	int MAXSIZEX; 
	int MAXSIZEY;

	int[][] ab = new int[MAXSIZEX][MAXSIZEY];
	vector[] V = new vector[10000];
	int Vnum = 0;
	int numVectors = 0;
	
	Sketchy mSketchy;
	private Bitmap mEdgeBitmap;
	private Bitmap mVectorBitmap;
	private Canvas mVectorCanvas;
	
	public class vector {

		int charnum;
	    int prev;
	    int sx,sy;
	    int ex,ey;
	    int next;
	    int status;
	  
	}

	public Vectoriser( Sketchy sketchy, Bitmap source, Bitmap vectorImage, Canvas vectorCanvas )
	{
		MAXSIZEX = sketchy.mImageWidth; 
		MAXSIZEY = sketchy.mImageHeight;
		mSketchy = sketchy;
		mEdgeBitmap = source;
		mVectorBitmap = vectorImage;
		mVectorCanvas = vectorCanvas;
	}
	
	public void raster2vector()
	{

	  procvector();

	  drawvectors();
	  
	  simplifyvector();

	  drawvectors();
	  
	  lengthenvector();
	  
		mSketchy.updateProgress("Vectoriser done: " + numVectors +" vectors");
		
		drawvectors();

	}
	
	private void addsquarevector(int j, int k ) // add four vectors in a 1-pixel square from j,k 
	{
		int m;
		
	  V[Vnum] = new vector();
	  
	  V[Vnum].prev = Vnum + 3;
	  V[Vnum].sx = j;     V[Vnum].sy = k;
	  V[Vnum].ex = j + 1; V[Vnum].ey = k;
	  V[Vnum].next = Vnum + 1;
	  V[Vnum].status = 0;

	  Vnum = Vnum + 1;
	  V[Vnum] = new vector();

	  V[Vnum].prev = Vnum - 1;
	  V[Vnum].sx = j + 1; V[Vnum].sy = k;
	  V[Vnum].ex = j + 1; V[Vnum].ey = k + 1;
	  V[Vnum].next = Vnum + 1;
	  V[Vnum].status = 0;

	  Vnum = Vnum + 1;
	  V[Vnum] = new vector();

	  V[Vnum].prev = Vnum - 1;
	  V[Vnum].sx = j + 1; V[Vnum].sy = k + 1;
	  V[Vnum].ex = j;     V[Vnum].ey = k + 1;
	  V[Vnum].next = Vnum + 1;
	  V[Vnum].status = 0;

	  Vnum = Vnum + 1;
	  V[Vnum] = new vector();

	  V[Vnum].prev = Vnum - 1;
	  V[Vnum].sx = j;     V[Vnum].sy = k + 1;
	  V[Vnum].ex = j;     V[Vnum].ey = k;
	  V[Vnum].next = Vnum - 3;
	  V[Vnum].status = 0;

	  Vnum = Vnum + 1;
	  
	  numVectors += 4;
	  
	}


	private void procvector()
	{

	int j, k;


	  Vnum = 0;
	  
	  for( j = 0; j < MAXSIZEX; j ++ ) 
	  {
		mSketchy.updateProgress("Vectoriser procVector " + j + " / " + MAXSIZEX );

	    for( k = 0; k < MAXSIZEY; k ++ ) 
	    {
	      //if( a[j][k] == 1 )
	    	int color = mEdgeBitmap.getPixel(j, k);
			if( color == -1 ) // as left by Canny
	           addsquarevector(j,k);
	    }
	  }
	}


	private void removevector(int mm, int mm2)
	{
	  int p,n;

	  p = V[mm].prev;
	  V[p].next = V[mm2].next;

	  n = V[mm2].next;
	  V[n].prev = p;
	  
	  numVectors --;
	}


	private void removevectors(int m, int m2)
	{

	  removevector(m,m2);
	  removevector(m2,m);

	  // lastly etch out the unneeded vectors.

	  V[m].status = -1;
	  V[m2].status = -1;

	}


	private boolean equalpoints(int p1x, int p1y, int p2x, int p2y)
	{
	boolean r;

  
	  r = false;
	  if ((p1x == p2x) && (p1y == p2y))
	    r = true;
	 return r;
	}
	


	private boolean equalvectors(int m, int m2)
	{
	  int msx,msy,mex,mey,m2sx,m2sy,m2ex,m2ey;
	  boolean r;

	
	  r = false;
	  if( V[m].status != -1) 
	  {
	      msx = V[m].sx; msy = V[m].sy;
	      mex = V[m].ex; mey = V[m].ey;
	      m2sx = V[m2].sx; m2sy = V[m2].sy;
	      m2ex = V[m2].ex; m2ey = V[m2].ey;

	      if( equalpoints(msx,msy,m2sx,m2sy) &&
	         equalpoints(mex,mey,m2ex,m2ey))
	         r = true;

	      if( equalpoints(msx,msy,m2ex,m2ey) &&
	         equalpoints(mex,mey,m2sx,m2sy))
	         r = true;
	  }


	  return r;
	}


	// grab each vector in list. If it is the same as any other vector,
	// get rid of it.
	private void simplifyvector()
	{
	  int m,m2;

	
	  for( m = 0; m < Vnum; m ++ )
	  {
	     mSketchy.updateProgress("Vectoriser simplifyvector " + m + " / " + Vnum + " (leaving " + numVectors + ")");

			
	    for( m2 = m + 1; m2 < Vnum; m2 ++ )
	    {
	        if( equalvectors(m,m2))
	          removevectors(m,m2);
	    }
	  }

	}


	private void lengthenvector()
	{
		int m;
	  

	  // now we have vectors, but some vectors have multiple points.
	  // so let's turn two vectors into one longer vector. Okay? Okay!

	  for( m = 0; m < Vnum; m ++ )
	  {
		  if( (m % 100) == 0 ) 
			  mSketchy.updateProgress("Vectoriser simplifyvector " + m + " / " + Vnum + " (leaving " + numVectors + ")" );

		     
	    if ((V[m].prev != 0) && (V[m].status > -1))
	    	if( (V[V[m].prev].sx == V[m].ex) &&     // if the prev vector starts where this one ends
	    			(V[V[m].prev].sy == V[m].ey))
	    	{
	         V[V[m].prev].ex = V[m].ex;   // set the prev one to end where this one ends
	         V[V[m].prev].ey = V[m].ey;
	         V[V[m].prev].next = V[m].next;
	         V[V[m].next].prev = V[m].prev;
	         V[m].status = -1;   // and mark this one as unused
	         
	         numVectors --;
	    	}
	  }
	}


	
	private void drawvectors()
	{
		int m;
		int translucent = Color.argb(128, 255,255,255);
		mVectorCanvas.drawColor(translucent);
		
		Paint blackPaint = new Paint(Color.BLACK);
		
		  for( m = 0; m < Vnum; m ++ )
		  {
		    if (V[m].status > -1)
		    {	
		    	mVectorCanvas.drawLine(V[m].sx, V[m].sy, V[m].ex, V[m].ey, blackPaint);
		    }
		  }
	}

	
}
