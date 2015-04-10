/*
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;


public class RandomLineFinder {

	   private void searchForLines( Bitmap input, Canvas canvas )
	    {
	    	
			Paint blackPaint = new Paint();
			blackPaint.setColor(Color.BLACK);

			
			int threshold = 128;
			
			Rect r = randomLine();
			boolean lastWorked = false;
			
			mNLines = 0;
			mNTries = 0;
			
			for( mNTries = 0; mNTries < 100000 && mNLines < 2000; mNTries ++ )
			{
				if( ! mWorking )
					break;
				
				if( ! lastWorked )
					r = randomLine();
				else
					r = randomStep( r );
				
				if( testLine( input, r, threshold  ))
				{
					lastWorked = true;
					mNLines ++;
					canvas.drawLine(r.left, r.top, r.right, r.bottom, blackPaint);
				}
				else
				{
					lastWorked = false;
				}
				
				if( lastWorked )
					updateProgress("Tries: " + mNTries + " Lines: " + mNLines);
			}
	       
	    	
	    }
	    
	    Rect randomStep( Rect r0 )
	    {
	    	Rect r = new Rect();
	    	
	    	r.left = r0.right;
	    	r.top = r0.bottom;
	    	r.right = r.left + mRandom.nextInt( mStep * 2 ) - mStep ;
	    	r.bottom = r.top + mRandom.nextInt( mStep * 2 ) - mStep ;
	    	
	    	if( r.right < 0 )
	    		r.right = 0;
	    	
	    	if( r.right > mWidth )
	    		r.right = mWidth;
	    	
	    	if( r.bottom < 0 )
	    		r.bottom = 0;
	    	
	    	if( r.bottom > mHeight )
	    		r.bottom = mHeight;
	    	return r;
	    }
	    
	    Rect randomLine()
	    {
	    	Rect r = new Rect();
	    	
	    	r.left = mRandom.nextInt( mWidth );
	    	r.right = mRandom.nextInt( mWidth );
	    	r.top = mRandom.nextInt( mHeight );
	    	r.bottom = mRandom.nextInt( mHeight );
	    	    	
	    	return r;
	    }
	    
	    int measureLine( Bitmap input, Rect rect ) // average intensity along this line, range 0->256
	    {
	    	int sum = 0;
	    	int num = 0;
	    	
	    	int x = rect.left;
	    	int y = rect.top;
	    	
	    	int len = (int) Math.sqrt(rect.width() * rect.width() + rect.height() * rect.height());
	    	
	    	for(int i = 0; i < len; i ++ )
	    	{
	       		x = rect.left + (rect.width() * i) / len;
	       		y = rect.top + (rect.height() * i) / len;

	       		if( x < rect.right && y < rect.bottom)
	       		{
	       			int color = input.getPixel(x, y);
	       			sum += (Color.red(color) + Color.green(color) + Color.blue(color)) / 3;
	       			num++;
	       		}
	    	}
	    	
	    	if( num < 1 )
	    		return 0;
	    	
	    	int mean = sum/num;
	    	
	    	return mean;
	    }
	    
	    boolean testLine( Bitmap input, Rect rect, int threshold ) 
	    {
	    	int sum = 0;
	    	int num = 0;
	    	
	    	int x = rect.left;
	    	int y = rect.top;
	    	
	    	int len = (int) Math.sqrt(rect.width() * rect.width() + rect.height() * rect.height());
	    	
	    	for(int i = 0; i < len; i ++ )
	    	{
	       		x = rect.left + (rect.width() * i) / len;
	       		y = rect.top + (rect.height() * i) / len;

	       		if( x < rect.right && y < rect.bottom)
	       		{
	       			int color = input.getPixel(x, y);
	       			
	       			if(  ((Color.red(color) + Color.green(color) + Color.blue(color)) / 3) > threshold)
	       				sum ++;
	       			
	       			num++;
	       		}
	    	}
	    	
	    	if( num < 1 )
	    		return false;
	    	
	    	return sum < num/10; // no more that 10% of line may be over threshold brightness
	    }
	    
}
*/